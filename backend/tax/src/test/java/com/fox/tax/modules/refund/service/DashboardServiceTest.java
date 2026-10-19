package com.fox.tax.modules.refund.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.fox.tax.modules.refund.dto.DashboardStatsDto;
import com.fox.tax.modules.refund.entity.ExportDeclaration;
import com.fox.tax.modules.refund.entity.ImportDeclaration;
import com.fox.tax.modules.refund.entity.TaxRefund;
import com.fox.tax.modules.refund.repository.ExportDeclarationRepository;
import com.fox.tax.modules.refund.repository.ImportDeclarationRepository;
import com.fox.tax.modules.refund.repository.TaxBomRepository;
import com.fox.tax.modules.refund.repository.TaxRefundRepository;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

/** 首頁儀表板統計服務單元測試 */
@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

  @Mock private ImportDeclarationRepository importDeclarationRepository;

  @Mock private ExportDeclarationRepository exportDeclarationRepository;

  @Mock private TaxRefundRepository taxRefundRepository;

  @Mock private TaxBomRepository taxBomRepository;

  @InjectMocks private DashboardService dashboardService;

  @Test
  void getDashboardStats_aggregatesAllMetricsCorrectly() {
    Date now = new Date();

    // 1. 模擬本周新增的進口報單（同一報單號碼 2 筆項次，另一報單 1 筆項次）
    ImportDeclaration imp1 =
        ImportDeclaration.builder()
            .docNo("IMP-001")
            .items("0001")
            .materialName("ALUMINUM")
            .materialSpec("6061")
            .importQty(new BigDecimal("100.000"))
            .totalRefundQty(new BigDecimal("40.000"))
            .build();
    imp1.setCreateTime(now);

    ImportDeclaration imp2 =
        ImportDeclaration.builder()
            .docNo("IMP-001")
            .items("0002")
            .materialName("STEEL")
            .materialSpec("S45C")
            .importQty(new BigDecimal("50.000"))
            .totalRefundQty(BigDecimal.ZERO)
            .build();
    imp2.setCreateTime(now);

    ImportDeclaration imp3 =
        ImportDeclaration.builder()
            .docNo("IMP-002")
            .items("0001")
            .materialName("CARBON")
            .materialSpec("T700")
            .importQty(new BigDecimal("200.000"))
            .totalRefundQty(new BigDecimal("200.000"))
            .build();
    imp3.setCreateTime(now);

    when(importDeclarationRepository.findByCreateTimeGreaterThanEqualOrderByCreateTimeDescIdDesc(
            any(Date.class)))
        .thenReturn(List.of(imp1, imp2, imp3));

    // 2. 模擬本周新增核銷的出口報單與退稅紀錄
    ExportDeclaration exp1 =
        ExportDeclaration.builder()
            .docNo("EXP-001")
            .items("0001")
            .prodType("FORK-A")
            .prodName("SUSPENSION FORK")
            .exportQty(new BigDecimal("80.000"))
            .status(ExportDeclaration._Status_Create_Refund)
            .build();
    exp1.setUpdateTime(now);

    TaxRefund tr1 =
        TaxRefund.builder()
            .reportNo("20261019-100000")
            .docNo("BOM-001")
            .exportDeclaration(exp1)
            .importDeclaration(imp1)
            .usageQty(new BigDecimal("40.000"))
            .branchNum(1)
            .build();
    tr1.setCreateTime(now);

    when(exportDeclarationRepository.findWeeklyVerifiedExports(any(Date.class)))
        .thenReturn(List.of(exp1));
    when(taxRefundRepository.findByCreateTimeGreaterThanEqual(any(Date.class)))
        .thenReturn(List.of(tr1));

    // 3. 模擬未核銷進口報單數目與整體核銷總量
    when(importDeclarationRepository.countDistinctDocNoWithUnverifiedItems()).thenReturn(5L);
    when(importDeclarationRepository.countUnverifiedItems()).thenReturn(12L);
    when(importDeclarationRepository.sumTotalImportAndRefundQty())
        .thenReturn(
            Collections.singletonList(
                new Object[] {new BigDecimal("1000.000"), new BigDecimal("650.000")}));

    // 4. 模擬前十名核銷原料
    Object[] topMatRow =
        new Object[] {"ALUMINUM", "KG", new BigDecimal("500.000"), 15L, 4L};
    when(taxRefundRepository.findTopVerifiedMaterialsByQty(any(Pageable.class)))
        .thenReturn(Collections.singletonList(topMatRow));
    when(taxRefundRepository.findTopVerifiedMaterialsByCount(any(Pageable.class)))
        .thenReturn(Collections.singletonList(topMatRow));

    // 5. 模擬前十名核銷成品
    Object[] topProdRow =
        new Object[] {"SUSPENSION FORK", "FORK-A", new BigDecimal("320.000"), 8L, 3L};
    when(exportDeclarationRepository.findTopVerifiedProductsByQty(any(Pageable.class)))
        .thenReturn(Collections.singletonList(topProdRow));
    when(exportDeclarationRepository.findTopVerifiedProductsByCount(any(Pageable.class)))
        .thenReturn(Collections.singletonList(topProdRow));

    // 6. 模擬出口狀態與 BOM 統計
    when(exportDeclarationRepository.countDistinctDocNoByStatus(ExportDeclaration._Status_Create))
        .thenReturn(2L);
    when(exportDeclarationRepository.countByStatus(ExportDeclaration._Status_Create))
        .thenReturn(6L);
    when(exportDeclarationRepository.countDistinctDocNoByStatus(
            ExportDeclaration._Status_Create_Refund))
        .thenReturn(3L);
    when(exportDeclarationRepository.countByStatus(ExportDeclaration._Status_Create_Refund))
        .thenReturn(9L);
    when(exportDeclarationRepository.countDistinctDocNoByStatus(
            ExportDeclaration._Status_Create_Refund_Report))
        .thenReturn(10L);
    when(exportDeclarationRepository.countByStatus(
            ExportDeclaration._Status_Create_Refund_Report))
        .thenReturn(25L);
    when(taxBomRepository.countDistinctDocNo()).thenReturn(4L);
    when(taxBomRepository.count()).thenReturn(30L);

    // 執行測試
    DashboardStatsDto result = dashboardService.getDashboardStats();

    // 驗證結果
    assertNotNull(result);
    assertEquals(2L, result.getWeeklyImportDocCount());
    assertEquals(3L, result.getWeeklyImportItemCount());
    assertEquals(2, result.getWeeklyImportDeclarations().size());
    assertEquals(
        new BigDecimal("150.000"),
        result.getWeeklyImportDeclarations().get(0).getTotalImportQty());

    assertEquals(1L, result.getWeeklyVerifiedExportDocCount());
    assertEquals(1L, result.getWeeklyVerifiedExportItemCount());
    assertEquals(1L, result.getWeeklyVerifiedExportDeclarations().get(0).getRefundRecordCount());

    assertEquals(5L, result.getUnverifiedImportDocCount());
    assertEquals(12L, result.getUnverifiedImportItemCount());
    assertEquals(new BigDecimal("350.000"), result.getTotalUnverifiedImportQty());
    assertEquals(new BigDecimal("65.0"), result.getOverallVerificationRate());

    assertEquals(1, result.getTopMaterialsByQty().size());
    assertEquals("ALUMINUM", result.getTopMaterialsByQty().get(0).getMaterialName());

    assertEquals(1, result.getTopProductsByQty().size());
    assertEquals("SUSPENSION FORK", result.getTopProductsByQty().get(0).getProdName());

    assertEquals(2L, result.getPendingExportDocCount());
    assertEquals(4L, result.getTotalBomDocCount());
  }
}
