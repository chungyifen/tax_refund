package com.fox.tax.modules.refund.service;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.fox.tax.modules.refund.dto.ExportDeclarationDto;
import com.fox.tax.modules.refund.dto.ImportResultDto;
import com.fox.tax.modules.refund.entity.ExportDeclaration;
import com.fox.tax.modules.refund.mapper.ExportDeclarationMapper;
import com.fox.tax.modules.refund.repository.ExportDeclarationRepository;

import jakarta.persistence.criteria.Predicate;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@Transactional(readOnly = true, rollbackFor = RuntimeException.class)
public class ExportDeclarationService {

    @Autowired
    private ExportDeclarationRepository repository;

    @Autowired
    private ExportDeclarationMapper mapper;

    public Page<ExportDeclarationDto> search(String docNo, String prodType, String prodName, Integer status,
            Pageable pageable) {
        Specification<ExportDeclaration> spec = buildSearchSpec(docNo, prodType, prodName, status);
        return repository.findAll(spec, pageable).map(mapper::toDto);
    }

    private Specification<ExportDeclaration> buildSearchSpec(String docNo, String prodType, String prodName,
            Integer status) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (StringUtils.hasText(docNo)) {
                predicates.add(cb.like(root.get("docNo"), "%" + docNo + "%"));
            }

            if (StringUtils.hasText(prodType)) {
                predicates.add(cb.like(root.get("prodType"), "%" + prodType + "%"));
            }

            if (StringUtils.hasText(prodName)) {
                predicates.add(cb.like(root.get("prodName"), "%" + prodName + "%"));
            }

            // Status: 0: All, 1: Create, 2: Refund, 3: Refund Report
            if (status != null && status != 0) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * 從 colMap 中依據多個可能的別名查找欄位索引
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

