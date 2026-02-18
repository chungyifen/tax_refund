package com.fox.tax.modules.refund.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fox.tax.common.config.SecurityConfig;
import com.fox.tax.common.security.JwtTokenProvider;
import com.fox.tax.modules.rbac.service.UserDetailsServiceImpl;
import com.fox.tax.modules.refund.dto.GenerateRefundResultDto;
import com.fox.tax.modules.refund.entity.TaxRefund;
import com.fox.tax.modules.refund.service.TaxRefundService;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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

@WebMvcTest(TaxRefundController.class)
@Import(SecurityConfig.class)
class TaxRefundControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TaxRefundService taxRefundService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private UserDetailsServiceImpl userDetailsService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @WithMockUser(authorities = "EXPORT_DECLARATION_EDIT")
    void generateRefundList() throws Exception {
        GenerateRefundResultDto result = new GenerateRefundResultDto();
        result.setSuccessCount(5);
        result.setReportNo("RPT001");
        result.setWarnings(List.of());
        when(taxRefundService.generateRefundList(eq("DOC1"))).thenReturn(result);

        mockMvc.perform(post("/api/refund/tax-refund/generate")
                        .param("docNo", "DOC1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.successCount").value(5))
                .andExpect(jsonPath("$.reportNo").value("RPT001"));
    }

    @Test
    @WithMockUser(authorities = "TAX_REFUND_VIEW")
    void findByReportNo() throws Exception {
        when(taxRefundService.findByReportNo("RPT001")).thenReturn(List.of());

        mockMvc.perform(get("/api/refund/tax-refund/RPT001"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = "TAX_REFUND_VIEW")
    void searchExportList() throws Exception {
        when(taxRefundService.searchExports(any(), any(), any()))
                .thenReturn(new PageImpl<>(new ArrayList<>(), PageRequest.of(0, 10), 0));

        mockMvc.perform(get("/api/refund/tax-refund/list"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = "EXPORT_DECLARATION_EDIT")
    void updateRefundQty() throws Exception {
        doNothing().when(taxRefundService).updateRefundQty(eq(1L), any(BigDecimal.class));

        Map<String, BigDecimal> body = Map.of("usageQty", BigDecimal.TEN);

        mockMvc.perform(put("/api/refund/tax-refund/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = "OTHER")
    void generateRefundList_noAuthority() throws Exception {
        mockMvc.perform(post("/api/refund/tax-refund/generate")
                        .param("docNo", "DOC1"))
                .andExpect(status().isForbidden());
    }
}
