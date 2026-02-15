package com.fox.tax.modules.refund.entity;

import java.math.BigDecimal;

import com.fox.tax.common.entity.AbstractPersistable;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.CascadeType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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
@Table(name = "tax_refund")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class TaxRefund extends AbstractPersistable {

	public static String _fox_uniform_numbers = "53019078";

	// 報表生成號碼
	@Column(name = "report_no", length = 30, nullable = false)
	private String reportNo;

	// 工業局標準文號
	@Column(name = "doc_no", length = 30, nullable = false)
	private String docNo;

	@JsonIgnoreProperties("taxRefundList")
	@ManyToOne(cascade = CascadeType.REFRESH, fetch = FetchType.LAZY)
	@JoinColumn(name = "export_declaration_id", nullable = false)
	private ExportDeclaration exportDeclaration;

	@JsonIgnoreProperties("taxRefundList")
	@ManyToOne(cascade = CascadeType.REFRESH, fetch = FetchType.LAZY)
	@JoinColumn(name = "import_declaration_id", nullable = false)
	private ImportDeclaration importDeclaration;

	@Column(name = "usage_qty", precision = 9, scale = 3, columnDefinition = "numeric(9,3)", nullable = false)
	private BigDecimal usageQty;

	// 原料分號
	@Column(name = "branch_num", nullable = false)
	private Integer branchNum;

	@ManyToOne(cascade = CascadeType.REFRESH, fetch = FetchType.LAZY)
	@JoinColumn(name = "tax_bom_id")
	private TaxBom taxBom;

	// public TaxRefundVo toVo() {
	// TaxRefundVo vo = new TaxRefundVo();
	// BeanUtils.copyProperties(this, vo);
	// BeanUtils.copyProperties(this.getExportDeclaration(),
	// vo.getExportDeclarationVo());
	// BeanUtils.copyProperties(this.getImportDeclaration(),
	// vo.getImportDeclarationVo());
	// BeanUtils.copyProperties(this.getTaxBom(), vo.getTaxBomVo());
	// return vo;
	// }

}
