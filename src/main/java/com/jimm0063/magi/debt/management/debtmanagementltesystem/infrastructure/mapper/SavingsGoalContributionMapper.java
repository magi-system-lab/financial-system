package com.jimm0063.magi.debt.management.debtmanagementltesystem.infrastructure.mapper;

import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.model.SavingsGoalContribution;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.infrastructure.entity.SavingsGoalContributionEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface SavingsGoalContributionMapper {
    @Mapping(target = "savingsGoal", ignore = true)
    SavingsGoalContributionEntity toEntity(SavingsGoalContribution contribution);
    SavingsGoalContribution toModel(SavingsGoalContributionEntity entity);
}
