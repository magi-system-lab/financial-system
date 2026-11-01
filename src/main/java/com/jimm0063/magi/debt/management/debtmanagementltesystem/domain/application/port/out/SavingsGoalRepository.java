package com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.application.port.out;

import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.model.SavingsGoal;

import java.util.List;
import java.util.Optional;

public interface SavingsGoalRepository {
    List<SavingsGoal> findAllActiveByUser(String email);
    Optional<SavingsGoal> findById(Integer id);
    SavingsGoal save(SavingsGoal goal, String userEmail);
    /** Name, target, date, debt link and status. */
    SavingsGoal update(SavingsGoal goal);
    void delete(Integer id);
}
