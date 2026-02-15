package com.fox.tax.modules.refund.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import com.fox.tax.modules.refund.dto.ImportDeclarationDto;
import com.fox.tax.modules.refund.entity.ImportDeclaration;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE, builder = @org.mapstruct.Builder(disableBuilder = true))
public interface ImportDeclarationMapper {

    ImportDeclarationDto toDto(ImportDeclaration entity);

    ImportDeclaration toEntity(ImportDeclarationDto dto);
}