                // 檢查此列是否包含關鍵欄位「報單號碼」
                boolean foundKey = false;
                for (Cell cell : row) {
                    if (cell != null && cell.toString().trim().contains("報單號碼")) {
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
                result.addError("找不到表頭列。請確認檔案包含「報單號碼」欄位。");
                return result;
            }

            log.info("偵測到表頭在第 {} 列，欄位: {}", headerRowNum, colMap.keySet());

            // 報單號碼
            Integer docNoIdx = findColumnIndex(colMap, "報單號碼", "出口報單號碼");
            // 出口報單項次 (items) - "出口報單?次" is for handling potential encoding issues where '項'
            // becomes '?'
            Integer itemsIdx = findColumnIndex(colMap, "出口報單項次", "報單項次", "項次", "ITEM", "出口報單?次");
            // 成品規格 (prodType)
            Integer prodTypeIdx = findColumnIndex(colMap, "成品規格", "規格", "SPEC");
            // 成品名稱 (prodName)
            Integer prodNameIdx = findColumnIndex(colMap, "成品名稱", "品名", "NAME");
            // 出口數量 (exportQty)
            Integer exportQtyIdx = findColumnIndex(colMap, "出口數量", "數量", "QTY");

            // 檢查必要欄位
            List<String> missingHeaders = new ArrayList<>();
            if (docNoIdx == null)
                missingHeaders.add("報單號碼");
            if (itemsIdx == null)
                missingHeaders.add("出口報單項次");
            if (prodTypeIdx == null)
                missingHeaders.add("成品規格");
            if (prodNameIdx == null)
                missingHeaders.add("成品名稱");
            if (exportQtyIdx == null)
                missingHeaders.add("出口數量");

            if (!missingHeaders.isEmpty()) {
                String foundHeaders = String.join(", ", colMap.keySet());
                result.addError("缺少必要表頭: " + String.join(", ", missingHeaders) + "。偵測到的表頭: [" + foundHeaders + "]");
                return result;
            }

            // 建立最終的欄位索引 Map
            java.util.Map<String, Integer> fieldMap = new java.util.HashMap<>();
            fieldMap.put("報單號碼", docNoIdx);
            fieldMap.put("項次", itemsIdx);
            fieldMap.put("成品規格", prodTypeIdx);
            fieldMap.put("成品名稱", prodNameIdx);
            fieldMap.put("出口數量", exportQtyIdx);

            for (int i = headerRowNum + 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null)
                    continue;

                try {
                    ExportDeclaration entity = parseRow(row, fieldMap);

                    // 若整行都是空的，則跳過
                    if (!StringUtils.hasText(entity.getDocNo()) && !StringUtils.hasText(entity.getItems())
                            && entity.getExportQty() == null) {
                        continue;
                    }

                    // Validate required fields
                    List<String> missingFields = new ArrayList<>();
                    if (!StringUtils.hasText(entity.getDocNo()))
                        missingFields.add("報單號碼");
                    if (!StringUtils.hasText(entity.getItems()))
                        missingFields.add("項次");
                    if (!StringUtils.hasText(entity.getProdName()))
                        missingFields.add("成品名稱");
                    if (entity.getExportQty() == null)
                        missingFields.add("出口數量");

                    if (!missingFields.isEmpty()) {
                        throw new IllegalArgumentException("缺少必填欄位: " + String.join(", ", missingFields));
                    }

                    if (StringUtils.hasText(entity.getDocNo()) && StringUtils.hasText(entity.getItems())) {
                        boolean exists = repository.existsByDocNoAndItems(entity.getDocNo(), entity.getItems());
                        if (!exists) {
                            entity.setStatus(ExportDeclaration._Status_Create);
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

    private ExportDeclaration parseRow(Row row, java.util.Map<String, Integer> fieldMap) {
        ExportDeclaration dec = new ExportDeclaration();

        // Doc No
        Cell cellDocNo = getCellByField(row, fieldMap, "報單號碼");
        if (cellDocNo != null) {
            cellDocNo.setCellType(org.apache.poi.ss.usermodel.CellType.STRING);
            dec.setDocNo(cellDocNo.getStringCellValue().trim());
        }

        // Items
        Cell cellItems = getCellByField(row, fieldMap, "項次");
        if (cellItems != null) {
            cellItems.setCellType(org.apache.poi.ss.usermodel.CellType.STRING);
            dec.setItems(cellItems.getStringCellValue().trim());
        }

        // Prod Type (Spec)
        Cell cellType = getCellByField(row, fieldMap, "成品規格");
        if (cellType != null) {
            cellType.setCellType(org.apache.poi.ss.usermodel.CellType.STRING);
            dec.setProdType(cellType.getStringCellValue().trim());
        } else {
            dec.setProdType("");
        }

        // Prod Name
        Cell cellName = getCellByField(row, fieldMap, "成品名稱");
        if (cellName != null) {
            cellName.setCellType(org.apache.poi.ss.usermodel.CellType.STRING);
            dec.setProdName(cellName.getStringCellValue().trim());
        }

        // Export Qty
        Cell cellQty = getCellByField(row, fieldMap, "出口數量");
        if (cellQty != null) {
            try {
                if (cellQty.getCellType() == org.apache.poi.ss.usermodel.CellType.NUMERIC) {
                    dec.setExportQty(BigDecimal.valueOf(cellQty.getNumericCellValue()));
                } else if (cellQty.getCellType() == org.apache.poi.ss.usermodel.CellType.STRING) {
                    String qtyStr = cellQty.getStringCellValue().trim();
                    if (StringUtils.hasText(qtyStr)) {
                        dec.setExportQty(new BigDecimal(qtyStr));
                    }
                }
            } catch (NumberFormatException e) {
                // ignore or set to null/zero
            }
        }

        return dec;
    }

    private Cell getCellByField(Row row, java.util.Map<String, Integer> fieldMap, String fieldName) {
        Integer idx = fieldMap.get(fieldName);
        return idx != null ? row.getCell(idx) : null;
    }

    @Transactional
    public ExportDeclarationDto update(Long id, ExportDeclarationDto dto) {
        ExportDeclaration entity = repository.findById(id).orElseThrow(() -> new RuntimeException("Not Found"));
        entity.setProdType(dto.getProdType());
        entity.setProdName(dto.getProdName());
        entity.setExportQty(dto.getExportQty());
        return mapper.toDto(repository.save(entity));
    }

    @Transactional
    public void delete(Long id) {
        ExportDeclaration entity = repository.findById(id).orElseThrow(() -> new RuntimeException("Not Found"));

        // 核退狀態=1:已匯入出口明細，才可以刪除
        if (entity.getStatus() != null && entity.getStatus() != ExportDeclaration._Status_Create) {
            throw new RuntimeException("該報單狀態非「已匯入」，不可刪除");
        }
        // Double check tax refund list
        if (entity.getTaxRefundList() != null && !entity.getTaxRefundList().isEmpty()) {
            throw new RuntimeException("已產生核銷清單，不可刪除");
        }

        repository.deleteById(id);
    }

    @Transactional
    public ImportResultDto batchDelete(List<Long> ids) {
        ImportResultDto result = new ImportResultDto();
        for (Long id : ids) {
            try {
                ExportDeclaration entity = repository.findById(id)
                        .orElseThrow(() -> new RuntimeException("出口報單不存在 (ID=" + id + ")"));

                // 核退狀態=1:已匯入出口明細，才可以刪除
                if (entity.getStatus() != null && entity.getStatus() != ExportDeclaration._Status_Create) {
                    result.addError(
                            "報單 " + entity.getDocNo() + " 項次 " + entity.getItems() + " 狀態非「已匯入」，不可刪除");
                } else if (entity.getTaxRefundList() != null && !entity.getTaxRefundList().isEmpty()) {
                    result.addError(
                            "報單 " + entity.getDocNo() + " 項次 " + entity.getItems() + " 已產生核銷清單，不可刪除");
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

    public List<String> getDistinctDocNos() {
        return repository.findDistinctDocNos();
    }

    public Workbook exportSearchResults(String docNo, String prodType, String prodName, Integer status) {
        Specification<ExportDeclaration> spec = buildSearchSpec(docNo, prodType, prodName, status);
        List<ExportDeclaration> list = repository.findAll(spec);

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Export Declarations");

        // Header
        Row header = sheet.createRow(0);
        String[] headers = { "項次", "報單號碼", "出口報單項次", "成品規格", "成品名稱", "出口數量", "狀態" };
        for (int i = 0; i < headers.length; i++) {
            header.createCell(i).setCellValue(headers[i]);
        }

        int rowNum = 1;
        for (ExportDeclaration entity : list) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(rowNum - 1);
            row.createCell(1).setCellValue(entity.getDocNo());
            row.createCell(2).setCellValue(entity.getItems());
            row.createCell(3).setCellValue(entity.getProdType());
            row.createCell(4).setCellValue(entity.getProdName());
            if (entity.getExportQty() != null) {
                row.createCell(5).setCellValue(entity.getExportQty().doubleValue());
            }
            if (entity.getStatus() != null) {
                String statusStr = "";
                switch (entity.getStatus()) {
                    case 1:
                        statusStr = "已匯入出口明細";
                        break;
                    case 2:
                        statusStr = "已產生核銷清單";
                        break;
                    case 3:
                        statusStr = "已產生核銷清單報表";
                        break;
                    default:
                        statusStr = String.valueOf(entity.getStatus());
                }
                row.createCell(6).setCellValue(statusStr);
            }
        }

        return workbook;
    }
}
