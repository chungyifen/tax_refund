package com.fox.tax.modules.refund.service;

import com.fox.tax.modules.refund.dto.GenerateRefundResultDto;
import com.fox.tax.modules.refund.entity.ExportDeclaration;
import com.fox.tax.modules.refund.entity.ImportDeclaration;
import com.fox.tax.modules.refund.entity.TaxBom;
import com.fox.tax.modules.refund.entity.TaxRefund;
import com.fox.tax.modules.refund.repository.ExportDeclarationRepository;
import com.fox.tax.modules.refund.repository.ImportDeclarationRepository;
import com.fox.tax.modules.refund.repository.TaxBomRepository;
import com.fox.tax.modules.refund.repository.TaxRefundRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaxRefundServiceTest {

    @Mock
    private TaxRefundRepository taxRefundRepository;

    @Mock
    private ExportDeclarationRepository exportDeclarationRepository;

    @Mock
    private ImportDeclarationRepository importDeclarationRepository;

    @Mock
    private TaxBomRepository taxBomRepository;

    @InjectMocks
    private TaxRefundService taxRefundService;

    @Captor
    private ArgumentCaptor<TaxRefund> taxRefundCaptor;

    @Captor
    private ArgumentCaptor<ImportDeclaration> importDeclarationCaptor;

    @Captor
    private ArgumentCaptor<ExportDeclaration> exportDeclarationCaptor;

    // ==================== generateRefundList tests ====================

    @Test
    void generateRefundList_normalScenario() {
        // Arrange
        String docNo = "EXP001";

        ExportDeclaration export = ExportDeclaration.builder()
                .docNo(docNo)
                .prodType("TypeA")
                .prodName("ProdA")
                .exportQty(new BigDecimal("10"))
                .status(1)
                .build();
        export.setId(1L);

        TaxBom bom = TaxBom.builder()
                .docNo("DOC1")
                .prodType("TypeA")
                .prodName("ProdA")
                .materialName("MatA")
                .materialSpec("SPEC1")
                .materialNum(1)
                .usageQty(new BigDecimal("2"))
                .build();
        bom.setId(1L);

        ImportDeclaration importDec = ImportDeclaration.builder()
                .docNo("IMP001")
                .materialName("MatA")
                .materialSpec("SPEC1")
                .importQty(new BigDecimal("100"))
                .totalRefundQty(new BigDecimal("0"))
                .build();
        importDec.setId(1L);

        when(exportDeclarationRepository.findByDocNo(docNo)).thenReturn(List.of(export));
        when(taxBomRepository.findByProdTypeAndProdName("TypeA", "ProdA")).thenReturn(List.of(bom));
        when(importDeclarationRepository.findByMaterialNameAndMaterialSpecOrderByIdAsc("MatA", "SPEC1"))
                .thenReturn(List.of(importDec));
        when(taxRefundRepository.save(any(TaxRefund.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(importDeclarationRepository.save(any(ImportDeclaration.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(exportDeclarationRepository.save(any(ExportDeclaration.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        GenerateRefundResultDto result = taxRefundService.generateRefundList(docNo);

        // Assert
        assertEquals(1, result.getSuccessCount());
        assertNotNull(result.getReportNo());

        verify(taxRefundRepository, times(1)).save(taxRefundCaptor.capture());
        TaxRefund savedRefund = taxRefundCaptor.getValue();
        assertEquals(0, new BigDecimal("20").compareTo(savedRefund.getUsageQty()));

        verify(importDeclarationRepository).save(importDeclarationCaptor.capture());
        ImportDeclaration savedImport = importDeclarationCaptor.getValue();
        assertEquals(0, new BigDecimal("20").compareTo(savedImport.getTotalRefundQty()));

        verify(exportDeclarationRepository).save(exportDeclarationCaptor.capture());
        ExportDeclaration savedExport = exportDeclarationCaptor.getValue();
        assertEquals(ExportDeclaration._Status_Create_Refund, savedExport.getStatus());
    }

    @Test
    void generateRefundList_multipleImports_FIFO() {
        // Arrange
        String docNo = "EXP002";

        ExportDeclaration export = ExportDeclaration.builder()
                .docNo(docNo)
                .prodType("TypeA")
                .prodName("ProdA")
                .exportQty(new BigDecimal("10"))
                .status(1)
                .build();
        export.setId(1L);

        TaxBom bom = TaxBom.builder()
                .docNo("DOC1")
                .prodType("TypeA")
                .prodName("ProdA")
                .materialName("MatA")
                .materialSpec("SPEC1")
                .materialNum(1)
                .usageQty(new BigDecimal("3"))
                .build();
        bom.setId(1L);

        // import1: available = 20 - 5 = 15
        ImportDeclaration import1 = ImportDeclaration.builder()
                .docNo("IMP001")
                .materialName("MatA")
                .materialSpec("SPEC1")
                .importQty(new BigDecimal("20"))
                .totalRefundQty(new BigDecimal("5"))
                .build();
        import1.setId(1L);

        // import2: available = 50 - 0 = 50
        ImportDeclaration import2 = ImportDeclaration.builder()
                .docNo("IMP002")
                .materialName("MatA")
                .materialSpec("SPEC1")
                .importQty(new BigDecimal("50"))
                .totalRefundQty(new BigDecimal("0"))
                .build();
        import2.setId(2L);

        when(exportDeclarationRepository.findByDocNo(docNo)).thenReturn(List.of(export));
        when(taxBomRepository.findByProdTypeAndProdName("TypeA", "ProdA")).thenReturn(List.of(bom));
        when(importDeclarationRepository.findByMaterialNameAndMaterialSpecOrderByIdAsc("MatA", "SPEC1"))
                .thenReturn(List.of(import1, import2));
        when(taxRefundRepository.save(any(TaxRefund.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(importDeclarationRepository.save(any(ImportDeclaration.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(exportDeclarationRepository.save(any(ExportDeclaration.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        // Required qty = 10 * 3 = 30. FIFO: import1 provides 15, import2 provides 15.
        GenerateRefundResultDto result = taxRefundService.generateRefundList(docNo);

        // Assert
        assertEquals(2, result.getSuccessCount());

        verify(taxRefundRepository, times(2)).save(taxRefundCaptor.capture());
        List<TaxRefund> savedRefunds = taxRefundCaptor.getAllValues();

        // First refund: uses all available from import1 (15)
        assertEquals(0, new BigDecimal("15").compareTo(savedRefunds.get(0).getUsageQty()));
        // Second refund: remaining 15 from import2
        assertEquals(0, new BigDecimal("15").compareTo(savedRefunds.get(1).getUsageQty()));

        // import1 totalRefundQty: 5 + 15 = 20
        assertEquals(0, new BigDecimal("20").compareTo(import1.getTotalRefundQty()));
        // import2 totalRefundQty: 0 + 15 = 15
        assertEquals(0, new BigDecimal("15").compareTo(import2.getTotalRefundQty()));
    }

    @Test
    void generateRefundList_skipsAlreadyProcessed() {
        // Arrange
        String docNo = "EXP003";

        ExportDeclaration export = ExportDeclaration.builder()
                .docNo(docNo)
                .prodType("TypeA")
                .prodName("ProdA")
                .exportQty(new BigDecimal("10"))
                .status(ExportDeclaration._Status_Create_Refund) // status = 2, already processed
                .build();
        export.setId(1L);

        when(exportDeclarationRepository.findByDocNo(docNo)).thenReturn(List.of(export));

        // Act
        GenerateRefundResultDto result = taxRefundService.generateRefundList(docNo);

        // Assert
        assertEquals(0, result.getSuccessCount());
        assertFalse(result.getWarnings().isEmpty());
        verify(taxRefundRepository, never()).save(any(TaxRefund.class));
    }

    @Test
    void generateRefundList_noBomFound() {
        // Arrange
        String docNo = "EXP004";

        ExportDeclaration export = ExportDeclaration.builder()
                .docNo(docNo)
                .prodType("TypeA")
                .prodName("ProdA")
                .exportQty(new BigDecimal("10"))
                .status(1)
                .build();
        export.setId(1L);

        when(exportDeclarationRepository.findByDocNo(docNo)).thenReturn(List.of(export));
        when(taxBomRepository.findByProdTypeAndProdName("TypeA", "ProdA")).thenReturn(Collections.emptyList());

        // Act
        GenerateRefundResultDto result = taxRefundService.generateRefundList(docNo);

        // Assert
        assertFalse(result.getWarnings().isEmpty());
        verify(taxRefundRepository, never()).save(any(TaxRefund.class));
    }

    @Test
    void generateRefundList_noExportFound() {
        // Arrange
        String docNo = "EXP005";

        when(exportDeclarationRepository.findByDocNo(docNo)).thenReturn(Collections.emptyList());

        // Act & Assert
        assertThrows(RuntimeException.class, () -> taxRefundService.generateRefundList(docNo));
    }

    @Test
    void generateRefundList_insufficientStock() {
        // Arrange
        String docNo = "EXP006";

        ExportDeclaration export = ExportDeclaration.builder()
                .docNo(docNo)
                .prodType("TypeA")
                .prodName("ProdA")
                .exportQty(new BigDecimal("100"))
                .status(1)
                .build();
        export.setId(1L);

        TaxBom bom = TaxBom.builder()
                .docNo("DOC1")
                .prodType("TypeA")
                .prodName("ProdA")
                .materialName("MatA")
                .materialSpec("SPEC1")
                .materialNum(1)
                .usageQty(new BigDecimal("5"))
                .build();
        bom.setId(1L);

        // Required = 100 * 5 = 500, but only 30 available
        ImportDeclaration importDec = ImportDeclaration.builder()
                .docNo("IMP001")
                .materialName("MatA")
                .materialSpec("SPEC1")
                .importQty(new BigDecimal("50"))
                .totalRefundQty(new BigDecimal("20"))
                .build();
        importDec.setId(1L);

        when(exportDeclarationRepository.findByDocNo(docNo)).thenReturn(List.of(export));
        when(taxBomRepository.findByProdTypeAndProdName("TypeA", "ProdA")).thenReturn(List.of(bom));
        when(importDeclarationRepository.findByMaterialNameAndMaterialSpecOrderByIdAsc("MatA", "SPEC1"))
                .thenReturn(List.of(importDec));
        when(taxRefundRepository.save(any(TaxRefund.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(importDeclarationRepository.save(any(ImportDeclaration.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(exportDeclarationRepository.save(any(ExportDeclaration.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        GenerateRefundResultDto result = taxRefundService.generateRefundList(docNo);

        // Assert
        // A partial refund should be created with available qty (30)
        assertFalse(result.getWarnings().isEmpty());
        verify(taxRefundRepository, times(1)).save(taxRefundCaptor.capture());
        TaxRefund savedRefund = taxRefundCaptor.getValue();
        assertEquals(0, new BigDecimal("30").compareTo(savedRefund.getUsageQty()));

        // import totalRefundQty should be updated: 20 + 30 = 50
        assertEquals(0, new BigDecimal("50").compareTo(importDec.getTotalRefundQty()));
    }

    // ==================== updateRefundQty tests ====================

    @Test
    void updateRefundQty_success() {
        // Arrange
        Long refundId = 1L;
        BigDecimal newQty = new BigDecimal("15");

        ImportDeclaration importDec = ImportDeclaration.builder()
                .docNo("IMP001")
                .materialName("MatA")
                .materialSpec("SPEC1")
                .importQty(new BigDecimal("100"))
                .totalRefundQty(new BigDecimal("50"))
                .build();
        importDec.setId(1L);

        TaxRefund refund = TaxRefund.builder()
                .reportNo("RPT001")
                .usageQty(new BigDecimal("10"))
                .importDeclaration(importDec)
                .build();
        refund.setId(refundId);

        when(taxRefundRepository.findById(refundId)).thenReturn(Optional.of(refund));
        when(taxRefundRepository.save(any(TaxRefund.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(importDeclarationRepository.save(any(ImportDeclaration.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        taxRefundService.updateRefundQty(refundId, newQty);

        // Assert
        // diff = 15 - 10 = 5, newTotalRefund = 50 + 5 = 55
        verify(taxRefundRepository).save(taxRefundCaptor.capture());
        assertEquals(0, new BigDecimal("15").compareTo(taxRefundCaptor.getValue().getUsageQty()));

        verify(importDeclarationRepository).save(importDeclarationCaptor.capture());
        assertEquals(0, new BigDecimal("55").compareTo(importDeclarationCaptor.getValue().getTotalRefundQty()));
    }

    @Test
    void updateRefundQty_exceedsImportQty() {
        // Arrange
        Long refundId = 1L;
        BigDecimal newQty = new BigDecimal("60");

        ImportDeclaration importDec = ImportDeclaration.builder()
                .docNo("IMP001")
                .materialName("MatA")
                .materialSpec("SPEC1")
                .importQty(new BigDecimal("100"))
                .totalRefundQty(new BigDecimal("50"))
                .build();
        importDec.setId(1L);

        TaxRefund refund = TaxRefund.builder()
                .reportNo("RPT001")
                .usageQty(new BigDecimal("10"))
                .importDeclaration(importDec)
                .build();
        refund.setId(refundId);

        when(taxRefundRepository.findById(refundId)).thenReturn(Optional.of(refund));

        // Act & Assert
        // diff = 60 - 10 = 50, newTotalRefund = 50 + 50 = 100... but let's exceed
        // Actually 100 == 100 so let's use newQty = 61
        // diff = 61 - 10 = 51, newTotalRefund = 50 + 51 = 101 > 100
        assertThrows(RuntimeException.class,
                () -> taxRefundService.updateRefundQty(refundId, new BigDecimal("61")));
    }

    @Test
    void updateRefundQty_negativeTotal() {
        // Arrange
        Long refundId = 1L;

        ImportDeclaration importDec = ImportDeclaration.builder()
                .docNo("IMP001")
                .materialName("MatA")
                .materialSpec("SPEC1")
                .importQty(new BigDecimal("100"))
                .totalRefundQty(new BigDecimal("10"))
                .build();
        importDec.setId(1L);

        TaxRefund refund = TaxRefund.builder()
                .reportNo("RPT001")
                .usageQty(new BigDecimal("20"))
                .importDeclaration(importDec)
                .build();
        refund.setId(refundId);

        when(taxRefundRepository.findById(refundId)).thenReturn(Optional.of(refund));

        // Act & Assert
        // diff = -15 - 20 = would need negative check
        // totalRefundQty = 10 + (-15 - 20) ... let's think:
        // newQty that makes it negative: if usageQty=20, totalRefundQty=10
        // diff = newQty - 20. newTotalRefund = 10 + diff = 10 + newQty - 20 = newQty - 10
        // For newTotalRefund < 0: newQty < 10... but newQty itself should be valid
        // Actually: diff = newQty - oldQty. If newQty is very small and totalRefundQty is small
        // totalRefundQty=10, usageQty=20, newQty=-5 would give diff=-25, newTotal=10-25=-15 < 0
        // But negative qty doesn't make sense. Let's use a scenario where totalRefundQty < usageQty
        // and reducing would go negative.
        // With totalRefundQty=10, usageQty=20: diff = newQty - 20
        // newTotal = 10 + (newQty - 20) = newQty - 10
        // For negative: newQty < 10, e.g. newQty = 5 => newTotal = -5
        assertThrows(RuntimeException.class,
                () -> taxRefundService.updateRefundQty(refundId, new BigDecimal("5")));
    }

    @Test
    void updateRefundQty_notFound() {
        // Arrange
        Long refundId = 999L;
        when(taxRefundRepository.findById(refundId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(RuntimeException.class,
                () -> taxRefundService.updateRefundQty(refundId, new BigDecimal("10")));
    }

    // ==================== Delegation tests ====================

    @Test
    void findByReportNo_delegatesToRepository() {
        // Arrange
        String reportNo = "RPT001";
        List<TaxRefund> expected = List.of(TaxRefund.builder().reportNo(reportNo).build());
        when(taxRefundRepository.findByReportNo(reportNo)).thenReturn(expected);

        // Act
        List<TaxRefund> result = taxRefundService.findByReportNo(reportNo);

        // Assert
        assertEquals(expected, result);
        verify(taxRefundRepository).findByReportNo(reportNo);
    }

    @Test
    void findByExportDeclarationId_delegatesToRepository() {
        // Arrange
        Long exportId = 1L;
        List<TaxRefund> expected = List.of(TaxRefund.builder().reportNo("RPT001").build());
        when(taxRefundRepository.findByExportDeclarationId(exportId)).thenReturn(expected);

        // Act
        List<TaxRefund> result = taxRefundService.findByExportDeclarationId(exportId);

        // Assert
        assertEquals(expected, result);
        verify(taxRefundRepository).findByExportDeclarationId(exportId);
    }
}
