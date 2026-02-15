package com.fox.tax.modules.refund.service;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.util.StringUtils;

import com.fox.tax.modules.refund.dto.ImportDeclarationDto;
import com.fox.tax.modules.refund.dto.ImportResultDto;
import com.fox.tax.modules.refund.entity.ImportDeclaration;
import com.fox.tax.modules.refund.mapper.ImportDeclarationMapper;
import com.fox.tax.modules.refund.repository.ImportDeclarationRepository;

import jakarta.persistence.criteria.Predicate;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@Transactional(readOnly = true, rollbackFor = RuntimeException.class)
public class ImportDeclarationService {

    @Autowired
    private ImportDeclarationRepository repository;

    @Autowired
    private ImportDeclarationMapper mapper;

    public Page<ImportDeclarationDto> search(String docNo, String materialName, Integer status, Pageable pageable) {
        Specification<ImportDeclaration> spec = buildSearchSpec(docNo, materialName, status);
        return repository.findAll(spec, pageable).map(this::enrichDto);
    }

    private Specification<ImportDeclaration> buildSearchSpec(String docNo, String materialName, Integer status) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (StringUtils.hasText(docNo)) {
                predicates.add(cb.like(root.get("docNo"), "%" + docNo + "%"));
            }

            if (StringUtils.hasText(materialName)) {
                predicates.add(cb.like(root.get("materialName"), "%" + materialName + "%"));
            }

            // Status: 0: All, 1: Unfinished, 2: Finished
            if (status != null && status != 0) {
                if (status == 1) { // Unfinished
                    predicates.add(cb.greaterThan(
                            cb.diff(root.get("importQty"), root.get("totalRefundQty")), 0));
                } else if (status == 2) { // Finished
                    predicates.add(cb.lessThanOrEqualTo(
                            cb.diff(root.get("importQty"), root.get("totalRefundQty")), 0));
                }
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private ImportDeclarationDto enrichDto(ImportDeclaration entity) {
        ImportDeclarationDto dto = mapper.toDto(entity);
        if (dto.getImportQty() != null) {
            BigDecimal refundQty = dto.getTotalRefundQty() != null ? dto.getTotalRefundQty() : BigDecimal.ZERO;
            dto.setUnverifiedQty(dto.getImportQty().subtract(refundQty));

            if (dto.getUnverifiedQty().compareTo(BigDecimal.ZERO) > 0) {
                dto.setVerificationStatus(1); // Unfinished
            } else {
                dto.setVerificationStatus(2); // Finished
            }
        }
        return dto;
    }

    /**
     * 從 colMap 中依據多個可能的別名查找欄位索引
     * 例如「報單號碼」可能在 Excel 中叫做「報單號碼」或「進口報單號碼」
     */
    private Integer findColumnIndex(java.util.Map<String, Integer> colMap, String... aliases) {
        for (String alias : aliases) {
            Integer idx = colMap.get(alias);
            if (idx != null)
                return idx;
        }
        return null;
    }

    @Transactional
    public ImportResultDto importExcel(MultipartFile file) throws IOException {
        ImportResultDto result = new ImportResultDto();
        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);

            // 自動尋找表頭列 (搜尋前 10 列，找到包含「報單號碼」的列)
            Row headerRow = null;
            int headerRowNum = 0;
            java.util.Map<String, Integer> colMap = new java.util.HashMap<>();

            for (int i = 0; i < 10 && i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null)
                    continue;

                // 檢查此列是否包含關鍵欄位「Declaration NO」或「報單號碼」
                boolean foundKey = false;
                for (Cell cell : row) {
                    String val = cell != null ? cell.toString().trim() : "";
                    if (val.equals("Declaration NO") || val.contains("報單號碼")) {
                        foundKey = true;
                        break;
                    }
                }

                if (foundKey) {
                    headerRow = row;
                    headerRowNum = i;
                    // 建立欄位索引 Map
                    for (Cell cell : headerRow) {
                        if (cell != null) {
                            String cellValue = cell.toString().trim();
                            colMap.put(cellValue, cell.getColumnIndex());
                        }
                    }
                    break;
                }
            }

            if (headerRow == null || colMap.isEmpty()) {
                result.addError("找不到表頭列。請確認檔案包含「Declaration NO」或「報單號碼」欄位。");
                return result;
            }

            log.info("偵測到表頭在第 {} 列，欄位: {}", headerRowNum, colMap.keySet());

            // 使用別名系統查找必要欄位
            // 報單號碼: "Declaration NO" 或 "報單號碼" 或 "進口報單號碼"
            Integer docNoIdx = findColumnIndex(colMap, "Declaration NO", "報單號碼", "進口報單號碼");
            // 報單項次: "報單項次" 或 "項次" 或 "ITEM"
            Integer itemsIdx = findColumnIndex(colMap, "報單項次", "項次", "ITEM");
            // 原料名稱: "原料名稱" 或 "退稅品名" 或 "品名"
            Integer materialNameIdx = findColumnIndex(colMap, "原料名稱", "退稅品名", "品名");
            // 進口數量: "進口數量" 或 "QTY" 或 "數量"
            Integer importQtyIdx = findColumnIndex(colMap, "進口數量", "QTY", "數量");

