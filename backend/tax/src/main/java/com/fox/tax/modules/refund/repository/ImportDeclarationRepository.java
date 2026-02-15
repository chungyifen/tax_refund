package com.fox.tax.modules.refund.repository;

import com.fox.tax.modules.refund.entity.ImportDeclaration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ImportDeclarationRepository
        extends JpaRepository<ImportDeclaration, Long>, JpaSpecificationExecutor<ImportDeclaration> {

    boolean existsByDocNoAndItems(String docNo, String items);

    // 依據原料名稱和規格查詢，依ID排序 (FIFO)
    List<ImportDeclaration> findByMaterialNameAndMaterialSpecOrderByIdAsc(String materialName, String materialSpec);

    // 查詢所有不重複的進口報單號碼，供 auto-complete 下拉選單使用
    @Query("SELECT DISTINCT d.docNo FROM ImportDeclaration d ORDER BY d.docNo")
    List<String> findDistinctDocNos();
}
