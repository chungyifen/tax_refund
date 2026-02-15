package com.fox.tax.modules.refund.entity;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import com.fox.tax.common.entity.AbstractPersistable;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * @author jeff
 */
@Entity
@Table(name = "tax_bom")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class TaxBom extends AbstractPersistable {

	// 工業局標準文號
	@Column(name = "doc_no", length = 30, nullable = false)
	private String docNo;
	// 產品類別
	@Column(name = "prod_type", length = 30, nullable = false)
	private String prodType;
	// 產品名稱
	@Column(name = "prod_name", length = 50, nullable = false)
	private String prodName;
	// 產品單位 預設為SET
	@Column(name = "prod_unit", length = 10)
	private String prodUnit;

	// 序號
	@Column(name = "material_num", nullable = false)
	private Integer materialNum;
	// 原料名稱
	@Column(name = "material_name", length = 50, nullable = false)
	private String materialName;
	// 原料單位
	@Column(name = "material_unit", length = 10, nullable = false)
	private String materialUnit;
	// 原料規格
	@Column(name = "material_spec", length = 300, nullable = false)
	private String materialSpec;
	// 使用量
	@Column(name = "usage_qty", precision = 9, scale = 3, columnDefinition = "numeric(9,3)", nullable = false)
	private BigDecimal usageQty;
	// 退稅申請單
	@OneToMany(mappedBy = "taxBom", cascade = CascadeType.REFRESH, fetch = FetchType.LAZY)
	@Builder.Default
	private List<TaxRefund> taxRefundList = new ArrayList<>();

}
