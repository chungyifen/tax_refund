package com.fox.tax.modules.refund.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fox.tax.modules.refund.dto.TaxBomDto;
import com.fox.tax.modules.refund.entity.TaxBom;
import com.fox.tax.modules.refund.mapper.TaxBomMapper;
import com.fox.tax.modules.refund.repository.TaxBomRepository;
import com.querydsl.core.types.Predicate;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TaxBomServiceTest {

    @Mock
    private TaxBomRepository repository;

    @Mock
    private TaxBomMapper mapper;

    @InjectMocks
    private TaxBomService taxBomService;

    private TaxBom createEntity(Long id, String docNo, String prodName, String prodType) {
        TaxBom entity = TaxBom.builder()
                .docNo(docNo)
                .prodName(prodName)
                .prodType(prodType)
                .prodUnit("KG")
                .materialNum(1)
                .materialName("材料A")
                .materialUnit("KG")
                .materialSpec("規格A")
                .usageQty(BigDecimal.ONE)
                .build();
        entity.setId(id);
        return entity;
    }

    private TaxBomDto createDto(Long id, String docNo, String prodName, String prodType) {
        TaxBomDto dto = new TaxBomDto();
        dto.setId(id);
        dto.setDocNo(docNo);
        dto.setProdName(prodName);
        dto.setProdType(prodType);
        dto.setProdUnit("KG");
        dto.setMaterialNum(1);
        dto.setMaterialName("材料A");
        dto.setMaterialUnit("KG");
        dto.setMaterialSpec("規格A");
        dto.setUsageQty(BigDecimal.ONE);
        return dto;
    }

    @Test
    void findAll_noFilters_returnsAll() {
        TaxBom entity1 = createEntity(1L, "DOC001", "產品A", "類型A");
        TaxBom entity2 = createEntity(2L, "DOC002", "產品B", "類型B");
        List<TaxBom> entities = List.of(entity1, entity2);
        TaxBomDto dto1 = createDto(1L, "DOC001", "產品A", "類型A");
        TaxBomDto dto2 = createDto(2L, "DOC002", "產品B", "類型B");
        List<TaxBomDto> dtos = List.of(dto1, dto2);

        when(repository.findAll(any(Predicate.class))).thenReturn(entities);
        when(mapper.toDtoList(entities)).thenReturn(dtos);

        List<TaxBomDto> result = taxBomService.findAll(null, null, null);

        assertThat(result).hasSize(2);
        verify(repository).findAll(any(Predicate.class));
        verify(mapper).toDtoList(entities);
    }

    @Test
    void findAll_withDocNoFilter_returnsMappedResults() {
        TaxBom entity = createEntity(1L, "DOC001", "產品A", "類型A");
        List<TaxBom> entities = List.of(entity);
        TaxBomDto dto = createDto(1L, "DOC001", "產品A", "類型A");
        List<TaxBomDto> dtos = List.of(dto);

        when(repository.findAll(any(Predicate.class))).thenReturn(entities);
        when(mapper.toDtoList(entities)).thenReturn(dtos);

        List<TaxBomDto> result = taxBomService.findAll("DOC001", null, null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getDocNo()).isEqualTo("DOC001");
        verify(repository).findAll(any(Predicate.class));
        verify(mapper).toDtoList(entities);
    }

    @Test
    void save_success() {
        TaxBomDto inputDto = createDto(null, "DOC001", "產品A", "類型A");
        TaxBom entity = createEntity(null, "DOC001", "產品A", "類型A");
        TaxBom savedEntity = createEntity(1L, "DOC001", "產品A", "類型A");
        TaxBomDto outputDto = createDto(1L, "DOC001", "產品A", "類型A");

        when(mapper.toEntity(inputDto)).thenReturn(entity);
        when(repository.save(entity)).thenReturn(savedEntity);
        when(mapper.toDto(savedEntity)).thenReturn(outputDto);

        TaxBomDto result = taxBomService.save(inputDto);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getDocNo()).isEqualTo("DOC001");
        verify(mapper).toEntity(inputDto);
        verify(repository).save(entity);
        verify(mapper).toDto(savedEntity);
    }

    @Test
    void update_success() {
        Long id = 1L;
        TaxBomDto inputDto = createDto(null, "DOC001", "產品B", "類型B");
        TaxBom existingEntity = createEntity(id, "DOC001", "產品A", "類型A");
        TaxBom savedEntity = createEntity(id, "DOC001", "產品B", "類型B");
        TaxBomDto outputDto = createDto(id, "DOC001", "產品B", "類型B");

        when(repository.findById(id)).thenReturn(Optional.of(existingEntity));
        when(repository.save(existingEntity)).thenReturn(savedEntity);
        when(mapper.toDto(savedEntity)).thenReturn(outputDto);

        TaxBomDto result = taxBomService.update(id, inputDto);

        assertThat(result.getId()).isEqualTo(id);
        assertThat(result.getProdName()).isEqualTo("產品B");
        verify(repository).findById(id);
        verify(repository).save(existingEntity);
        verify(mapper).toDto(savedEntity);
    }

    @Test
    void update_notFound_throwsException() {
        Long id = 99L;
        TaxBomDto inputDto = createDto(null, "DOC001", "產品A", "類型A");

        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taxBomService.update(id, inputDto))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void delete_success() {
        Long id = 1L;

        taxBomService.delete(id);

        verify(repository).deleteById(id);
    }

    @Test
    void deleteBatch_success() {
        List<Long> ids = List.of(1L, 2L, 3L);

        taxBomService.deleteBatch(ids);

        verify(repository).deleteAllById(ids);
    }
}
