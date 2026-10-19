package com.fox.tax.modules.refund.repository;

import com.fox.tax.modules.refund.entity.ExportDeclaration;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/** 出口報單資料存取介面 */
@Repository
public interface ExportDeclarationRepository
    extends JpaRepository<ExportDeclaration, Long>, JpaSpecificationExecutor<ExportDeclaration> {

  /** 檢查指定出口報單號碼與項次是否已存在 */
  boolean existsByDocNoAndItems(String docNo, String items);

  /** 依據報單號碼查詢所有項次 */
  List<ExportDeclaration> findByDocNo(String docNo);

  /**
   * 依據報單號碼查詢所有項次並一次載入退稅紀錄、BOM 與進口報單關聯，
   * 避免匯出報表 (Report L / Report A) 時產生 N+1 Lazy Loading 查詢。
   */
  @EntityGraph(
      attributePaths = {
        "taxRefundList",
        "taxRefundList.taxBom",
        "taxRefundList.importDeclaration"
      })
  @Query("SELECT DISTINCT e FROM ExportDeclaration e WHERE e.docNo = :docNo ORDER BY e.id ASC")
  List<ExportDeclaration> findWithRefundsByDocNo(@Param("docNo") String docNo);

  /** 查詢所有不重複的出口報單號碼，依號碼降冪排序 */
  @Query("SELECT DISTINCT e.docNo FROM ExportDeclaration e ORDER BY e.docNo DESC")
  List<String> findDistinctDocNos();

  /**
   * 查詢指定起始時間以來狀態更新或建立的已核銷出口報單項次（搭配索引快速過濾）
   */
  @Query(
      "SELECT e FROM ExportDeclaration e "
          + "WHERE e.status >= 2 "
          + "AND (e.updateTime >= :startDate OR e.createTime >= :startDate) "
          + "ORDER BY e.id DESC")
  List<ExportDeclaration> findWeeklyVerifiedExports(@Param("startDate") java.util.Date startDate);

  /**
   * 查詢核銷總出口數量前 N 名的成品統計（狀態 >= 2 已核銷）
   *
   * <p>回傳欄位依序為：prodName, prodType, totalVerifiedQty, verifyCount, docCount
   */
  @Query(
      "SELECT e.prodName, e.prodType, SUM(e.exportQty), COUNT(e.id), COUNT(DISTINCT e.docNo) "
          + "FROM ExportDeclaration e "
          + "WHERE e.status >= 2 "
          + "GROUP BY e.prodName, e.prodType "
          + "ORDER BY SUM(e.exportQty) DESC")
  List<Object[]> findTopVerifiedProductsByQty(org.springframework.data.domain.Pageable pageable);

  /**
   * 查詢核銷項次筆數前 N 名的成品統計（狀態 >= 2 已核銷）
   *
   * <p>回傳欄位依序為：prodName, prodType, totalVerifiedQty, verifyCount, docCount
   */
  @Query(
      "SELECT e.prodName, e.prodType, SUM(e.exportQty), COUNT(e.id), COUNT(DISTINCT e.docNo) "
          + "FROM ExportDeclaration e "
          + "WHERE e.status >= 2 "
          + "GROUP BY e.prodName, e.prodType "
          + "ORDER BY COUNT(e.id) DESC, SUM(e.exportQty) DESC")
  List<Object[]> findTopVerifiedProductsByCount(org.springframework.data.domain.Pageable pageable);

  /** 依據狀態統計不重複出口報單號碼數目 */
  @Query("SELECT COUNT(DISTINCT e.docNo) FROM ExportDeclaration e WHERE e.status = :status")
  long countDistinctDocNoByStatus(@Param("status") Integer status);

  /** 依據狀態統計出口報單明細項次數目 */
  long countByStatus(Integer status);
}