            // 檢查必要欄位是否都找得到
            List<String> missingHeaders = new ArrayList<>();
            if (docNoIdx == null)
                missingHeaders.add("報單號碼");
            if (itemsIdx == null)
                missingHeaders.add("報單項次/ITEM");
            if (materialNameIdx == null)
                missingHeaders.add("原料名稱/退稅品名");
            if (importQtyIdx == null)
                missingHeaders.add("進口數量/QTY");

            if (!missingHeaders.isEmpty()) {
                String foundHeaders = String.join(", ", colMap.keySet());
                result.addError("缺少必要表頭: " + String.join(", ", missingHeaders) + "。偵測到的表頭: [" + foundHeaders + "]");
                return result;
            }

            // 可選欄位
            // 原料規格: "原料規格" 或 "SPEC" 或 "規格"
            Integer materialSpecIdx = findColumnIndex(colMap, "原料規格", "SPEC", "規格");
            // 原料單位: "原料單位" 或 "單位" 或 "使用單位" 或 "UNIT"
            Integer materialUnitIdx = findColumnIndex(colMap, "原料單位", "單位", "使用單位", "UNIT");

            // 建立最終的欄位索引 Map (用標準名稱)
            java.util.Map<String, Integer> fieldMap = new java.util.HashMap<>();
            fieldMap.put("報單號碼", docNoIdx);
            fieldMap.put("報單項次", itemsIdx);
            fieldMap.put("原料名稱", materialNameIdx);
            fieldMap.put("進口數量", importQtyIdx);
            if (materialSpecIdx != null)
                fieldMap.put("原料規格", materialSpecIdx);
            if (materialUnitIdx != null)
                fieldMap.put("原料單位", materialUnitIdx);

            // 從表頭列的下一列開始讀取資料
            for (int i = headerRowNum + 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null)
                    continue;

