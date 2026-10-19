package com.fox.tax.modules.refund.controller;

import com.fox.tax.modules.refund.dto.DashboardStatsDto;
import com.fox.tax.modules.refund.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 登入首頁儀表板 Controller
 *
 * <p>提供登入首頁所需的各項進出口報單統計、核銷進度、前十名原料與前十名成品排行 API。
 */
@Tag(name = "Dashboard", description = "登入首頁儀表板統計 API")
@RestController
@RequestMapping("/api/refund/dashboard")
@RequiredArgsConstructor
public class DashboardController {

  private final DashboardService dashboardService;

  /**
   * 取得登入首頁儀表板統計資訊
   *
   * @return DashboardStatsDto 儀表板統計結果
   */
  @Operation(summary = "取得登入首頁儀表板統計資訊")
  @GetMapping("/stats")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<DashboardStatsDto> getDashboardStats() {
    return ResponseEntity.ok(dashboardService.getDashboardStats());
  }
}
