package com.fox.tax.modules.refund.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fox.tax.modules.refund.dto.GenerateRefundResultDto;
import com.fox.tax.modules.refund.entity.ExportDeclaration;
import com.fox.tax.modules.refund.entity.ImportDeclaration;
import com.fox.tax.modules.refund.entity.TaxBom;
import com.fox.tax.modules.refund.entity.TaxRefund;
import com.fox.tax.modules.refund.repository.ExportDeclarationRepository;
import com.fox.tax.modules.refund.repository.ImportDeclarationRepository;
import com.fox.tax.modules.refund.repository.TaxBomRepository;
import com.fox.tax.modules.refund.repository.TaxRefundRepository;

import lombok.extern.slf4j.Slf4j;

/**
 * 退稅核銷服務
 * 處理產生退稅清單的核心邏輯
 */
@Slf4j
@Service
@Transactional(readOnly = true, rollbackFor = RuntimeException.class)
public class TaxRefundService {

    @Autowired
    private TaxRefundRepository taxRefundRepository;

    @Autowired
    private ExportDeclarationRepository exportDeclarationRepository;

    @Autowired
    private ImportDeclarationRepository importDeclarationRepository;

    @Autowired
    private TaxBomRepository taxBomRepository;

    /**
     * 產生退稅清單
     * 依據出口報單號碼進行核銷作業
     * 
     * @param docNo 出口報單號碼
     * @return 產生結果 DTO，包含成功筆數、報表號碼、警告訊息
     */
    @Transactional
    public GenerateRefundResultDto generateRefundList(String docNo) {
        log.info("開始產生退稅清單，出口報單號碼: {}", docNo);
        GenerateRefundResultDto result = new GenerateRefundResultDto();
        int successCount = 0;

        // 1. 產生報表號碼 (格式: YYYYMMDD-HHmmss)
        String reportNo = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        result.setReportNo(reportNo);

        // 2. 依據報單號碼查詢所有出口項次
        List<ExportDeclaration> exportList = exportDeclarationRepository.findByDocNo(docNo);
        if (exportList.isEmpty()) {
            throw new RuntimeException("找不到出口報單: " + docNo);
        }

        // 3. 逐一處理每個出口項次
        for (ExportDeclaration export : exportList) {
            // 檢查狀態，已產生核銷清單的跳過
            if (export.getStatus() >= ExportDeclaration._Status_Create_Refund) {
                log.info("出口項次 {} 已產生核銷清單，跳過", export.getItems());
                result.addWarning("出口項次 " + export.getItems() + " 已產生核銷清單，跳過");
                continue;
            }

            // 4. 利用產品類別與產品名稱找出核銷BOM
            List<TaxBom> bomList = taxBomRepository.findByProdTypeAndProdName(
                    export.getProdType(), export.getProdName());

            if (bomList.isEmpty()) {
                log.warn("找不到對應的BOM: prodType={}, prodName={}",
                        export.getProdType(), export.getProdName());
                result.addWarning("項次 " + export.getItems() + " 找不到對應的核退標準 BOM (規格="
                        + export.getProdType() + ", 品名=" + export.getProdName() + ")");
                continue;
            }

            // 5. 依據BOM逐一核銷原料
            int itemRefundCount = 0; // 記錄此出口項次產生的退稅筆數

            for (TaxBom bom : bomList) {
                // 計算需核銷的原料數量 = 出口數量 * BOM使用量
                BigDecimal requiredQty = export.getExportQty().multiply(bom.getUsageQty());
                log.info("原料 {} 需核銷數量: {}", bom.getMaterialName(), requiredQty);

                // 6. 搜尋進口報單項次進行核銷 (依據原料名稱和規格，FIFO)
                // TaxBom.materialSpec 可能包含多個規格，用逗號分隔
                List<ImportDeclaration> importList = new ArrayList<>();
                String[] specs = bom.getMaterialSpec().split(",");
                for (String spec : specs) {
                    String trimmedSpec = spec.trim();
                    List<ImportDeclaration> specImports = importDeclarationRepository
                            .findByMaterialNameAndMaterialSpecOrderByIdAsc(
                                    bom.getMaterialName(), trimmedSpec);
                    importList.addAll(specImports);
                }

                if (importList.isEmpty()) {
                    log.warn("找不到對應的進口報單: materialName={}, materialSpec={}",
                            bom.getMaterialName(), bom.getMaterialSpec());
                    result.addWarning("項次 " + export.getItems() + " 原料「"
                            + bom.getMaterialName() + "」找不到對應的進口報單");
                    continue;
                }

                // branchNum 從 1 開始，每新增一筆紀錄 +1
                int branchNumCounter = 1;

                // 逐一從進口報單扣除
                BigDecimal remainingQty = requiredQty;
                for (ImportDeclaration importDec : importList) {
                    if (remainingQty.compareTo(BigDecimal.ZERO) <= 0) {
                        break; // 已核銷完畢
                    }

                    // 計算可用餘額 = 進口數量 - 已核銷數量
                    BigDecimal available = importDec.getImportQty()
                            .subtract(importDec.getTotalRefundQty());

                    if (available.compareTo(BigDecimal.ZERO) <= 0) {
                        continue; // 此進口報單已無餘額
                    }

                    // 本次核銷數量 = min(可用餘額, 剩餘需求)
                    BigDecimal usageQty = available.min(remainingQty);

                    // 建立退稅核銷紀錄
                    TaxRefund refund = TaxRefund.builder()
                            .reportNo(reportNo)
                            .docNo(bom.getDocNo()) // 工業局標準文號
                            .exportDeclaration(export)
                            .importDeclaration(importDec)
                            .taxBom(bom)
                            .usageQty(usageQty)
                            .branchNum(branchNumCounter++) // 原料分號，從1開始遞增
                            .build();

                    taxRefundRepository.save(refund);
                    successCount++;
                    itemRefundCount++;

                    // 更新進口報單已核銷數量
                    importDec.setTotalRefundQty(
                            importDec.getTotalRefundQty().add(usageQty));
                    importDeclarationRepository.save(importDec);

                    // 扣除剩餘需求
                    remainingQty = remainingQty.subtract(usageQty);

                    log.info("核銷紀錄: 進口報單={}-{}, 數量={}",
                            importDec.getDocNo(), importDec.getItems(), usageQty);
                }

                if (remainingQty.compareTo(BigDecimal.ZERO) > 0) {
                    log.warn("原料 {} 庫存不足，剩餘未核銷數量: {}",
                            bom.getMaterialName(), remainingQty);
                    result.addWarning("項次 " + export.getItems() + " 原料「"
                            + bom.getMaterialName() + "」庫存不足，剩餘未核銷數量: " + remainingQty);
                }
            }

            // 7. 更新出口報單狀態 (僅當有產生核銷紀錄時才更新)
            if (itemRefundCount > 0) {
                export.setStatus(ExportDeclaration._Status_Create_Refund);
                exportDeclarationRepository.save(export);
            } else {
                result.addWarning("項次 " + export.getItems() + " 未產生任何核銷紀錄 (可能是找不到進口報單或庫存不足)");
            }

        }

        result.setSuccessCount(successCount);
        log.info("退稅清單產生完成，共 {} 筆紀錄", successCount);
        return result;
    }

