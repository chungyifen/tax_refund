package com.fox.tax.modules.refund.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fox.tax.modules.refund.dto.ExportDeclarationDto;
import com.fox.tax.modules.refund.dto.ImportResultDto;
import com.fox.tax.modules.refund.entity.ExportDeclaration;
import com.fox.tax.modules.refund.entity.TaxRefund;
import com.fox.tax.modules.refund.mapper.ExportDeclarationMapper;
import com.fox.tax.modules.refund.repository.ExportDeclarationRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class ExportDeclarationServiceTest {

    @Mock
    private ExportDeclarationRepository repository;

    @Mock
    private ExportDeclarationMapper mapper;

    @InjectMocks
    private ExportDeclarationService exportDeclarationService;

    private ExportDeclaration createEntity(Long id, String docNo, String prodType,
            String prodName, BigDecimal exportQty, Integer status) {
        ExportDeclaration entity = ExportDeclaration.builder()
                .docNo(docNo)
                .items("001")
                .prodType(prodType)
                .prodName(prodName)
                .exportQty(exportQty)
                .status(status)
                .taxRefundList(new ArrayList<>())
                .build();
        entity.setId(id);
        return entity;
    }

    private ExportDeclarationDto createDto(Long id, String docNo, String prodType,
            String prodName, BigDecimal exportQty, Integer status) {
        ExportDeclarationDto dto = new ExportDeclarationDto();
        dto.setId(id);
        dto.setDocNo(docNo);
        dto.setItems("001");
        dto.setProdType(prodType);
        dto.setProdName(prodName);
        dto.setExportQty(exportQty);
        dto.setStatus(status);
        return dto;
    }

    @SuppressWarnings("unchecked")
    @Test
    void search_returnsPageOfDtos() {
        ExportDeclaration entity = createEntity(1L, "EXP001", "類型A", "產品A",
                BigDecimal.TEN, ExportDeclaration._Status_Create);
        ExportDeclarationDto dto = createDto(1L, "EXP001", "類型A", "產品A",
                BigDecimal.TEN, ExportDeclaration._Status_Create);
        Pageable pageable = PageRequest.of(0, 10);
        Page<ExportDeclaration> entityPage = new PageImpl<>(List.of(entity), pageable, 1);

        when(repository.findAll(any(Specification.class), eq(pageable))).thenReturn(entityPage);
        when(mapper.toDto(entity)).thenReturn(dto);

        Page<ExportDeclarationDto> result = exportDeclarationService.search(
                null, null, null, null, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getDocNo()).isEqualTo("EXP001");
        verify(repository).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    void update_success() {
        Long id = 1L;
        ExportDeclaration existing = createEntity(id, "EXP001", "類型A", "產品A",
                BigDecimal.TEN, ExportDeclaration._Status_Create);
        ExportDeclarationDto inputDto = createDto(id, "EXP001", "類型B", "產品B",
                new BigDecimal("20"), ExportDeclaration._Status_Create);
        ExportDeclarationDto outputDto = createDto(id, "EXP001", "類型B", "產品B",
                new BigDecimal("20"), ExportDeclaration._Status_Create);

        when(repository.findById(id)).thenReturn(Optional.of(existing));
        when(repository.save(existing)).thenReturn(existing);
        when(mapper.toDto(existing)).thenReturn(outputDto);

        ExportDeclarationDto result = exportDeclarationService.update(id, inputDto);

        assertThat(result.getProdType()).isEqualTo("類型B");
        assertThat(result.getProdName()).isEqualTo("產品B");
        verify(repository).findById(id);
        verify(repository).save(existing);
    }

    @Test
    void update_notFound_throwsException() {
        Long id = 99L;
        ExportDeclarationDto dto = createDto(null, "EXP001", "類型A", "產品A",
                BigDecimal.TEN, ExportDeclaration._Status_Create);

        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> exportDeclarationService.update(id, dto))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void delete_success_whenStatusIsCreateAndNoTaxRefund() {
        Long id = 1L;
        ExportDeclaration entity = createEntity(id, "EXP001", "類型A", "產品A",
                BigDecimal.TEN, ExportDeclaration._Status_Create);

        when(repository.findById(id)).thenReturn(Optional.of(entity));

        exportDeclarationService.delete(id);

        verify(repository).deleteById(id);
    }

    @Test
    void delete_statusNotCreate_throwsException() {
        Long id = 1L;
        ExportDeclaration entity = createEntity(id, "EXP001", "類型A", "產品A",
                BigDecimal.TEN, ExportDeclaration._Status_Create_Refund);

        when(repository.findById(id)).thenReturn(Optional.of(entity));

        assertThatThrownBy(() -> exportDeclarationService.delete(id))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("不可刪除");

        verify(repository, never()).deleteById(id);
    }

    @Test
    void delete_hasTaxRefundList_throwsException() {
        Long id = 1L;
        ExportDeclaration entity = createEntity(id, "EXP001", "類型A", "產品A",
                BigDecimal.TEN, ExportDeclaration._Status_Create);
        entity.setTaxRefundList(List.of(new TaxRefund()));

        when(repository.findById(id)).thenReturn(Optional.of(entity));

        assertThatThrownBy(() -> exportDeclarationService.delete(id))
                .isInstanceOf(RuntimeException.class);

        verify(repository, never()).deleteById(id);
    }

    @Test
    void batchDelete_mixed_returnsCorrectCounts() {
        Long deletableId = 1L;
        Long nonDeletableId = 2L;
        ExportDeclaration deletable = createEntity(deletableId, "EXP001", "類型A", "產品A",
                BigDecimal.TEN, ExportDeclaration._Status_Create);
        ExportDeclaration nonDeletable = createEntity(nonDeletableId, "EXP002", "類型B", "產品B",
                BigDecimal.TEN, ExportDeclaration._Status_Create_Refund);

        when(repository.findById(deletableId)).thenReturn(Optional.of(deletable));
        when(repository.findById(nonDeletableId)).thenReturn(Optional.of(nonDeletable));

        ImportResultDto result = exportDeclarationService.batchDelete(
                List.of(deletableId, nonDeletableId));

        assertThat(result.getSuccessCount()).isEqualTo(1);
        assertThat(result.getErrorCount()).isEqualTo(1);
        assertThat(result.getErrorMessages()).isNotEmpty();
        verify(repository).deleteById(deletableId);
        verify(repository, never()).deleteById(nonDeletableId);
    }

    @Test
    void getDistinctDocNos_delegatesToRepository() {
        List<String> docNos = List.of("EXP001", "EXP002");

        when(repository.findDistinctDocNos()).thenReturn(docNos);

        List<String> result = exportDeclarationService.getDistinctDocNos();

        assertThat(result).containsExactly("EXP001", "EXP002");
        verify(repository).findDistinctDocNos();
    }
}
