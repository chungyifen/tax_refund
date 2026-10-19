package com.fox.tax.modules.refund.repository;

import com.fox.tax.modules.refund.entity.TaxRefund;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

/** 退稅核銷紀錄資料存取介面 */
@Repository
public interface TaxRefundRepository
    extends JpaRepository<TaxRefund, Long>, JpaSpecificationExecutor<TaxRefund> {

  /** 依據報表生成號碼查詢退稅紀錄（一併載入關聯實體避免 N+1 查詢） */
  @EntityGraph(attributePaths = {"exportDeclaration", "importDeclaration", "taxBom"})
  List<TaxRefund> findByReportNo(String reportNo);

  /** 依據出口報單 ID 查詢退稅紀錄（一併載入關聯實體避免 N+1 查詢） */
  @EntityGraph(attributePaths = {"exportDeclaration", "importDeclaration", "taxBom"})
  List<TaxRefund> findByExportDeclarationId(Long exportDeclarationId);

  /** 依據進口報單 ID 查詢退稅紀錄 */
  List<TaxRefund> findByImportDeclarationId(Long importDeclarationId);

  /** 查詢指定時間之後建立的退稅核銷紀錄（一併載入出口報單關聯） */
  @EntityGraph(attributePaths = {"exportDeclaration"})
  List<TaxRefund> findByCreateTimeGreaterThanEqual(java.util.Date startDate);

  /**
   * 查詢核銷總數量前 N 名的原料統計（直接由進口報單累計核銷量聚合，避免百萬筆退稅明細 JOIN 全表掃描）
   *
   * <p>回傳欄位依序為：materialName, materialUnit, totalVerifiedQty, verifyCount, docCount
   */
  @org.springframework.data.jpa.repository.Query(
      "SELECT i.materialName, MAX(i.materialUnit), SUM(i.totalRefundQty), "
          + "COUNT(i.id), COUNT(DISTINCT i.docNo) "
          + "FROM ImportDeclaration i "
          + "WHERE i.totalRefundQty > 0 "
          + "GROUP BY i.materialName "
          + "ORDER BY SUM(i.totalRefundQty) DESC")
  List<Object[]> findTopVerifiedMaterialsByQty(org.springframework.data.domain.Pageable pageable);

  /**
   * 查詢核銷筆數前 N 名的原料統計（直接由進口報單累計核銷量聚合，避免百萬筆退稅明細 JOIN 全表掃描）
   *
   * <p>回傳欄位依序為：materialName, materialUnit, totalVerifiedQty, verifyCount, docCount
   */
  @org.springframework.data.jpa.repository.Query(
      "SELECT i.materialName, MAX(i.materialUnit), SUM(i.totalRefundQty), "
          + "COUNT(i.id), COUNT(DISTINCT i.docNo) "
          + "FROM ImportDeclaration i "
          + "WHERE i.totalRefundQty > 0 "
          + "GROUP BY i.materialName "
          + "ORDER BY COUNT(i.id) DESC, SUM(i.totalRefundQty) DESC")
  List<Object[]> findTopVerifiedMaterialsByCount(
      org.springframework.data.domain.Pageable pageable);
}


