package com.jimm0063.magi.debt.management.debtmanagementltesystem.infrastructure.mapper;

import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.model.SavingsGoal;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.infrastructure.entity.SavingsGoalEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface SavingsGoalMapper {
    @Mapping(target = "debtSysUser", ignore = true)
    SavingsGoalEntity toEntity(SavingsGoal goal);
    SavingsGoal toModel(SavingsGoalEntity entity);
}
