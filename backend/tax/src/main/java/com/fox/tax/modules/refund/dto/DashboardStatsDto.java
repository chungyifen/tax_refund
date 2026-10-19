package com.fox.tax.modules.refund.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 登入首頁儀表板統計資訊 DTO
 *
 * <p>包含使用者要求的五大核心指標（本周新增進口報單、本周新增核銷出口報單、未核銷進口報單數、
 * 核銷前十名原料與前十名成品），以及建議補充的進出口作業進度與整體核銷率指標。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStatsDto {

  /** 本周統計起始日期 (yyyy-MM-dd) */
  private String weekStartDate;

  /** 本周統計結束日期 (yyyy-MM-dd) */
  private String weekEndDate;

  // ==========================================
  // 1. 本周新增的進口報單
  // ==========================================

  /** 本周新增的不重複進口報單號碼數量（張數） */
  private long weeklyImportDocCount;

  /** 本周新增的進口報單明細項次數量（筆數） */
  private long weeklyImportItemCount;

  /** 本周新增的進口報單彙整清單 */
  @Builder.Default
  private List<WeeklyImportSummaryDto> weeklyImportDeclarations = new ArrayList<>();

  // ==========================================
  // 2. 本周新增核銷的出口報單
  // ==========================================

  /** 本周新增核銷的不重複出口報單號碼數量（張數） */
  private long weeklyVerifiedExportDocCount;

  /** 本周新增核銷的出口報單明細項次數量（筆數） */
  private long weeklyVerifiedExportItemCount;

  /** 本周新增核銷的出口報單彙整清單 */
  @Builder.Default
  private List<WeeklyVerifiedExportSummaryDto> weeklyVerifiedExportDeclarations =
      new ArrayList<>();

  // ==========================================
  // 3. 目前為止還有未核銷項目的進口報單數目
  // ==========================================

  /** 目前尚有未核銷餘額的不重複進口報單號碼數目（張數） */
  private long unverifiedImportDocCount;

  /** 目前尚有未核銷餘額的進口報單明細項次數目（筆數） */
  private long unverifiedImportItemCount;

  /** 目前尚可核銷的進口原料總餘額數量 */
  @Builder.Default private BigDecimal totalUnverifiedImportQty = BigDecimal.ZERO;

  // ==========================================
  // 4. 核銷最多前十名的原料
  // ==========================================

  /** 核銷總數量前十名的原料清單 */
  @Builder.Default private List<TopMaterialDto> topMaterialsByQty = new ArrayList<>();

  /** 核銷筆數前十名的原料清單 */
  @Builder.Default private List<TopMaterialDto> topMaterialsByCount = new ArrayList<>();

  // ==========================================
  // 5. 核銷最多前十名的成品
  // ==========================================

  /** 核銷總出口數量前十名的成品清單 */
  @Builder.Default private List<TopProductDto> topProductsByQty = new ArrayList<>();

  /** 核銷項次筆數前十名的成品清單 */
  @Builder.Default private List<TopProductDto> topProductsByCount = new ArrayList<>();

  // ==========================================
  // 6. 建議擴充項目：作業進度與全系統概況
  // ==========================================

  /** 待核銷的出口報單張數 (status = 1 已匯入待核銷) */
  private long pendingExportDocCount;

  /** 待核銷的出口報單項次數 (status = 1) */
  private long pendingExportItemCount;

  /** 已核銷但尚未產出報表的出口報單張數 (status = 2) */
  private long verifiedUnreportedExportDocCount;

  /** 已核銷但尚未產出報表的出口報單項次數 (status = 2) */
  private long verifiedUnreportedExportItemCount;

  /** 已完成報表匯出的出口報單張數 (status = 3) */
  private long reportedExportDocCount;

  /** 已完成報表匯出的出口報單項次數 (status = 3) */
  private long reportedExportItemCount;

  /** 全系統累計進口總數量 */
  @Builder.Default private BigDecimal totalImportQty = BigDecimal.ZERO;

  /** 全系統累計已核銷總數量 */
  @Builder.Default private BigDecimal totalRefundQty = BigDecimal.ZERO;

  /** 整體進口原料核銷率百分比 (0 ~ 100) */
  @Builder.Default private BigDecimal overallVerificationRate = BigDecimal.ZERO;

  /** 已建檔的核退標準 (BOM) 工業局文號總數 */
  private long totalBomDocCount;

  /** 已建檔的核退標準 (BOM) 明細總筆數 */
  private long totalBomItemCount;

  /** 本周新增進口報單彙整 DTO */
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class WeeklyImportSummaryDto {
    /** 進口報單號碼 */
    private String docNo;

    /** 報單包含的明細項次數 */
    private long itemCount;

    /** 該張報單總進口數量 */
    private BigDecimal totalImportQty;

    /** 該張報單目前已核銷數量 */
    private BigDecimal totalRefundQty;

    /** 匯入/建立時間 */
    private Date createTime;
  }

  /** 本周新增核銷出口報單彙整 DTO */
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class WeeklyVerifiedExportSummaryDto {
    /** 出口報單號碼 */
    private String docNo;

    /** 本周核銷的出口項次數 */
    private long itemCount;

    /** 該張報單已核銷項次的總出口數量 */
    private BigDecimal totalExportQty;

    /** 產生的退稅核銷紀錄筆數 */
    private long refundRecordCount;

    /** 最新狀態 (2: 已產生核銷清單, 3: 已產生核銷清單報表) */
    private Integer status;

    /** 核銷時間 */
    private Date verifiedTime;
  }

  /** 核銷前十名原料統計 DTO */
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class TopMaterialDto {
    /** 原料名稱 */
    private String materialName;

    /** 原料單位 */
    private String materialUnit;

    /** 總核銷數量 */
    private BigDecimal totalVerifiedQty;

    /** 核銷次數（退稅核銷紀錄筆數） */
    private long verifyCount;

    /** 涉及的不重複進口報單張數 */
    private long docCount;
  }

  /** 核銷前十名成品統計 DTO */
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class TopProductDto {
    /** 成品名稱 */
    private String prodName;

    /** 成品規格 / 產品類別 */
    private String prodType;

    /** 總核銷出口數量 */
    private BigDecimal totalVerifiedQty;

    /** 已核銷出口項次筆數 */
    private long verifyCount;

    /** 涉及的不重複出口報單張數 */
    private long docCount;
  }
}
