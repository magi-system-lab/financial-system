package com.jimm0063.magi.debt.management.debtmanagementltesystem.infrastructure.persistence;

import com.jimm0063.magi.debt.management.debtmanagementltesystem.infrastructure.entity.SavingsGoalContributionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SavingsGoalContributionJpaRepository extends JpaRepository<SavingsGoalContributionEntity, Integer> {
    List<SavingsGoalContributionEntity> findAllBySavingsGoal_IdAndActiveTrueOrderByContributionDateDescIdDesc(Integer goalId);
    List<SavingsGoalContributionEntity> findAllBySavingsGoal_DebtSysUser_EmailAndSavingsGoal_ActiveTrueAndActiveTrueOrderByContributionDateDescIdDesc(String email);
}
