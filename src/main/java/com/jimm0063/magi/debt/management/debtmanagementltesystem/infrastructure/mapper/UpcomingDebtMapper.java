package com.jimm0063.magi.debt.management.debtmanagementltesystem.infrastructure.mapper;

import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.model.UpcomingDebt;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.infrastructure.entity.UpcomingDebtEntity;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface UpcomingDebtMapper {
    UpcomingDebtEntity toEntity(UpcomingDebt upcomingDebt);
    UpcomingDebt toModel(UpcomingDebtEntity upcomingDebtEntity);
}
