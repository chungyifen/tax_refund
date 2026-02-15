package com.fox.tax;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fox.tax.modules.rbac.entity.Function;
import com.fox.tax.modules.rbac.entity.Role;
import com.fox.tax.modules.rbac.entity.User;
import com.fox.tax.modules.rbac.repository.FunctionRepository;
import com.fox.tax.modules.rbac.repository.RoleRepository;
import com.fox.tax.modules.rbac.repository.UserRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashSet;
import java.util.Set;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class RbacIntegrationTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private UserRepository userRepository;

        @Autowired
        private RoleRepository roleRepository;

        @Autowired
        private FunctionRepository functionRepository;

        @Autowired
        private ObjectMapper objectMapper;

        @BeforeEach
        void setUp() {
                userRepository.deleteAll();
                roleRepository.deleteAll();
                functionRepository.deleteAll();

                // 1. Create Functions
                Function fnView = functionRepository
                                .save(Function.builder().code("USER_VIEW").name("View Users").build());
                Function fnEdit = functionRepository
                                .save(Function.builder().code("USER_EDIT").name("Edit Users").build());

                // 2. Create Roles
                Role adminRole = Role.builder().name("ROLE_ADMIN").build();
                adminRole.setFunctions(new HashSet<>(Set.of(fnView, fnEdit)));
                roleRepository.save(adminRole);

                Role viewerRole = Role.builder().name("ROLE_VIEWER").build();
                viewerRole.setFunctions(new HashSet<>(Set.of(fnView)));
                roleRepository.save(viewerRole);

                // 3. Create Users
                User admin = User.builder().username("admin").password("password").enabled(true).build();
                admin.setRoles(new HashSet<>(Set.of(adminRole)));
                userRepository.save(admin); // Password encoding happens in Service, but for repository saving we might
                                            // need
                                            // manual encoding if we bypass service.
                // Wait, UserDetailsServiceImpl uses raw password if not encoded? No,
                // SecurityConfig usually expects encoded.
                // My UserService encodes it. But here I am using Repository.
                // To properly test login, I should use UserService to save User, or encode
                // manually.
                // However, I'll bypass login endpoint test and use @WithMockUser for now, OR
                // valid httpBasic.
                // Let's use UserService to creating users to ensure encoding.
        }

        @Autowired
        private com.fox.tax.modules.rbac.service.UserService userService;

        @Test
        void testAdminAccess() throws Exception {
                // Re-create users via Service to ensure password encoding
                userRepository.deleteAll(); // clear again

                // Setup data again but use proper service for user
                Function fnView = functionRepository.findByCode("USER_VIEW")
                                .orElse(functionRepository
                                                .save(Function.builder().code("USER_VIEW").name("View Users").build()));
                Function fnEdit = functionRepository.findByCode("USER_EDIT")
                                .orElse(functionRepository
                                                .save(Function.builder().code("USER_EDIT").name("Edit Users").build()));

                Role adminRole = roleRepository.findByName("ROLE_ADMIN").orElseGet(() -> {
                        Role r = Role.builder().name("ROLE_ADMIN").build();
                        r.setFunctions(new HashSet<>(Set.of(fnView, fnEdit)));
                        return roleRepository.save(r);
                });

                User admin = User.builder().username("admin").password("password").enabled(true).build();
                admin.setRoles(new HashSet<>(Set.of(adminRole)));
                userService.save(admin);

                // Test GET /api/users (requires USER_VIEW)
                mockMvc.perform(get("/api/users")
                                .with(httpBasic("admin", "password")))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$[0].username").value("admin"));

                // Test POST /api/users (requires USER_EDIT)
                User newUser = User.builder().username("testUser").password("pw").build();
                mockMvc.perform(post("/api/users")
                                .with(httpBasic("admin", "password"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(newUser)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.username").value("testUser"));
        }

        @Test
        void testViewerAccess() throws Exception {
                // Setup data
                Function fnView = functionRepository.findByCode("USER_VIEW")
                                .orElse(functionRepository
                                                .save(Function.builder().code("USER_VIEW").name("View Users").build()));

                Role viewerRole = roleRepository.findByName("ROLE_VIEWER").orElseGet(() -> {
                        Role r = Role.builder().name("ROLE_VIEWER").build();
                        r.setFunctions(new HashSet<>(Set.of(fnView)));
                        return roleRepository.save(r);
                });

                User viewer = User.builder().username("viewer").password("password").enabled(true).build();
                viewer.setRoles(new HashSet<>(Set.of(viewerRole)));
                userService.save(viewer);

                // Test GET /api/users (Allowed)
                mockMvc.perform(get("/api/users")
                                .with(httpBasic("viewer", "password")))
                                .andExpect(status().isOk());

                // Test POST /api/users (Forbidden)
                User newUser = User.builder().username("testUser2").password("pw").build();
                mockMvc.perform(post("/api/users")
                                .with(httpBasic("viewer", "password"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(newUser)))
                                .andExpect(status().isForbidden());
        }
}
