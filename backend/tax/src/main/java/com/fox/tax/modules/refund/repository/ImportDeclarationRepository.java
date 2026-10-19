package com.fox.tax.modules.refund.repository;

import com.fox.tax.modules.refund.entity.ImportDeclaration;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

/** 進口報單資料存取介面 */
@Repository
public interface ImportDeclarationRepository
    extends JpaRepository<ImportDeclaration, Long>, JpaSpecificationExecutor<ImportDeclaration> {

  /** 檢查指定報單號碼與項次是否已存在 */
  boolean existsByDocNoAndItems(String docNo, String items);

  /** 依據原料名稱和單一規格查詢，依 ID 升冪排序 (FIFO) */
  List<ImportDeclaration> findByMaterialNameAndMaterialSpecOrderByIdAsc(
      String materialName, String materialSpec);

  /**
   * 依據原料名稱與多個規格清單查詢，依 ID 升冪排序實現跨規格先進先出 (FIFO)，
   * 並使用悲觀寫入鎖防止併發核銷時發生超額扣抵。
   */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  List<ImportDeclaration> findByMaterialNameAndMaterialSpecInOrderByIdAsc(
      String materialName, Collection<String> materialSpecs);

  /** 查詢所有不重複的進口報單號碼，供前端 auto-complete 下拉選單使用 */
  @Query("SELECT DISTINCT d.docNo FROM ImportDeclaration d ORDER BY d.docNo")
  List<String> findDistinctDocNos();

  /** 依據建立時間查詢新增的進口報單明細（依建立時間降冪、ID 降冪排序） */
  List<ImportDeclaration> findByCreateTimeGreaterThanEqualOrderByCreateTimeDescIdDesc(
      java.util.Date startDate);

  /** 統計目前尚有未核銷餘額（進口數量 > 已核銷數量）的不重複進口報單號碼數目 */
  @Query(
      "SELECT COUNT(DISTINCT d.docNo) FROM ImportDeclaration d "
          + "WHERE d.importQty > d.totalRefundQty")
  long countDistinctDocNoWithUnverifiedItems();

  /** 統計目前尚有未核銷餘額（進口數量 > 已核銷數量）的進口報單明細項次數目 */
  @Query("SELECT COUNT(d) FROM ImportDeclaration d WHERE d.importQty > d.totalRefundQty")
  long countUnverifiedItems();

  /** 統計全系統累計進口總數量與已核銷總數量 */
  @Query(
      "SELECT COALESCE(SUM(d.importQty), 0), COALESCE(SUM(d.totalRefundQty), 0) "
          + "FROM ImportDeclaration d")
  List<Object[]> sumTotalImportAndRefundQty();

  /**
   * 依據進口報單已核銷數量統計前 N 名原料（作為無退稅明細紀錄時的備援統計）
   *
   * <p>回傳欄位依序為：materialName, materialUnit, totalVerifiedQty, verifyCount, docCount
   */
  @Query(
      "SELECT d.materialName, MAX(d.materialUnit), SUM(d.totalRefundQty), "
          + "COUNT(d.id), COUNT(DISTINCT d.docNo) "
          + "FROM ImportDeclaration d "
          + "WHERE d.totalRefundQty > 0 "
          + "GROUP BY d.materialName "
          + "ORDER BY SUM(d.totalRefundQty) DESC")
  List<Object[]> findTopVerifiedMaterialsFromImport(
      org.springframework.data.domain.Pageable pageable);
}


