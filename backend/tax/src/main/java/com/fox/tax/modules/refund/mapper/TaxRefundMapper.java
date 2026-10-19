package com.fox.tax.modules.refund.mapper;

import com.fox.tax.modules.refund.dto.ImportDeclarationDto;
import com.fox.tax.modules.refund.dto.TaxRefundDto;
import com.fox.tax.modules.refund.entity.TaxRefund;
import java.math.BigDecimal;
import java.util.List;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

/** 退稅核銷紀錄 MapStruct 轉換器 */
@Mapper(
    componentModel = "spring",
    uses = {
      ExportDeclarationMapper.class,
      ImportDeclarationMapper.class,
      TaxBomMapper.class
    },
    unmappedTargetPolicy = ReportingPolicy.IGNORE,
    builder = @org.mapstruct.Builder(disableBuilder = true))
public interface TaxRefundMapper {

  /** 將 TaxRefund Entity 轉換為 TaxRefundDto */
  TaxRefundDto toDto(TaxRefund entity);

  /** 將 TaxRefund Entity 列表轉換為 TaxRefundDto 列表 */
  List<TaxRefundDto> toDtoList(List<TaxRefund> entityList);

  /** 轉換後自動計算進口報單的未核銷可用餘額 (unverifiedQty = importQty - totalRefundQty) */
  @AfterMapping
  default void calculateImportUnverifiedQty(@MappingTarget TaxRefundDto dto) {
    ImportDeclarationDto importDto = dto.getImportDeclaration();
    if (importDto != null
        && importDto.getImportQty() != null
        && importDto.getTotalRefundQty() != null) {
      BigDecimal unverified =
          importDto.getImportQty().subtract(importDto.getTotalRefundQty());
      importDto.setUnverifiedQty(unverified);
    }
  }
}
