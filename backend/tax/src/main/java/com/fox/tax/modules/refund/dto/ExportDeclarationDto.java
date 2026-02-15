package com.fox.tax.modules.refund.dto;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class ExportDeclarationDto {
    private Long id;
    private String docNo;
    private String items;
    private String prodType;
    private String prodName;
    private BigDecimal exportQty;
    private Integer status; // 1: Create, 2: Refund, 3: Report
}
