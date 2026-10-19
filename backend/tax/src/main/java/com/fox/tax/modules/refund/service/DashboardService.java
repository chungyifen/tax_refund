package com.fox.tax.modules.refund.service;

import com.fox.tax.modules.refund.dto.DashboardStatsDto;
import com.fox.tax.modules.refund.dto.DashboardStatsDto.TopMaterialDto;
import com.fox.tax.modules.refund.dto.DashboardStatsDto.TopProductDto;
import com.fox.tax.modules.refund.dto.DashboardStatsDto.WeeklyImportSummaryDto;
import com.fox.tax.modules.refund.dto.DashboardStatsDto.WeeklyVerifiedExportSummaryDto;
import com.fox.tax.modules.refund.entity.ExportDeclaration;
import com.fox.tax.modules.refund.entity.ImportDeclaration;
import com.fox.tax.modules.refund.entity.TaxRefund;
import com.fox.tax.modules.refund.repository.ExportDeclarationRepository;
import com.fox.tax.modules.refund.repository.ImportDeclarationRepository;
import com.fox.tax.modules.refund.repository.TaxBomRepository;
import com.fox.tax.modules.refund.repository.TaxRefundRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 首頁儀表板統計服務
 *
 * <p>負責彙整登入首頁所需的各項進出口報單統計、核銷進度以及前十名原料與成品排行。
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true, rollbackFor = RuntimeException.class)
public class DashboardService {

  private static final int TOP_LIMIT = 10;
  private static final DateTimeFormatter DATE_FORMATTER =
      DateTimeFormatter.ofPattern("yyyy-MM-dd");

  private final ImportDeclarationRepository importDeclarationRepository;
  private final ExportDeclarationRepository exportDeclarationRepository;
  private final TaxRefundRepository taxRefundRepository;
  private final TaxBomRepository taxBomRepository;

  /**
   * 取得登入首頁完整統計資料
   *
   * @return DashboardStatsDto 首頁儀表板統計 DTO
   */
  public DashboardStatsDto getDashboardStats() {
    // 計算本周一 00:00:00 與本周日日期
    LocalDate today = LocalDate.now();
    LocalDate monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    LocalDate sunday = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
    Date startOfWeek = Date.from(monday.atStartOfDay(ZoneId.systemDefault()).toInstant());

    DashboardStatsDto stats = new DashboardStatsDto();
    stats.setWeekStartDate(monday.format(DATE_FORMATTER));
    stats.setWeekEndDate(sunday.format(DATE_FORMATTER));

    // 1. 本周新增的進口報單統計與清單
    populateWeeklyImports(stats, startOfWeek);

    // 2. 本周新增核銷的出口報單統計與清單
    populateWeeklyVerifiedExports(stats, startOfWeek);

    // 3. 目前為止還有未核銷項目的進口報單數目與整體核銷率
    populateUnverifiedImportsAndOverallRate(stats);

    // 4. 核銷最多前十名的原料
    Pageable topTenPageable = PageRequest.of(0, TOP_LIMIT);
    populateTopMaterials(stats, topTenPageable);

    // 5. 核銷最多前十名的成品
    populateTopProducts(stats, topTenPageable);

    // 6. 建議擴充指標：出口報單各階段作業進度與 BOM 核退標準統計
    populateWorkflowAndBomStats(stats);

    return stats;
  }

