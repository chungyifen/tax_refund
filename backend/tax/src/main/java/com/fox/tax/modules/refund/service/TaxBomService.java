package com.fox.tax.modules.refund.service;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import com.querydsl.core.BooleanBuilder;
import com.fox.tax.modules.refund.entity.QTaxBom;
import org.springframework.util.StringUtils;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.ss.util.CellRangeAddress;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.fox.tax.modules.refund.dto.ImportResultDto;
import com.fox.tax.modules.refund.dto.TaxBomDto;
import com.fox.tax.modules.refund.entity.TaxBom;
import com.fox.tax.modules.refund.mapper.TaxBomMapper;
import com.fox.tax.modules.refund.repository.TaxBomRepository;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@Transactional(readOnly = true, rollbackFor = RuntimeException.class)
public class TaxBomService {

    private final TaxBomRepository repository;
    private final TaxBomMapper mapper;

    @Autowired
    public TaxBomService(TaxBomRepository repository, TaxBomMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    public List<TaxBomDto> findAll(String docNo, String prodName, String prodType) {
        log.info("findAll query: docNo={}, prodName={}, prodType={}", docNo, prodName, prodType);

        QTaxBom qTaxBom = QTaxBom.taxBom;
        BooleanBuilder builder = new BooleanBuilder();

        if (StringUtils.hasText(docNo)) {
            builder.and(qTaxBom.docNo.contains(docNo.trim()));
        }
        if (StringUtils.hasText(prodName)) {
            builder.and(qTaxBom.prodName.contains(prodName.trim()));
        }
        if (StringUtils.hasText(prodType)) {
            builder.and(qTaxBom.prodType.contains(prodType.trim()));
        }

        Iterable<TaxBom> iterable = repository.findAll(builder);
        List<TaxBom> list = StreamSupport.stream(iterable.spliterator(), false)
                .collect(Collectors.toList());
        return mapper.toDtoList(list);
    }

    @Transactional
    public TaxBomDto save(TaxBomDto dto) {
        TaxBom entity = mapper.toEntity(dto);
        return mapper.toDto(repository.save(entity));
    }

    @Transactional
    public TaxBomDto update(Long id, TaxBomDto dto) {
        TaxBom entity = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tax Bom not found"));
        // Only verify key fields are not null in DTO before copy if strict, but
        // BeanUtils copies all.
        // We should manually map or use mapper if partial updates are not desired.
        // Here we copy properties similar to previous service, but excluding audit and
        // id fields.
        BeanUtils.copyProperties(dto, entity, "id", "createTime", "creator", "taxRefundList");
        return mapper.toDto(repository.save(entity));
    }

    @Transactional
    public void delete(Long id) {
        repository.deleteById(id);
    }

    @Transactional
    public void deleteBatch(List<Long> ids) {
        repository.deleteAllById(ids);
    }

    @Transactional
    public ImportResultDto importExcel(MultipartFile file) throws IOException {
        ImportResultDto result = new ImportResultDto();
        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            for (Row row : sheet) {
                if (row.getRowNum() == 0)
                    continue; // Skip header

                try {
                    TaxBom dto = parseRow(sheet, row);
                    // Check logic based on composite key or business key: DocNo + MaterialNum
                    if (dto.getDocNo() != null && dto.getMaterialNum() != null) {
                        // Assuming findByDocNo doesn't filter by material num, we need custom check
                        // logic or repository method
                        // Previous used findByDocNoAndSerialNumber. TaxBomRepository has
                        // existsByDocNoAndMaterialNum but not findOne.
                        // We will implement logic to find potential existing record manually or assume
                        // inserts for now if list is large?
                        // Better to find one specific record. Using existing repository methods.
                        // IMPORTANT: TaxBomRepository currently lacks findByDocNoAndMaterialNum.
                        // I will create a temporary workaround using filtering or just insert if
                        // duplicates allowed,
                        // but best is to add method to repository. For now, assuming distinct check.

                        // NOTE: Adapting logic to previous service behavior of Update/Insert
                        // Let's assume we search by DocNo and filter in memory if needed or add Repo
                        // method later.
                        // For now, let's look for match.
                        List<TaxBom> existingList = repository.findByDocNo(dto.getDocNo());
                        TaxBom existing = existingList.stream()
                                .filter(e -> e.getMaterialNum().equals(dto.getMaterialNum()))
                                .findFirst().orElse(null);

                        if (existing != null) {
                            // Update
                            BeanUtils.copyProperties(dto, existing, "id", "createTime", "creator", "taxRefundList");
                            repository.save(existing);
                        } else {
                            // Insert
                            repository.save(dto);
                        }
                        result.addSuccess();
                    } else {
                        result.addError(
                                "Row " + (row.getRowNum() + 1) + ": Missing required fields (DocNo or MaterialNum)");
                    }
                } catch (Exception e) {
                    result.addError("Row " + (row.getRowNum() + 1) + ": " + e.getMessage());
                    log.error("Error importing row " + row.getRowNum(), e);
                }
            }
        }
        return result;
    }

