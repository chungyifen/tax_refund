package com.fox.tax.modules.refund.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fox.tax.common.config.SecurityConfig;
import com.fox.tax.common.security.JwtTokenProvider;
import com.fox.tax.modules.rbac.service.UserDetailsServiceImpl;
import com.fox.tax.modules.refund.dto.ImportDeclarationDto;
import com.fox.tax.modules.refund.service.ImportDeclarationService;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ImportDeclarationController.class)
@Import(SecurityConfig.class)
class ImportDeclarationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ImportDeclarationService importDeclarationService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private UserDetailsServiceImpl userDetailsService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @WithMockUser(authorities = "IMPORT_DECLARATION_VIEW")
    void search_withAuthority() throws Exception {
        when(importDeclarationService.search(any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(new ArrayList<>(), PageRequest.of(0, 20), 0));

        mockMvc.perform(get("/api/refund/import-declaration"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = "OTHER")
    void search_noAuthority() throws Exception {
        mockMvc.perform(get("/api/refund/import-declaration"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "IMPORT_DECLARATION_VIEW")
    void getDocNos() throws Exception {
        when(importDeclarationService.getDistinctDocNos()).thenReturn(List.of("DOC001", "DOC002"));

        mockMvc.perform(get("/api/refund/import-declaration/doc-nos"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = "IMPORT_DECLARATION_EDIT")
    void update() throws Exception {
        ImportDeclarationDto dto = new ImportDeclarationDto();
        when(importDeclarationService.update(eq(1L), any(ImportDeclarationDto.class))).thenReturn(dto);

        mockMvc.perform(put("/api/refund/import-declaration/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = "IMPORT_DECLARATION_EDIT")
    void deleteOne() throws Exception {
        doNothing().when(importDeclarationService).delete(1L);

        mockMvc.perform(delete("/api/refund/import-declaration/1"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = "IMPORT_DECLARATION_EDIT")
    void batchDelete() throws Exception {
        when(importDeclarationService.batchDelete(anyList())).thenReturn(null);

        mockMvc.perform(post("/api/refund/import-declaration/batch-delete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(List.of(1L, 2L))))
                .andExpect(status().isOk());
    }
}