  /**
   * 彙整本周新增的進口報單指標與清單（依報單號碼 docNo 分組彙總）
   *
   * @param stats 儀表板 DTO
   * @param startOfWeek 本周起始時間
   */
  private void populateWeeklyImports(DashboardStatsDto stats, Date startOfWeek) {
    List<ImportDeclaration> weeklyItems =
        importDeclarationRepository.findByCreateTimeGreaterThanEqualOrderByCreateTimeDescIdDesc(
            startOfWeek);

    stats.setWeeklyImportItemCount(weeklyItems.size());

    // 依 docNo 彙整每張進口報單的項次數、總進口數量與已核銷數量
    Map<String, WeeklyImportSummaryDto> summaryMap = new LinkedHashMap<>();
    for (ImportDeclaration item : weeklyItems) {
      String docNo = item.getDocNo();
      BigDecimal importQty = item.getImportQty() != null ? item.getImportQty() : BigDecimal.ZERO;
      BigDecimal refundQty =
          item.getTotalRefundQty() != null ? item.getTotalRefundQty() : BigDecimal.ZERO;

      WeeklyImportSummaryDto summary = summaryMap.get(docNo);
      if (summary == null) {
        summary =
            WeeklyImportSummaryDto.builder()
                .docNo(docNo)
                .itemCount(1L)
                .totalImportQty(importQty)
                .totalRefundQty(refundQty)
                .createTime(item.getCreateTime())
                .build();
        summaryMap.put(docNo, summary);
      } else {
        summary.setItemCount(summary.getItemCount() + 1);
        summary.setTotalImportQty(summary.getTotalImportQty().add(importQty));
        summary.setTotalRefundQty(summary.getTotalRefundQty().add(refundQty));
        if (item.getCreateTime() != null
            && (summary.getCreateTime() == null
                || item.getCreateTime().after(summary.getCreateTime()))) {
          summary.setCreateTime(item.getCreateTime());
        }
      }
    }

    stats.setWeeklyImportDocCount(summaryMap.size());
    stats.setWeeklyImportDeclarations(new ArrayList<>(summaryMap.values()));
  }

  /**
   * 彙整本周新增核銷的出口報單指標與清單（依出口報單號碼 docNo 分組彙總）
   *
   * @param stats 儀表板 DTO
   * @param startOfWeek 本周起始時間
   */
  private void populateWeeklyVerifiedExports(DashboardStatsDto stats, Date startOfWeek) {
    List<ExportDeclaration> verifiedExports =
        exportDeclarationRepository.findWeeklyVerifiedExports(startOfWeek);
    List<TaxRefund> weeklyRefunds =
        taxRefundRepository.findByCreateTimeGreaterThanEqual(startOfWeek);

    // 合併由 updateTime/createTime 查出的出口報單與本周退稅紀錄關聯的出口報單（依 ID 去重）
    Map<Long, ExportDeclaration> mergedExportsMap = new LinkedHashMap<>();
    if (verifiedExports != null) {
      for (ExportDeclaration exp : verifiedExports) {
        if (exp != null) {
          mergedExportsMap.put(exp.getId() != null ? exp.getId() : (long) System.identityHashCode(exp), exp);
        }
      }
    }

    // 統計每張出口報單在本周產生的退稅核銷紀錄筆數與最新核銷時間
    Map<String, Long> refundCountByDocNo = new HashMap<>();
    Map<String, Date> latestRefundTimeByDocNo = new HashMap<>();
    if (weeklyRefunds != null) {
      for (TaxRefund refund : weeklyRefunds) {
        ExportDeclaration exp = refund.getExportDeclaration();
        if (exp != null && exp.getDocNo() != null) {
          mergedExportsMap.putIfAbsent(
              exp.getId() != null ? exp.getId() : (long) System.identityHashCode(exp), exp);
          String docNo = exp.getDocNo();
          refundCountByDocNo.merge(docNo, 1L, Long::sum);
          Date refundTime = refund.getCreateTime();
          if (refundTime != null) {
            latestRefundTimeByDocNo.merge(
                docNo,
                refundTime,
                (oldTime, newTime) -> newTime.after(oldTime) ? newTime : oldTime);
          }
        }
      }
    }

    List<ExportDeclaration> allWeeklyExports = new ArrayList<>(mergedExportsMap.values());
    stats.setWeeklyVerifiedExportItemCount(allWeeklyExports.size());

    Map<String, WeeklyVerifiedExportSummaryDto> summaryMap = new LinkedHashMap<>();
    for (ExportDeclaration exp : allWeeklyExports) {
      String docNo = exp.getDocNo();
      BigDecimal exportQty = exp.getExportQty() != null ? exp.getExportQty() : BigDecimal.ZERO;
      Date expVerifiedTime =
          latestRefundTimeByDocNo.getOrDefault(
              docNo, exp.getUpdateTime() != null ? exp.getUpdateTime() : exp.getCreateTime());

      WeeklyVerifiedExportSummaryDto summary = summaryMap.get(docNo);
      if (summary == null) {
        summary =
            WeeklyVerifiedExportSummaryDto.builder()
                .docNo(docNo)
                .itemCount(1L)
                .totalExportQty(exportQty)
                .refundRecordCount(refundCountByDocNo.getOrDefault(docNo, 0L))
                .status(exp.getStatus())
                .verifiedTime(expVerifiedTime)
                .build();
        summaryMap.put(docNo, summary);
      } else {
        summary.setItemCount(summary.getItemCount() + 1);
        summary.setTotalExportQty(summary.getTotalExportQty().add(exportQty));
        if (exp.getStatus() != null
            && (summary.getStatus() == null || exp.getStatus() > summary.getStatus())) {
          summary.setStatus(exp.getStatus());
        }
        if (expVerifiedTime != null
            && (summary.getVerifiedTime() == null
                || expVerifiedTime.after(summary.getVerifiedTime()))) {
          summary.setVerifiedTime(expVerifiedTime);
        }
      }
    }

    stats.setWeeklyVerifiedExportDocCount(summaryMap.size());
    stats.setWeeklyVerifiedExportDeclarations(new ArrayList<>(summaryMap.values()));
  }

