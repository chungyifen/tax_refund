package com.fox.tax.modules.refund.controller;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
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

import com.fox.tax.modules.refund.dto.ImportDeclarationDto;
import com.fox.tax.modules.refund.dto.ImportResultDto;
import com.fox.tax.modules.refund.service.ImportDeclarationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;

@Tag(name = "ImportDeclaration", description = "進口報單管理 API")
@RestController
@RequestMapping("/api/refund/import-declaration")
public class ImportDeclarationController {

    @Autowired
    private ImportDeclarationService service;

    @Operation(summary = "搜尋進口報單")
    @GetMapping
    @PreAuthorize("hasAuthority('IMPORT_DECLARATION_VIEW')")
    public ResponseEntity<Page<ImportDeclarationDto>> search(
            @RequestParam(required = false) String docNo,
            @RequestParam(required = false) String materialName,
            @RequestParam(required = false) Integer status,
            @PageableDefault(size = 20, sort = "docNo", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(service.search(docNo, materialName, status, pageable));
    }

    @Operation(summary = "取得所有不重複的進口報單號碼（auto-complete 用）")
    @GetMapping("/doc-nos")
    @PreAuthorize("hasAuthority('IMPORT_DECLARATION_VIEW')")
    public ResponseEntity<java.util.List<String>> getDocNos() {
        return ResponseEntity.ok(service.getDistinctDocNos());
    }

    @Operation(summary = "更新進口報單")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('IMPORT_DECLARATION_EDIT')")
    public ResponseEntity<ImportDeclarationDto> update(@PathVariable Long id, @RequestBody ImportDeclarationDto dto) {
        return ResponseEntity.ok(service.update(id, dto));
    }

    @Operation(summary = "批次刪除進口報單")
    @PostMapping("/batch-delete")
    @PreAuthorize("hasAuthority('IMPORT_DECLARATION_EDIT')")
    public ResponseEntity<ImportResultDto> batchDelete(@RequestBody java.util.List<Long> ids) {
        return ResponseEntity.ok(service.batchDelete(ids));
    }

    @Operation(summary = "刪除進口報單")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('IMPORT_DECLARATION_EDIT')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "匯入進口報單 Excel")
    @PostMapping("/import")
    @PreAuthorize("hasAuthority('IMPORT_DECLARATION_EDIT')")
    public ResponseEntity<ImportResultDto> importExcel(@RequestParam("file") MultipartFile file)
            throws IOException {
        return ResponseEntity.ok(service.importExcel(file));
    }

    @Operation(summary = "匯出搜尋結果 Excel")
    @GetMapping("/export")
    @PreAuthorize("hasAuthority('IMPORT_DECLARATION_VIEW')")
    public void exportSearchResults(
            @RequestParam(required = false) String docNo,
            @RequestParam(required = false) String materialName,
            @RequestParam(required = false) Integer status,
            HttpServletResponse response) throws IOException {

        Workbook workbook = service.exportSearchResults(docNo, materialName, status);

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        String filename = URLEncoder.encode("進口報單搜尋結果.xlsx", StandardCharsets.UTF_8.toString());
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"");

        workbook.write(response.getOutputStream());
        workbook.close();
    }
}
