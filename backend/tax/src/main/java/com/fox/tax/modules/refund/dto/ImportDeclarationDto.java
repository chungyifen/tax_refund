package com.fox.tax.modules.refund.dto;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class ImportDeclarationDto {
    private Long id;
    private String docNo;
    private String items; // Item No
    private String materialName;
    private String materialUnit;
    private String materialSpec;
    private BigDecimal importQty;
    private BigDecimal totalRefundQty; // Verified Qty

    // Calculated fields
    private BigDecimal unverifiedQty;

    // 0: All (Not used in DTO usually, but for filter), 1: Unfinished, 2: Finished
    // Here we can return a status string or code
    private Integer verificationStatus; // 1: Unfinished, 2: Finished
}