  /**
   * 統計目前尚有未核銷項目的進口報單數目，以及全系統累計進口與核銷比率
   *
   * @param stats 儀表板 DTO
   */
  private void populateUnverifiedImportsAndOverallRate(DashboardStatsDto stats) {
    long unverifiedDocCount = importDeclarationRepository.countDistinctDocNoWithUnverifiedItems();
    long unverifiedItemCount = importDeclarationRepository.countUnverifiedItems();
    stats.setUnverifiedImportDocCount(unverifiedDocCount);
    stats.setUnverifiedImportItemCount(unverifiedItemCount);

    List<Object[]> sums = importDeclarationRepository.sumTotalImportAndRefundQty();
    BigDecimal totalImportQty = BigDecimal.ZERO;
    BigDecimal totalRefundQty = BigDecimal.ZERO;

    if (sums != null && !sums.isEmpty() && sums.get(0) != null) {
      Object[] row = sums.get(0);
      totalImportQty = toBigDecimal(row[0]);
      totalRefundQty = toBigDecimal(row[1]);
    }

    BigDecimal unverifiedQty = totalImportQty.subtract(totalRefundQty).max(BigDecimal.ZERO);
    stats.setTotalImportQty(totalImportQty);
    stats.setTotalRefundQty(totalRefundQty);
    stats.setTotalUnverifiedImportQty(unverifiedQty);

    if (totalImportQty.compareTo(BigDecimal.ZERO) > 0) {
      BigDecimal rate =
          totalRefundQty
              .multiply(BigDecimal.valueOf(100))
              .divide(totalImportQty, 1, RoundingMode.HALF_UP)
              .min(BigDecimal.valueOf(100));
      stats.setOverallVerificationRate(rate);
    } else {
      stats.setOverallVerificationRate(BigDecimal.ZERO);
    }
  }

  /**
   * 查詢核銷最多前十名的原料（同時提供依總核銷數量排序與依核銷次數排序）
   *
   * @param stats 儀表板 DTO
   * @param pageable 前十名分頁參數
   */
  private void populateTopMaterials(DashboardStatsDto stats, Pageable pageable) {
    List<Object[]> byQtyRows = taxRefundRepository.findTopVerifiedMaterialsByQty(pageable);
    List<Object[]> byCountRows = taxRefundRepository.findTopVerifiedMaterialsByCount(pageable);

    // 若退稅明細表尚無紀錄但進口報單本身已有核銷數量，則使用進口報單統計作為備援
    if (byQtyRows == null || byQtyRows.isEmpty()) {
      List<Object[]> fallbackRows =
          importDeclarationRepository.findTopVerifiedMaterialsFromImport(pageable);
      List<TopMaterialDto> fallbackList = mapToTopMaterialList(fallbackRows);
      stats.setTopMaterialsByQty(fallbackList);
      stats.setTopMaterialsByCount(fallbackList);
      return;
    }

    stats.setTopMaterialsByQty(mapToTopMaterialList(byQtyRows));
    stats.setTopMaterialsByCount(mapToTopMaterialList(byCountRows));
  }