    /**
     * 查詢退稅紀錄 by 報表號碼
     */
    public List<TaxRefund> findByReportNo(String reportNo) {
        return taxRefundRepository.findByReportNo(reportNo);
    }

    /**
     * 查詢退稅紀錄 by 出口報單ID
     */
    public List<TaxRefund> findByExportDeclarationId(Long exportDeclarationId) {
        return taxRefundRepository.findByExportDeclarationId(exportDeclarationId);
    }

    /**
     * 搜尋出口報單列表 (退稅清單功能用)
     * 
     * @param docNo    出口報單號碼 (模糊查詢)
     * @param status   狀態 (0:全部, 1:已匯入出口明細, 2:已產生核銷清單, 3:已產生核銷清單報表)
     * @param pageable 分頁資訊
     * @return 出口報單分頁結果
     */
    public org.springframework.data.domain.Page<ExportDeclaration> searchExports(
            String docNo, Integer status, org.springframework.data.domain.Pageable pageable) {

        return exportDeclarationRepository.findAll((root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();

            if (org.springframework.util.StringUtils.hasText(docNo)) {
                predicates.add(cb.like(root.get("docNo"), "%" + docNo + "%"));
            }

            if (status != null && status != 0) {
                // status 1, 2, 3 直接對應 DB 狀態值
                // ExportDeclaration._Status_Create = 1
                // ExportDeclaration._Status_Create_Refund = 2
                // ExportDeclaration._Status_Create_Refund_Report = 3
                predicates.add(cb.equal(root.get("status"), status));
            }

            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        }, pageable);
    }

