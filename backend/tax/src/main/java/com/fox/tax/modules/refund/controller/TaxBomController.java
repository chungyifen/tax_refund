package com.fox.tax.modules.refund.controller;

import java.io.IOException;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.fox.tax.modules.refund.dto.ImportResultDto;
import com.fox.tax.modules.refund.dto.TaxBomDto;
import com.fox.tax.modules.refund.service.TaxBomService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "TaxBom", description = "退稅BOM管理 API")
@RestController
@RequestMapping("/api/refund/bom")
public class TaxBomController {

    @Autowired
    private TaxBomService service;

    @Operation(summary = "查詢退稅BOM（分頁）")
    @GetMapping
    public ResponseEntity<Page<TaxBomDto>> findAll(
            @RequestParam(name = "docNo", required = false) String docNo,
            @RequestParam(name = "prodName", required = false) String prodName,
            @RequestParam(name = "prodType", required = false) String prodType,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(service.findAll(docNo, prodName, prodType, pageable));
    }

    @Operation(summary = "新增退稅BOM")
    @PostMapping
    public ResponseEntity<TaxBomDto> create(@RequestBody TaxBomDto dto) {
        return ResponseEntity.ok(service.save(dto));
    }

    @Operation(summary = "更新退稅BOM")
    @PutMapping("/{id}")
    public ResponseEntity<TaxBomDto> update(@PathVariable Long id, @RequestBody TaxBomDto dto) {
        return ResponseEntity.ok(service.update(id, dto));
    }

    @Operation(summary = "刪除退稅BOM")
    @DeleteMapping("/{id:\\d+}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "批次刪除退稅BOM")
    @DeleteMapping("/batch")
    public ResponseEntity<Void> deleteBatch(@RequestBody List<Long> ids) {
        service.deleteBatch(ids);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "匯入退稅BOM Excel")
    @PostMapping("/import")
    public ResponseEntity<ImportResultDto> importExcel(@RequestParam("file") MultipartFile file)
            throws IOException {
        return ResponseEntity.ok(service.importExcel(file));
    }
}