  /**
   * 查詢核銷最多前十名的成品（同時提供依總核銷出口數量排序與依核銷項次筆數排序）
   *
   * @param stats 儀表板 DTO
   * @param pageable 前十名分頁參數
   */
  private void populateTopProducts(DashboardStatsDto stats, Pageable pageable) {
    List<Object[]> byQtyRows = exportDeclarationRepository.findTopVerifiedProductsByQty(pageable);
    List<Object[]> byCountRows =
        exportDeclarationRepository.findTopVerifiedProductsByCount(pageable);

    stats.setTopProductsByQty(mapToTopProductList(byQtyRows));
    stats.setTopProductsByCount(mapToTopProductList(byCountRows));
  }

  /**
   * 統計建議擴充項目：出口報單各階段作業狀態數目與核退標準 BOM 統計
   *
   * @param stats 儀表板 DTO
   */
  private void populateWorkflowAndBomStats(DashboardStatsDto stats) {
    stats.setPendingExportDocCount(
        exportDeclarationRepository.countDistinctDocNoByStatus(ExportDeclaration._Status_Create));
    stats.setPendingExportItemCount(
        exportDeclarationRepository.countByStatus(ExportDeclaration._Status_Create));

    stats.setVerifiedUnreportedExportDocCount(
        exportDeclarationRepository.countDistinctDocNoByStatus(
            ExportDeclaration._Status_Create_Refund));
    stats.setVerifiedUnreportedExportItemCount(
        exportDeclarationRepository.countByStatus(ExportDeclaration._Status_Create_Refund));

    stats.setReportedExportDocCount(
        exportDeclarationRepository.countDistinctDocNoByStatus(
            ExportDeclaration._Status_Create_Refund_Report));
    stats.setReportedExportItemCount(
        exportDeclarationRepository.countByStatus(ExportDeclaration._Status_Create_Refund_Report));

    stats.setTotalBomDocCount(taxBomRepository.countDistinctDocNo());
    stats.setTotalBomItemCount(taxBomRepository.count());
  }

  /** 將 JPQL 聚合結果轉換為 TopMaterialDto 列表 */
  private List<TopMaterialDto> mapToTopMaterialList(List<Object[]> rows) {
    List<TopMaterialDto> list = new ArrayList<>();
    if (rows == null) {
      return list;
    }
    for (Object[] row : rows) {
      if (row == null || row.length < 5) {
        continue;
      }
      list.add(
          TopMaterialDto.builder()
              .materialName(row[0] != null ? row[0].toString() : "未命名原料")
              .materialUnit(row[1] != null ? row[1].toString() : "")
              .totalVerifiedQty(toBigDecimal(row[2]))
              .verifyCount(toLong(row[3]))
              .docCount(toLong(row[4]))
              .build());
    }
    return list;
  }

  /** 將 JPQL 聚合結果轉換為 TopProductDto 列表 */
  private List<TopProductDto> mapToTopProductList(List<Object[]> rows) {
    List<TopProductDto> list = new ArrayList<>();
    if (rows == null) {
      return list;
    }
    for (Object[] row : rows) {
      if (row == null || row.length < 5) {
        continue;
      }
      list.add(
          TopProductDto.builder()
              .prodName(row[0] != null ? row[0].toString() : "未命名成品")
              .prodType(row[1] != null ? row[1].toString() : "")
              .totalVerifiedQty(toBigDecimal(row[2]))
              .verifyCount(toLong(row[3]))
              .docCount(toLong(row[4]))
              .build());
    }
    return list;
  }

  /** 安全轉換 Object 為 BigDecimal */
  private BigDecimal toBigDecimal(Object val) {
    if (val == null) {
      return BigDecimal.ZERO;
    }
    if (val instanceof BigDecimal bd) {
      return bd;
    }
    if (val instanceof Number num) {
      return BigDecimal.valueOf(num.doubleValue());
    }
    return new BigDecimal(val.toString());
  }

  /** 安全轉換 Object 為 long */
  private long toLong(Object val) {
    if (val == null) {
      return 0L;
    }
    if (val instanceof Number num) {
      return num.longValue();
    }
    return Long.parseLong(val.toString());
  }
}