    /**
     * 更新退稅核銷數量
     * 同步更新進口報單的已核銷數量
     * 
     * @param refundId 退稅紀錄ID
     * @param newQty   新的核銷數量
     */
    @Transactional
    public void updateRefundQty(Long refundId, BigDecimal newQty) {
        TaxRefund refund = taxRefundRepository.findById(refundId)
                .orElseThrow(() -> new RuntimeException("找不到退稅紀錄: " + refundId));

        ImportDeclaration importDec = refund.getImportDeclaration();
        BigDecimal oldQty = refund.getUsageQty();
        BigDecimal diff = newQty.subtract(oldQty);

        // 檢查進口報單剩餘數量是否足夠
        // 新的已核銷總量 = 目前已核銷總量 + 差異
        BigDecimal newTotalRefund = importDec.getTotalRefundQty().add(diff);

        if (newTotalRefund.compareTo(importDec.getImportQty()) > 0) {
            throw new RuntimeException("更新失敗：進口報單數量不足 (進口總量: "
                    + importDec.getImportQty() + ", 調整後已核銷: " + newTotalRefund + ")");
        }

        if (newTotalRefund.compareTo(BigDecimal.ZERO) < 0) {
            throw new RuntimeException("更新失敗：已核銷數量不能小於 0");
        }

        // 更新退稅紀錄
        refund.setUsageQty(newQty);
        taxRefundRepository.save(refund);

        // 更新進口報單
        importDec.setTotalRefundQty(newTotalRefund);
        importDeclarationRepository.save(importDec);

        log.info("更新退稅數量: refundId={}, oldQty={}, newQty={}, importDoc={}, newTotalRefund={}",
                refundId, oldQty, newQty, importDec.getDocNo(), newTotalRefund);
    }

    /**
     * 產生用料清表 (Report L)
     * 欄位順序 (A~V, 共22欄):
     *   報單項次, 工業局標準文號, 製造商統一編號, 成品貨物英文名稱, 成品規格,
     *   成品數量/重成品量, 成品單位, 成品牌名, 成品型號, 成品貨物中文名稱,
     *   原料序號, 原料分號, 原料名稱, 原料規格, 原料數量/重量(含損耗), 原料單位,
     *   進口商統一編號, 原料牌名, 原料型號, 進口報單號碼, 項次, 備註
     */
    @Transactional
    public Workbook exportReportL(String docNo) {
        List<ExportDeclaration> exportList = exportDeclarationRepository.findByDocNo(docNo);
        if (exportList.isEmpty()) {
            throw new RuntimeException("找不到出口報單: " + docNo);
        }

        HSSFWorkbook workbook = new HSSFWorkbook();
        Sheet sheet = workbook.createSheet("用料清表");

        // Row 0: 出口報單號碼
        Row row0 = sheet.createRow(0);
        row0.createCell(0).setCellValue("出口報單號碼");
        row0.createCell(1).setCellValue(docNo);

        // Row 1: Headers (A~V, 共22欄)
        Row header = sheet.createRow(1);
        String[] headers = {
                "報單項次",                // A
                "工業局標準文號",            // B
                "製造商統一編號",            // C
                "成品貨物英文名稱",          // D
                "成品規格",                // E
                "成品數量/重成品量",          // F
                "成品單位",                // G
                "成品牌名",                // H
                "成品型號",                // I
                "成品貨物中文名稱",          // J
                "原料序號",                // K
                "原料分號",                // L
                "原料名稱",                // M
                "原料規格",                // N
                "原料數量/重量(含損耗)",      // O
                "原料單位",                // P
                "進口商統一編號",            // Q
                "原料牌名",                // R
                "原料型號",                // S
                "進口報單號碼",             // T
                "項次",                   // U
                "備註"                    // V
        };
        for (int i = 0; i < headers.length; i++) {
            header.createCell(i).setCellValue(headers[i]);
        }

        int rowNum = 2;
        for (ExportDeclaration export : exportList) {
            List<TaxRefund> refundList = export.getTaxRefundList();
            if (refundList == null)
                continue;

            for (TaxRefund refund : refundList) {
                Row row = sheet.createRow(rowNum++);
                TaxBom bom = refund.getTaxBom();
                ImportDeclaration importDec = refund.getImportDeclaration();

                int col = 0;
                row.createCell(col++).setCellValue(export.getItems());                          // A 報單項次
                row.createCell(col++).setCellValue(refund.getDocNo());                          // B 工業局標準文號
                row.createCell(col++).setCellValue(TaxRefund._fox_uniform_numbers);             // C 製造商統一編號
                row.createCell(col++).setCellValue(export.getProdName());                       // D 成品貨物英文名稱
                row.createCell(col++).setCellValue(export.getProdType());                       // E 成品規格
                row.createCell(col++).setCellValue(export.getExportQty().doubleValue());        // F 成品數量/重成品量
                row.createCell(col++).setCellValue(
                        bom.getProdUnit() != null ? bom.getProdUnit() : "SET");                 // G 成品單位
                row.createCell(col++).setCellValue("");                                         // H 成品牌名
                row.createCell(col++).setCellValue("");                                         // I 成品型號
                row.createCell(col++).setCellValue("");                                         // J 成品貨物中文名稱
                row.createCell(col++).setCellValue(bom.getMaterialNum());                       // K 原料序號
                row.createCell(col++).setCellValue(refund.getBranchNum());                      // L 原料分號
                row.createCell(col++).setCellValue(bom.getMaterialName());                      // M 原料名稱
                row.createCell(col++).setCellValue(bom.getMaterialSpec());                      // N 原料規格
                row.createCell(col++).setCellValue(refund.getUsageQty().doubleValue());         // O 原料數量/重量(含損耗)
                row.createCell(col++).setCellValue(bom.getMaterialUnit());                      // P 原料單位
                row.createCell(col++).setCellValue(TaxRefund._fox_uniform_numbers);             // Q 進口商統一編號
                row.createCell(col++).setCellValue("");                                         // R 原料牌名
                row.createCell(col++).setCellValue("");                                         // S 原料型號
                row.createCell(col++).setCellValue(importDec.getDocNo());                       // T 進口報單號碼
                row.createCell(col++).setCellValue(importDec.getItems());                       // U 項次
                row.createCell(col++).setCellValue("");                                         // V 備註
            }
        }

        updateStatusToReported(exportList);
        return workbook;
    }

