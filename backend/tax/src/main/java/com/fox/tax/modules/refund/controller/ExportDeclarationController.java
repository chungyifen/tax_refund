package com.fox.tax.modules.refund.controller;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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

import com.fox.tax.modules.refund.dto.ExportDeclarationDto;
import com.fox.tax.modules.refund.dto.ImportResultDto;
import com.fox.tax.modules.refund.service.ExportDeclarationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "ExportDeclaration", description = "出口報單管理 API")
@RestController
@RequestMapping("/api/refund/export-declaration")
public class ExportDeclarationController {

    @Autowired
    private ExportDeclarationService service;

    @Operation(summary = "搜尋出口報單")
    @GetMapping
    @PreAuthorize("hasAuthority('EXPORT_DECLARATION_VIEW')")
    public ResponseEntity<Page<ExportDeclarationDto>> search(
            @RequestParam(required = false) String docNo,
            @RequestParam(required = false) String prodType,
            @RequestParam(required = false) String prodName,
            @RequestParam(required = false) Integer status,
            @PageableDefault(size = 20, sort = "docNo", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(service.search(docNo, prodType, prodName, status, pageable));
    }

    @Operation(summary = "取得不重複的出口報單號碼")
    @GetMapping("/doc-nos")
    @PreAuthorize("hasAuthority('EXPORT_DECLARATION_VIEW')")
    public ResponseEntity<java.util.List<String>> getDistinctDocNos() {
        return ResponseEntity.ok(service.getDistinctDocNos());
    }

    @Operation(summary = "更新出口報單")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('EXPORT_DECLARATION_EDIT')")
    public ResponseEntity<ExportDeclarationDto> update(@PathVariable Long id, @RequestBody ExportDeclarationDto dto) {
        return ResponseEntity.ok(service.update(id, dto));
    }

    @Operation(summary = "批次刪除出口報單")
    @PostMapping("/batch-delete")
    @PreAuthorize("hasAuthority('EXPORT_DECLARATION_EDIT')")
    public ResponseEntity<ImportResultDto> batchDelete(@RequestBody java.util.List<Long> ids) {
        return ResponseEntity.ok(service.batchDelete(ids));
    }

    @Operation(summary = "刪除出口報單")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('EXPORT_DECLARATION_EDIT')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "匯出搜尋結果 Excel")
    @GetMapping("/export")
    @PreAuthorize("hasAuthority('EXPORT_DECLARATION_VIEW')")
    public void export(@RequestParam(required = false) String docNo,
            @RequestParam(required = false) String prodType,
            @RequestParam(required = false) String prodName,
            @RequestParam(required = false) Integer status,
            jakarta.servlet.http.HttpServletResponse response) throws IOException {
        org.apache.poi.ss.usermodel.Workbook workbook = service.exportSearchResults(docNo, prodType, prodName, status);
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=export_declarations.xlsx");
        workbook.write(response.getOutputStream());
        workbook.close();
    }

    @Operation(summary = "匯入出口報單 Excel")
    @PostMapping("/import")
    @PreAuthorize("hasAuthority('EXPORT_DECLARATION_EDIT')")
    public ResponseEntity<ImportResultDto> importExcel(@RequestParam("file") MultipartFile file) throws IOException {
        return ResponseEntity.ok(service.importExcel(file));
    }
}
