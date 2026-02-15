package com.fox.tax.modules.refund.dto;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;

/**
 * 產生退稅清單的結果 DTO
 * 包含成功筆數、報表號碼、以及警告訊息
 */
@Data
public class GenerateRefundResultDto {

    /** 成功建立的退稅紀錄筆數 */
    private int successCount;

    /** 報表生成號碼 */
    private String reportNo;

    /** 警告訊息列表（如 BOM 找不到、庫存不足等） */
    private List<String> warnings = new ArrayList<>();

    /**
     * 新增一筆警告訊息
     */
    public void addWarning(String warning) {
        this.warnings.add(warning);
    }
}
