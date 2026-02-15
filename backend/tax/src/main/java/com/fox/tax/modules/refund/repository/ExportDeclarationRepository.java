package com.fox.tax.modules.refund.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.fox.tax.modules.refund.entity.ExportDeclaration;

import java.util.List;

@Repository
public interface ExportDeclarationRepository
        extends JpaRepository<ExportDeclaration, Long>, JpaSpecificationExecutor<ExportDeclaration> {

    boolean existsByDocNoAndItems(String docNo, String items);

    // 依據報單號碼查詢所有項次
    List<ExportDeclaration> findByDocNo(String docNo);

    @org.springframework.data.jpa.repository.Query("SELECT DISTINCT e.docNo FROM ExportDeclaration e ORDER BY e.docNo DESC")
    List<String> findDistinctDocNos();
}
