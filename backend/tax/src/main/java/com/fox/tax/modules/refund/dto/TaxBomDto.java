package com.fox.tax.modules.refund.dto;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class TaxBomDto {
    private Long id;
    private String docNo;
    private String prodType;
    private String prodName;
    private String prodUnit;
    private Integer materialNum;
    private String materialName;
    private String materialUnit;
    private String materialSpec;
    private BigDecimal usageQty;
}