                try {
                    ImportDeclaration entity = parseRow(row, fieldMap);

                    // 若整行都是空的，則跳過
                    if (!StringUtils.hasText(entity.getDocNo()) && !StringUtils.hasText(entity.getItems())
                            && entity.getImportQty() == null) {
                        continue;
                    }

                    // Validate required fields
                    List<String> missingFields = new ArrayList<>();
                    if (!StringUtils.hasText(entity.getDocNo()))
                        missingFields.add("報單號碼");
                    if (!StringUtils.hasText(entity.getItems()))
                        missingFields.add("報單項次");
                    if (!StringUtils.hasText(entity.getMaterialName()))
                        missingFields.add("原料名稱");
                    if (entity.getImportQty() == null)
                        missingFields.add("進口數量");

                    if (!missingFields.isEmpty()) {
                        throw new IllegalArgumentException("缺少必填欄位: " + String.join(", ", missingFields));
                    }

                    if (StringUtils.hasText(entity.getDocNo()) && StringUtils.hasText(entity.getItems())) {
                        boolean exists = repository.existsByDocNoAndItems(entity.getDocNo(), entity.getItems());
                        if (!exists) {
                            repository.save(entity);
                            result.addSuccess();
                        } else {
                            result.addError("第 " + (row.getRowNum() + 1) + " 列: 重複的報單號碼 " + entity.getDocNo()
                                    + " 與項次 " + entity.getItems());
                        }
                    }
                } catch (Exception e) {
                    String errorMsg = "第 " + (row.getRowNum() + 1) + " 列: " + e.getMessage();
                    result.addError(errorMsg);
                    log.error("Error importing row {}", row.getRowNum(), e);
                }
            }
        }
        return result;
    }

    /**
     * 解析一行 Excel 資料，fieldMap 的 key 為標準欄位名稱，value 為欄位索引
     */
    private ImportDeclaration parseRow(Row row, java.util.Map<String, Integer> fieldMap) {
        ImportDeclaration dec = new ImportDeclaration();

        // Doc No: 報單號碼
        Cell cellDocNo = getCellByField(row, fieldMap, "報單號碼");
        if (cellDocNo != null) {
            cellDocNo.setCellType(org.apache.poi.ss.usermodel.CellType.STRING);
            dec.setDocNo(cellDocNo.getStringCellValue().trim());
        }

        // Items: 報單項次
        Cell cellItems = getCellByField(row, fieldMap, "報單項次");
        if (cellItems != null) {
            cellItems.setCellType(org.apache.poi.ss.usermodel.CellType.STRING);
            dec.setItems(cellItems.getStringCellValue().trim());
        }

        // Material Name: 原料名稱
        Cell cellName = getCellByField(row, fieldMap, "原料名稱");
        if (cellName != null) {
            cellName.setCellType(org.apache.poi.ss.usermodel.CellType.STRING);
            dec.setMaterialName(cellName.getStringCellValue().trim());
        }

        // Material Spec: 原料規格 (Optional, default NIL)
        Cell cellSpec = getCellByField(row, fieldMap, "原料規格");
        if (cellSpec != null) {
            cellSpec.setCellType(org.apache.poi.ss.usermodel.CellType.STRING);
            dec.setMaterialSpec(cellSpec.getStringCellValue().trim());
        } else {
            dec.setMaterialSpec("NIL");
        }

        // Qty: 進口數量
        Cell cellQty = getCellByField(row, fieldMap, "進口數量");
        if (cellQty != null) {
            try {
                if (cellQty.getCellType() == org.apache.poi.ss.usermodel.CellType.NUMERIC) {
                    dec.setImportQty(BigDecimal.valueOf(cellQty.getNumericCellValue()));
                } else if (cellQty.getCellType() == org.apache.poi.ss.usermodel.CellType.STRING) {
                    String qtyStr = cellQty.getStringCellValue().trim();
                    if (StringUtils.hasText(qtyStr)) {
                        dec.setImportQty(new BigDecimal(qtyStr));
                    }
                }
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("進口數量格式錯誤: " + cellQty.toString());
            }
        }

        // Unit: 原料單位
        Cell cellUnit = getCellByField(row, fieldMap, "原料單位");
        if (cellUnit != null) {
            cellUnit.setCellType(org.apache.poi.ss.usermodel.CellType.STRING);
            dec.setMaterialUnit(cellUnit.getStringCellValue().trim());
        }

        dec.setTotalRefundQty(BigDecimal.ZERO);

        return dec;
    }

    /**
     * 根據標準欄位名稱從 fieldMap 取得對應的 Cell
     */
    private Cell getCellByField(Row row, java.util.Map<String, Integer> fieldMap, String fieldName) {
        Integer idx = fieldMap.get(fieldName);
        return idx != null ? row.getCell(idx) : null;
    }

    @Transactional
    public ImportDeclarationDto update(Long id, ImportDeclarationDto dto) {
        ImportDeclaration entity = repository.findById(id).orElseThrow(() -> new RuntimeException("Not Found"));
        entity.setMaterialName(dto.getMaterialName());
        entity.setMaterialSpec(dto.getMaterialSpec());
        entity.setImportQty(dto.getImportQty());
        entity.setMaterialUnit(dto.getMaterialUnit());
        return enrichDto(repository.save(entity));
    }

    @Transactional
    public void delete(Long id) {
        ImportDeclaration entity = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("進口報單不存在"));
        // 已核銷數量 > 0 則不可刪除
        if (entity.getTotalRefundQty() != null && entity.getTotalRefundQty().compareTo(BigDecimal.ZERO) > 0) {
            throw new RuntimeException("已核銷數量大於 0，不可刪除此報單");
        }
        repository.deleteById(id);
    }

    /**
     * 批次刪除進口報單（已核銷數量 = 0 才可刪除）
     * 
     * @param ids 要刪除的報單 ID 列表
     * @return 刪除結果，包含成功數與失敗原因
     */
    @Transactional
    public ImportResultDto batchDelete(List<Long> ids) {
        ImportResultDto result = new ImportResultDto();
        for (Long id : ids) {
            try {
                ImportDeclaration entity = repository.findById(id)
                        .orElseThrow(() -> new RuntimeException("進口報單不存在 (ID=" + id + ")"));
                // 已核銷數量 > 0 則不可刪除
                if (entity.getTotalRefundQty() != null && entity.getTotalRefundQty().compareTo(BigDecimal.ZERO) > 0) {
                    result.addError("報單 " + entity.getDocNo() + " 項次 " + entity.getItems()
                            + " 已核銷數量大於 0，不可刪除");
                } else {
                    repository.deleteById(id);
                    result.addSuccess();
                }
            } catch (Exception e) {
                result.addError("ID " + id + ": " + e.getMessage());
            }
        }
        return result;
    }

    /**
     * 取得所有不重複的進口報單號碼（供 auto-complete 下拉選單使用）
     */
    public List<String> getDistinctDocNos() {
        return repository.findDistinctDocNos();
    }

    public Workbook exportSearchResults(String docNo, String materialName, Integer status) {
        Specification<ImportDeclaration> spec = buildSearchSpec(docNo, materialName, status);
        List<ImportDeclaration> list = repository.findAll(spec);

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Import Declarations");

        // Header
        Row header = sheet.createRow(0);
        String[] headers = { "項次", "報單號碼", "報單項次", "原料名稱", "原料規格", "進口數量", "已核銷數量", "未核銷數量" };
        for (int i = 0; i < headers.length; i++)
            header.createCell(i).setCellValue(headers[i]);

        int rowNum = 1;
        for (ImportDeclaration entity : list) {
            ImportDeclarationDto dto = enrichDto(entity);
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(rowNum - 1);
            row.createCell(1).setCellValue(dto.getDocNo());
            row.createCell(2).setCellValue(dto.getItems());
            row.createCell(3).setCellValue(dto.getMaterialName());
            row.createCell(4).setCellValue(dto.getMaterialSpec());
            row.createCell(5).setCellValue(dto.getImportQty().doubleValue());
            row.createCell(6).setCellValue(dto.getTotalRefundQty().doubleValue());
            row.createCell(7).setCellValue(dto.getUnverifiedQty().doubleValue());
        }

        return workbook;
    }
}
