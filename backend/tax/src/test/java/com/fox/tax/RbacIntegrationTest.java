package com.fox.tax;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fox.tax.modules.rbac.entity.Function;
import com.fox.tax.modules.rbac.entity.Role;
import com.fox.tax.modules.rbac.entity.User;
import com.fox.tax.modules.rbac.repository.FunctionRepository;
import com.fox.tax.modules.rbac.repository.RoleRepository;
import com.fox.tax.modules.rbac.repository.UserRepository;
import com.fox.tax.modules.rbac.service.UserService;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/** RBAC 權限控管與 JWT 認證整合測試 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RbacIntegrationTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private UserRepository userRepository;

  @Autowired private RoleRepository roleRepository;

  @Autowired private FunctionRepository functionRepository;

  @Autowired private UserService userService;

  @Autowired private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    userRepository.deleteAll();
    roleRepository.deleteAll();
    functionRepository.deleteAll();
  }

  /** 透過 /api/auth/login 取得真實 JWT Token */
  private String obtainJwtToken(String username, String password) throws Exception {
    Map<String, String> loginPayload = Map.of("username", username, "password", password);
    MvcResult result =
        mockMvc
            .perform(
                post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(loginPayload)))
            .andExpect(status().isOk())
            .andReturn();
    JsonNode jsonNode = objectMapper.readTree(result.getResponse().getContentAsString());
    return jsonNode.get("accessToken").asText();
  }

  @Test
  void testAdminAccess() throws Exception {
    // 1. 建立功能權限
    Function fnView =
        functionRepository.save(Function.builder().code("USER_VIEW").name("View Users").build());
    Function fnEdit =
        functionRepository.save(Function.builder().code("USER_EDIT").name("Edit Users").build());

    // 2. 建立管理員角色與使用者
    Role adminRole = Role.builder().name("ROLE_ADMIN").build();
    adminRole.setFunctions(new HashSet<>(Set.of(fnView, fnEdit)));
    roleRepository.save(adminRole);

    User admin = User.builder().username("admin").password("password").enabled(true).build();
    admin.setRoles(new HashSet<>(Set.of(adminRole)));
    userService.save(admin);

    // 3. 登入取得 JWT Token
    String token = obtainJwtToken("admin", "password");

    // 4. 驗證 GET /api/users (需要 USER_VIEW)
    mockMvc
        .perform(get("/api/users").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].username").value("admin"));

    // 5. 驗證 POST /api/users (需要 USER_EDIT)
    Map<String, Object> newUserPayload =
        Map.of("username", "testUser", "password", "password123", "enabled", true);
    mockMvc
        .perform(
            post("/api/users")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newUserPayload)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.username").value("testUser"));
  }

  @Test
  void testViewerAccess() throws Exception {
    // 1. 建立唯讀權限與角色
    Function fnView =
        functionRepository.save(Function.builder().code("USER_VIEW").name("View Users").build());

    Role viewerRole = Role.builder().name("ROLE_VIEWER").build();
    viewerRole.setFunctions(new HashSet<>(Set.of(fnView)));
    roleRepository.save(viewerRole);

    User viewer = User.builder().username("viewer").password("password").enabled(true).build();
    viewer.setRoles(new HashSet<>(Set.of(viewerRole)));
    userService.save(viewer);

    // 2. 登入取得 JWT Token
    String token = obtainJwtToken("viewer", "password");

    // 3. 驗證 GET /api/users (允許存取)
    mockMvc
        .perform(get("/api/users").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk());

    // 4. 驗證 POST /api/users (無 USER_EDIT 權限，應回傳 403 Forbidden)
    Map<String, Object> newUserPayload =
        Map.of("username", "testUser2", "password", "password123", "enabled", true);
    mockMvc
        .perform(
            post("/api/users")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newUserPayload)))
        .andExpect(status().isForbidden());
  }
}
