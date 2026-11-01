package com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.application.port.out;

import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.model.SavingsGoalContribution;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface SavingsGoalContributionRepository {
    /** Newest first. */
    List<SavingsGoalContribution> findAllByGoal(Integer goalId);
    /** Contributions of every goal of the user, by goal id (newest first). */
    Map<Integer, List<SavingsGoalContribution>> findAllByUser(String email);
    Optional<Integer> findGoalIdOf(Integer contributionId);
    SavingsGoalContribution save(SavingsGoalContribution contribution, Integer goalId);
    void delete(Integer id);
}
