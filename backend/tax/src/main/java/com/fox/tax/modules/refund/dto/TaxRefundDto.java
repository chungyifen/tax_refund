package com.fox.tax.modules.refund.dto;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 退稅核銷紀錄資料傳輸物件 (DTO)
 *
 * <p>用於封裝退稅核銷紀錄及其關聯的出口報單、進口報單與退稅標準 (BOM) 資訊，避免直接暴露 Entity。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaxRefundDto {

  /** 退稅紀錄 ID */
  private Long id;

  /** 報表生成號碼 */
  private String reportNo;

  /** 工業局標準文號 */
  private String docNo;

  /** 核銷使用數量 */
  private BigDecimal usageQty;

  /** 原料分號 */
  private Integer branchNum;

  /** 關聯出口報單資訊 */
  private ExportDeclarationDto exportDeclaration;

  /** 關聯進口報單資訊（含進口總量與已核銷數量） */
  private ImportDeclarationDto importDeclaration;

  /** 關聯退稅標準 (BOM) 資訊 */
  private TaxBomDto taxBom;
}
