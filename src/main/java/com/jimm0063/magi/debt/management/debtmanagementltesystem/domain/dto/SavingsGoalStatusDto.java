package com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.dto;

import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.model.Debt;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.model.SavingsGoal;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.model.SavingsGoalContribution;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

/**
 * A savings goal with its progress.
 *
 * @param target           live payoff balance of the linked debt, else the goal's amount
 * @param debt             the linked debt while it is still active (null otherwise)
 * @param debtGone         linked to a debt that is no longer active (finished, paid off or deleted)
 * @param monthsLeft       months until the target date (0 = this month or overdue); null without a date
 * @param suggestedMonthly what to put aside each month to arrive on time; null without a date or when reached
 * @param behindBy         how far it is behind a steady pace since it was created (0 = on track)
 */
public record SavingsGoalStatusDto(
        SavingsGoal goal,
        BigDecimal saved,
        BigDecimal target,
        BigDecimal remaining,
        int percent,
        Debt debt,
        boolean debtGone,
        Integer monthsLeft,
        BigDecimal suggestedMonthly,
        BigDecimal behindBy,
        List<SavingsGoalContribution> contributions
) implements Serializable {
    public boolean reached() { return target.signum() > 0 && remaining.signum() == 0; }
    public boolean behind() { return behindBy.signum() > 0; }
    /** Enough money is put aside to pay the linked debt off right now. */
    public boolean canPayOffDebt() { return goal.isOpen() && debt != null && reached(); }
    public YearMonth targetMonth() { return goal.getTargetDate() == null ? null : YearMonth.from(goal.getTargetDate()); }
}
