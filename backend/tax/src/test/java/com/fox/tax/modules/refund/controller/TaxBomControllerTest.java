package com.fox.tax.modules.refund.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fox.tax.common.config.SecurityConfig;
import com.fox.tax.common.security.JwtTokenProvider;
import com.fox.tax.modules.rbac.service.UserDetailsServiceImpl;
import com.fox.tax.modules.refund.dto.TaxBomDto;
import com.fox.tax.modules.refund.service.TaxBomService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(TaxBomController.class)
@Import(SecurityConfig.class)
class TaxBomControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TaxBomService taxBomService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private UserDetailsServiceImpl userDetailsService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @WithMockUser
    void findAll() throws Exception {
        Page<TaxBomDto> emptyPage = new PageImpl<>(List.of(), PageRequest.of(0, 20), 0);
        when(taxBomService.findAll(any(), any(), any(), any(Pageable.class))).thenReturn(emptyPage);

        mockMvc.perform(get("/api/refund/bom"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    void create() throws Exception {
        TaxBomDto dto = new TaxBomDto();
        when(taxBomService.save(any(TaxBomDto.class))).thenReturn(dto);

        mockMvc.perform(post("/api/refund/bom")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    void update() throws Exception {
        TaxBomDto dto = new TaxBomDto();
        when(taxBomService.update(eq(1L), any(TaxBomDto.class))).thenReturn(dto);

        mockMvc.perform(put("/api/refund/bom/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    void deleteOne() throws Exception {
        doNothing().when(taxBomService).delete(1L);

        mockMvc.perform(delete("/api/refund/bom/1"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    void deleteBatch() throws Exception {
        doNothing().when(taxBomService).deleteBatch(anyList());

        mockMvc.perform(delete("/api/refund/bom/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(List.of(1L, 2L, 3L))))
                .andExpect(status().isOk());
    }
}
