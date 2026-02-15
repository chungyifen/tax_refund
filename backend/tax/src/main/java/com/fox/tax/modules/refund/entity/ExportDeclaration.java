package com.fox.tax.modules.refund.entity;

import java.math.BigDecimal;
import java.util.List;

import com.fox.tax.common.entity.AbstractPersistable;
import com.fasterxml.jackson.annotation.JsonIgnore;

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
@Table(name = "export_declaration")
@Data
@EqualsAndHashCode(callSuper = false)
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ExportDeclaration extends AbstractPersistable {

	public static int _Status_Create = 1;
	public static int _Status_Create_Refund = 2;
	public static int _Status_Create_Refund_Report = 3;

	// 進口報單號碼
	@Column(name = "doc_no", length = 30, nullable = false)
	private String docNo;
	// 報單項次
	@Column(name = "items", length = 4, nullable = false)
	private String items;

	@Column(name = "prod_type", length = 30, nullable = false)
	private String prodType;

	@Column(name = "prod_name", length = 50, nullable = false)
	private String prodName;

	@Column(name = "export_qty", precision = 9, scale = 3, columnDefinition = "numeric(9,3)", nullable = false)
	private BigDecimal exportQty;

	@Column(name = "status", nullable = false)
	private Integer status;

	@JsonIgnore
	@OneToMany(mappedBy = "exportDeclaration", cascade = CascadeType.REFRESH, fetch = FetchType.LAZY)
	private List<TaxRefund> taxRefundList;

}
