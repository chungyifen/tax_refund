package com.fox.tax.modules.refund.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fox.tax.common.config.SecurityConfig;
import com.fox.tax.common.security.JwtTokenProvider;
import com.fox.tax.modules.rbac.service.UserDetailsServiceImpl;
import com.fox.tax.modules.refund.dto.DashboardStatsDto;
import com.fox.tax.modules.refund.service.DashboardService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

/** 登入首頁儀表板 Controller 單元測試 */
@WebMvcTest(DashboardController.class)
@Import(SecurityConfig.class)
class DashboardControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockBean private DashboardService dashboardService;

  @MockBean private JwtTokenProvider jwtTokenProvider;

  @MockBean private UserDetailsServiceImpl userDetailsService;

  @Test
  @WithMockUser(username = "op_user", authorities = "BOM_VIEW")
  void getDashboardStats_authenticatedUser_returnsOk() throws Exception {
    DashboardStatsDto dto =
        DashboardStatsDto.builder()
            .weeklyImportDocCount(3L)
            .weeklyVerifiedExportDocCount(2L)
            .unverifiedImportDocCount(7L)
            .build();

    when(dashboardService.getDashboardStats()).thenReturn(dto);

    mockMvc
        .perform(get("/api/refund/dashboard/stats"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.weeklyImportDocCount").value(3))
        .andExpect(jsonPath("$.weeklyVerifiedExportDocCount").value(2))
        .andExpect(jsonPath("$.unverifiedImportDocCount").value(7));
  }

  @Test
  void getDashboardStats_unauthenticated_returnsForbiddenOrUnauthorized() throws Exception {
    mockMvc.perform(get("/api/refund/dashboard/stats")).andExpect(status().isForbidden());
  }
}
