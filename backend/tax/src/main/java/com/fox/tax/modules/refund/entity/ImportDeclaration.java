package com.fox.tax.modules.refund.entity;

import java.math.BigDecimal;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;

import com.fox.tax.common.entity.AbstractPersistable;

import lombok.EqualsAndHashCode;

/**
 * @author jeff
 */
@Entity
@Table(name = "import_declaration")
@Data
@EqualsAndHashCode(callSuper = false)
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ImportDeclaration extends AbstractPersistable {

	// 進口報單號碼
	@Column(name = "doc_no", length = 30, nullable = false)
	private String docNo;
	// 報單項次
	@Column(name = "items", length = 4, nullable = false)
	private String items;

	@Column(name = "material_name", length = 50, nullable = false)
	private String materialName;

	@Column(name = "material_unit", length = 10)
	private String materialUnit;

	@Column(name = "material_spec", length = 200, nullable = false)
	private String materialSpec;

	@Column(name = "import_qty", precision = 9, scale = 3, columnDefinition = "numeric(9,3)", nullable = false)
	private BigDecimal importQty;

	@OneToMany(mappedBy = "importDeclaration", cascade = CascadeType.REFRESH, fetch = FetchType.LAZY)
	private List<TaxRefund> taxRefundList;
	@Column(name = "total_refund_qty", precision = 9, scale = 3, columnDefinition = "numeric(9,3) default 0", nullable = false)
	private BigDecimal totalRefundQty;

	public BigDecimal countTotalRefundQty() {
		if (CollectionUtils.isNotEmpty(this.taxRefundList)) {
			return this.taxRefundList.stream().map(taxRefund -> taxRefund.getUsageQty()).reduce((x, y) -> x.add(y))
					.get();
		} else {
			return BigDecimal.ZERO;
		}
	}

}
