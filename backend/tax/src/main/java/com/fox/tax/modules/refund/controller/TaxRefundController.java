package com.fox.tax.modules.refund.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.fox.tax.modules.refund.entity.ExportDeclaration;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.apache.poi.ss.usermodel.Workbook;

import java.io.IOException;
import java.net.URLEncoder;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.math.BigDecimal;
import java.util.Map;

import jakarta.servlet.http.HttpServletResponse;

import com.fox.tax.modules.refund.dto.GenerateRefundResultDto;
import com.fox.tax.modules.refund.entity.TaxRefund;
import com.fox.tax.modules.refund.service.TaxRefundService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 退稅核銷清單 Controller
 */
@Tag(name = "TaxRefund", description = "退稅核銷清單管理 API")
@RestController
@RequestMapping("/api/refund/tax-refund")
public class TaxRefundController {

    @Autowired
    private TaxRefundService taxRefundService;

    @Operation(summary = "產生退稅清單")
    @PostMapping("/generate")
    @PreAuthorize("hasAuthority('EXPORT_DECLARATION_EDIT')")
    public ResponseEntity<GenerateRefundResultDto> generateRefundList(
            @RequestParam String docNo) {
        GenerateRefundResultDto result = taxRefundService.generateRefundList(docNo);
        return ResponseEntity.ok(result);
    }

    @Operation(summary = "依據報表編號查詢退稅清單")
    @GetMapping("/{reportNo}")
    @PreAuthorize("hasAuthority('TAX_REFUND_VIEW')")
    public ResponseEntity<List<TaxRefund>> findByReportNo(
            @PathVariable String reportNo) {
        return ResponseEntity.ok(taxRefundService.findByReportNo(reportNo));
    }

    @Operation(summary = "依據出口報單 ID 查詢退稅清單")
    @GetMapping("/export-declaration/{exportDeclarationId}")
    @PreAuthorize("hasAuthority('TAX_REFUND_VIEW')")
    public ResponseEntity<List<TaxRefund>> findByExportDeclarationId(
            @PathVariable Long exportDeclarationId) {
        return ResponseEntity.ok(taxRefundService.findByExportDeclarationId(exportDeclarationId));
    }

    @Operation(summary = "搜尋出口報單列表 (退稅清單功能)")
    @GetMapping("/list")
    @PreAuthorize("hasAuthority('TAX_REFUND_VIEW')")
    public ResponseEntity<Page<ExportDeclaration>> searchExportList(
            @RequestParam(required = false) String docNo,
            @RequestParam(required = false) Integer status,
            @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(taxRefundService.searchExports(docNo, status, pageable));
    }

    @Operation(summary = "更新退稅核銷數量")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('EXPORT_DECLARATION_EDIT')")
    public ResponseEntity<Void> updateRefundQty(
            @PathVariable Long id,
            @RequestBody Map<String, BigDecimal> payload) {
        BigDecimal usageQty = payload.get("usageQty");
        if (usageQty == null) {
            throw new IllegalArgumentException("usageQty is required");
        }
        taxRefundService.updateRefundQty(id, usageQty);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "導出用料清表 (Report L)")
    @GetMapping("/export/L")
    @PreAuthorize("hasAuthority('TAX_REFUND_VIEW')")
    public void exportL(@RequestParam String docNo, HttpServletResponse response) throws IOException {
        Workbook workbook = taxRefundService.exportReportL(docNo);
        String filename = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + "-" + docNo
                + "_L.xls";
        response.setContentType("application/vnd.ms-excel");
        response.setHeader("Content-Disposition", "attachment; filename=" + URLEncoder.encode(filename, "UTF-8"));
        workbook.write(response.getOutputStream());
        workbook.close();
    }

    @Operation(summary = "導出沖退稅申請 (Report A)")
    @GetMapping("/export/A")
    @PreAuthorize("hasAuthority('TAX_REFUND_VIEW')")
    public void exportA(@RequestParam String docNo, HttpServletResponse response) throws IOException {
        Workbook workbook = taxRefundService.exportReportA(docNo);
        String filename = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + "-" + docNo
                + "_A.xls";
        response.setContentType("application/vnd.ms-excel");
        response.setHeader("Content-Disposition", "attachment; filename=" + URLEncoder.encode(filename, "UTF-8"));
        workbook.write(response.getOutputStream());
        workbook.close();
    }
}
