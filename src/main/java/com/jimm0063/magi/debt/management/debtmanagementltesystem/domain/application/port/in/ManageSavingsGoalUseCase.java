package com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.application.port.in;

import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.dto.SavingsGoalStatusDto;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.model.SavingsGoal;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.model.SavingsGoalContribution;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface ManageSavingsGoalUseCase {
    /** Every goal (active, completed and cancelled) with its progress, active ones first. */
    List<SavingsGoalStatusDto> getByEmail(String email);
    SavingsGoalStatusDto getStatus(String email, Integer goalId);
    /** With debtId the target follows that debt's payoff balance and targetAmount is ignored. */
    SavingsGoal create(String email, String name, BigDecimal targetAmount, LocalDate targetDate, Integer debtId);
    SavingsGoal update(String email, Integer goalId, String name, BigDecimal targetAmount, LocalDate targetDate, Integer debtId);
    /** Negative amount = money taken out. Date defaults to today. */
    SavingsGoalContribution contribute(Integer goalId, BigDecimal amount, LocalDate date, String note);
    void deleteContribution(Integer contributionId);
    /** Uses the money put aside to pay the linked debt off: the debt is closed and the goal completed. */
    SavingsGoalStatusDto payOffDebt(String email, Integer goalId);
    SavingsGoal complete(Integer goalId);
    SavingsGoal cancel(Integer goalId);
    SavingsGoal reopen(Integer goalId);
    void delete(Integer goalId);
}
