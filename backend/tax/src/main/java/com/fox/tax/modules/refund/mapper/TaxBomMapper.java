package com.fox.tax.modules.refund.mapper;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import com.fox.tax.modules.refund.dto.TaxBomDto;
import com.fox.tax.modules.refund.entity.TaxBom;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE, builder = @org.mapstruct.Builder(disableBuilder = true))
public interface TaxBomMapper {

    TaxBomDto toDto(TaxBom entity);

    TaxBom toEntity(TaxBomDto dto);

    List<TaxBomDto> toDtoList(List<TaxBom> entityList);

    List<TaxBom> toEntityList(List<TaxBomDto> dtoList);
}