    /**
     * 解析 Excel 行資料為 TaxBom 物件
     * 
     * @param sheet Sheet 物件，用於查詢合併儲存格
     * @param row   當前行
     * @return 解析後的 TaxBom 物件
     */
    private TaxBom parseRow(Sheet sheet, Row row) {
        TaxBom bom = new TaxBom();
        int rowIdx = row.getRowNum();

        // 0: 核准文號 (DocNo) - 可能為合併儲存格
        bom.setDocNo(getMergedCellValue(sheet, rowIdx, 0));

        // 1: 成品規格 (ProdType) - 可能為合併儲存格
        bom.setProdType(getMergedCellValue(sheet, rowIdx, 1));

        // 2: 成品名稱 (ProdName) - 可能為合併儲存格
        bom.setProdName(getMergedCellValue(sheet, rowIdx, 2));

        // 3: 成品單位 (ProdUnit) - 可能為合併儲存格
        bom.setProdUnit(getMergedCellValue(sheet, rowIdx, 3));

        // 4: 原料序號 (MaterialNum) - Col 4
        String matNumStr = getCellValue(row.getCell(4));
        if (matNumStr != null && !matNumStr.isEmpty()) {
            try {
                // 移除數值格式化時的小數點 (例如 "1.0" -> "1")
                if (matNumStr.contains(".")) {
                    matNumStr = matNumStr.substring(0, matNumStr.indexOf("."));
                }
                bom.setMaterialNum(Integer.parseInt(matNumStr));
            } catch (NumberFormatException e) {
                bom.setMaterialNum(0);
            }
        } else {
            bom.setMaterialNum(0);
        }

        // 5: 原料名稱 (MaterialName) - Col 5
        bom.setMaterialName(getCellValue(row.getCell(5)));

        // 6: 原料SPEC (MaterialSpec) - Col 6
        bom.setMaterialSpec(getCellValue(row.getCell(6)));

        // 7: 使用數量 (UsageQty) - Col 7
        String qtyStr = getCellValue(row.getCell(7));
        if (qtyStr != null && !qtyStr.isEmpty()) {
            try {
                bom.setUsageQty(new BigDecimal(qtyStr));
            } catch (Exception e) {
                bom.setUsageQty(BigDecimal.ZERO);
            }
        } else {
            bom.setUsageQty(BigDecimal.ZERO);
        }

        // 8: 使用單位 (MaterialUnit) - Col 8
        bom.setMaterialUnit(getCellValue(row.getCell(8)));

        // 產品單位預設為 "SET"
        if (bom.getProdUnit() == null || bom.getProdUnit().isEmpty()) {
            bom.setProdUnit("SET");
        }

        return bom;
    }

    /**
     * 取得可能為合併儲存格的值。
     * 當指定位置的 cell 值為空時，查詢該 cell 是否位於合併區域內，
     * 若是則取合併區域起始 cell 的值。
     *
     * @param sheet  Sheet 物件
     * @param rowIdx 行索引
     * @param colIdx 欄索引
     * @return cell 值（字串）
     */
    private String getMergedCellValue(Sheet sheet, int rowIdx, int colIdx) {
        // 先嘗試直接讀取當前 cell 的值
        Row row = sheet.getRow(rowIdx);
        if (row != null) {
            String value = getCellValue(row.getCell(colIdx));
            if (value != null && !value.isEmpty()) {
                return value;
            }
        }

        // 若值為空，檢查是否屬於合併儲存格區域
        for (int i = 0; i < sheet.getNumMergedRegions(); i++) {
            CellRangeAddress mergedRegion = sheet.getMergedRegion(i);
            if (mergedRegion.isInRange(rowIdx, colIdx)) {
                // 取合併區域左上角（第一個 cell）的值
                Row firstRow = sheet.getRow(mergedRegion.getFirstRow());
                if (firstRow != null) {
                    return getCellValue(firstRow.getCell(mergedRegion.getFirstColumn()));
                }
            }
        }

        return "";
    }

    private String getCellValue(Cell cell) {
        if (cell == null) {
            return "";
        }
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue().trim();
            case NUMERIC:
                if (org.apache.poi.ss.usermodel.DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                } else {
                    // Avoid scientific notation for simple integers
                    double val = cell.getNumericCellValue();
                    if (val == (long) val) {
                        return String.format("%d", (long) val);
                    } else {
                        return String.valueOf(val);
                    }
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                try {
                    return cell.getStringCellValue();
                } catch (Exception e) {
                    return String.valueOf(cell.getNumericCellValue());
                }
            default:
                return "";
        }
    }
}