    /**
     * 產生沖退稅申請 (Report A)
     */
    @Transactional
    public Workbook exportReportA(String docNo) {
        List<ExportDeclaration> exportList = exportDeclarationRepository.findByDocNo(docNo);
        if (exportList.isEmpty()) {
            throw new RuntimeException("找不到出口報單: " + docNo);
        }

        HSSFWorkbook workbook = new HSSFWorkbook();
        Sheet sheet = workbook.createSheet("沖退稅申請");

        // Row 0: 出口報單號碼
        Row row0 = sheet.createRow(0);
        row0.createCell(0).setCellValue("出口報單號碼");
        row0.createCell(1).setCellValue(docNo);

        // Row 1: Headers
        Row header = sheet.createRow(1);
        String[] headers = { "報單項次", "原料序號", "原料分號", "進口報單號碼", "項次", "申退數量/重量" };
        for (int i = 0; i < headers.length; i++) {
            header.createCell(i).setCellValue(headers[i]);
        }

        int rowNum = 2;
        for (ExportDeclaration export : exportList) {
            List<TaxRefund> refundList = export.getTaxRefundList();
            if (refundList == null)
                continue;

            for (TaxRefund refund : refundList) {
                Row row = sheet.createRow(rowNum++);
                TaxBom bom = refund.getTaxBom();
                ImportDeclaration importDec = refund.getImportDeclaration();

                row.createCell(0).setCellValue(export.getItems());
                row.createCell(1).setCellValue(bom.getMaterialNum());
                row.createCell(2).setCellValue(refund.getBranchNum()); // 原料分號
                row.createCell(3).setCellValue(importDec.getDocNo());
                row.createCell(4).setCellValue(importDec.getItems());
                row.createCell(5).setCellValue(refund.getUsageQty().doubleValue());
            }
        }

        updateStatusToReported(exportList);
        return workbook;
    }

    private void updateStatusToReported(List<ExportDeclaration> exportList) {
        for (ExportDeclaration export : exportList) {
            if (export.getStatus() != null && export.getStatus() < ExportDeclaration._Status_Create_Refund_Report) {
                export.setStatus(ExportDeclaration._Status_Create_Refund_Report);
                exportDeclarationRepository.save(export);
            }
        }
    }
}
