package com.fox.tax.modules.refund.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import com.fox.tax.modules.refund.dto.ExportDeclarationDto;
import com.fox.tax.modules.refund.entity.ExportDeclaration;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE, builder = @org.mapstruct.Builder(disableBuilder = true))
public interface ExportDeclarationMapper {

    ExportDeclarationDto toDto(ExportDeclaration entity);

    ExportDeclaration toEntity(ExportDeclarationDto dto);
}
