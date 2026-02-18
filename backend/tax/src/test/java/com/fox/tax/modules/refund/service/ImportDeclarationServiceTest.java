package com.fox.tax.modules.refund.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fox.tax.modules.refund.dto.ImportDeclarationDto;
import com.fox.tax.modules.refund.dto.ImportResultDto;
import com.fox.tax.modules.refund.entity.ImportDeclaration;
import com.fox.tax.modules.refund.mapper.ImportDeclarationMapper;
import com.fox.tax.modules.refund.repository.ImportDeclarationRepository;
import java.math.BigDecimal;
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
class ImportDeclarationServiceTest {

    @Mock
    private ImportDeclarationRepository repository;

    @Mock
    private ImportDeclarationMapper mapper;

    @InjectMocks
    private ImportDeclarationService importDeclarationService;

    private ImportDeclaration createEntity(Long id, String docNo, String materialName,
            BigDecimal importQty, BigDecimal totalRefundQty) {
        ImportDeclaration entity = ImportDeclaration.builder()
                .docNo(docNo)
                .items("001")
                .materialName(materialName)
                .materialUnit("KG")
                .materialSpec("規格A")
                .importQty(importQty)
                .totalRefundQty(totalRefundQty)
                .build();
        entity.setId(id);
        return entity;
    }

    private ImportDeclarationDto createDto(Long id, String docNo, String materialName,
            BigDecimal importQty, BigDecimal totalRefundQty) {
        ImportDeclarationDto dto = new ImportDeclarationDto();
        dto.setId(id);
        dto.setDocNo(docNo);
        dto.setItems("001");
        dto.setMaterialName(materialName);
        dto.setMaterialUnit("KG");
        dto.setMaterialSpec("規格A");
        dto.setImportQty(importQty);
        dto.setTotalRefundQty(totalRefundQty);
        return dto;
    }

    @SuppressWarnings("unchecked")
    @Test
    void search_noFilters_returnsPage() {
        ImportDeclaration entity = createEntity(1L, "IMP001", "材料A",
                BigDecimal.TEN, BigDecimal.ZERO);
        ImportDeclarationDto dto = createDto(1L, "IMP001", "材料A",
                BigDecimal.TEN, BigDecimal.ZERO);
        Pageable pageable = PageRequest.of(0, 10);
        Page<ImportDeclaration> entityPage = new PageImpl<>(List.of(entity), pageable, 1);

        when(repository.findAll(any(Specification.class), eq(pageable))).thenReturn(entityPage);
        when(mapper.toDto(entity)).thenReturn(dto);

        Page<ImportDeclarationDto> result = importDeclarationService.search(null, null, null, pageable);

        assertThat(result.getContent()).hasSize(1);
        verify(repository).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    void update_success() {
        Long id = 1L;
        ImportDeclaration existing = createEntity(id, "IMP001", "材料A",
                BigDecimal.TEN, BigDecimal.ZERO);
        ImportDeclarationDto inputDto = createDto(id, "IMP001", "材料B",
                new BigDecimal("20"), BigDecimal.ZERO);
        ImportDeclarationDto outputDto = createDto(id, "IMP001", "材料B",
                new BigDecimal("20"), BigDecimal.ZERO);
        outputDto.setUnverifiedQty(new BigDecimal("20"));
        outputDto.setVerificationStatus(1);

        when(repository.findById(id)).thenReturn(Optional.of(existing));
        when(repository.save(existing)).thenReturn(existing);
        when(mapper.toDto(existing)).thenReturn(outputDto);

        ImportDeclarationDto result = importDeclarationService.update(id, inputDto);

        assertThat(result.getMaterialName()).isEqualTo("材料B");
        verify(repository).findById(id);
        verify(repository).save(existing);
    }

    @Test
    void update_notFound_throwsException() {
        Long id = 99L;
        ImportDeclarationDto dto = createDto(null, "IMP001", "材料A",
                BigDecimal.TEN, BigDecimal.ZERO);

        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> importDeclarationService.update(id, dto))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void delete_success_whenTotalRefundQtyIsZero() {
        Long id = 1L;
        ImportDeclaration entity = createEntity(id, "IMP001", "材料A",
                BigDecimal.TEN, BigDecimal.ZERO);

        when(repository.findById(id)).thenReturn(Optional.of(entity));

        importDeclarationService.delete(id);

        verify(repository).deleteById(id);
    }

    @Test
    void delete_hasRefundQty_throwsException() {
        Long id = 1L;
        ImportDeclaration entity = createEntity(id, "IMP001", "材料A",
                BigDecimal.TEN, new BigDecimal("5"));

        when(repository.findById(id)).thenReturn(Optional.of(entity));

        assertThatThrownBy(() -> importDeclarationService.delete(id))
                .isInstanceOf(RuntimeException.class);

        verify(repository, never()).deleteById(id);
    }

    @Test
    void delete_notFound_throwsException() {
        Long id = 99L;

        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> importDeclarationService.delete(id))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void batchDelete_mixed_returnsCorrectCounts() {
        Long deletableId = 1L;
        Long nonDeletableId = 2L;
        ImportDeclaration deletable = createEntity(deletableId, "IMP001", "材料A",
                BigDecimal.TEN, BigDecimal.ZERO);
        ImportDeclaration nonDeletable = createEntity(nonDeletableId, "IMP002", "材料B",
                BigDecimal.TEN, new BigDecimal("5"));

        when(repository.findById(deletableId)).thenReturn(Optional.of(deletable));
        when(repository.findById(nonDeletableId)).thenReturn(Optional.of(nonDeletable));

        ImportResultDto result = importDeclarationService.batchDelete(List.of(deletableId, nonDeletableId));

        assertThat(result.getSuccessCount()).isEqualTo(1);
        assertThat(result.getErrorCount()).isEqualTo(1);
        assertThat(result.getErrorMessages()).isNotEmpty();
        verify(repository).deleteById(deletableId);
        verify(repository, never()).deleteById(nonDeletableId);
    }

    @Test
    void getDistinctDocNos_delegatesToRepository() {
        List<String> docNos = List.of("IMP001", "IMP002", "IMP003");

        when(repository.findDistinctDocNos()).thenReturn(docNos);

        List<String> result = importDeclarationService.getDistinctDocNos();

        assertThat(result).containsExactly("IMP001", "IMP002", "IMP003");
        verify(repository).findDistinctDocNos();
    }
}
