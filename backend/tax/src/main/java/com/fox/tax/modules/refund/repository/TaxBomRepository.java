package com.fox.tax.modules.refund.repository;

import com.fox.tax.modules.refund.entity.TaxBom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaxBomRepository extends JpaRepository<TaxBom, Long>, QuerydslPredicateExecutor<TaxBom> {

    // 依據文號查詢 (完全匹配)
    List<TaxBom> findByDocNo(String docNo);

    // 依據產品名稱查詢 (模糊搜尋)
    List<TaxBom> findByProdNameContaining(String prodName);

    // 綜合搜尋：文號 OR 產品名稱 OR 原料名稱
    @Query("SELECT t FROM TaxBom t WHERE " +
            "t.docNo LIKE %:keyword% OR " +
            "t.prodName LIKE %:keyword% OR " +
            "t.materialName LIKE %:keyword%")
    List<TaxBom> searchByKeyword(String keyword);

    // 檢查是否存在重複的 (文號 + 序號) - 防止資料重複匯入
    boolean existsByDocNoAndMaterialNum(String docNo, Integer materialNum);

    // 模糊搜尋：文號 AND 產品類別 AND 產品名稱
    List<TaxBom> findByDocNoLikeAndProdTypeLikeAndProdNameLike(String docNo, String prodType, String prodName);

    // 完全匹配：產品類別 AND 產品名稱
    List<TaxBom> findByProdTypeAndProdName(String prodType, String prodName);
}