package com.fox.tax.modules.refund.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.fox.tax.modules.refund.entity.TaxRefund;

import java.util.List;

@Repository
public interface TaxRefundRepository
        extends JpaRepository<TaxRefund, Long>, JpaSpecificationExecutor<TaxRefund> {

    // 依據報表生成號碼查詢
    List<TaxRefund> findByReportNo(String reportNo);

    // 依據出口報單ID查詢
    List<TaxRefund> findByExportDeclarationId(Long exportDeclarationId);

    // 依據進口報單ID查詢
    List<TaxRefund> findByImportDeclarationId(Long importDeclarationId);
}
