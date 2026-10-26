package com.jimm0063.magi.debt.management.debtmanagementltesystem.infrastructure.mapper;

import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.model.WishItem;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.infrastructure.entity.WishItemEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface WishItemMapper {
    @Mapping(target = "debtSysUser", ignore = true)
    WishItemEntity toEntity(WishItem wishItem);
    WishItem toModel(WishItemEntity wishItemEntity);
}
