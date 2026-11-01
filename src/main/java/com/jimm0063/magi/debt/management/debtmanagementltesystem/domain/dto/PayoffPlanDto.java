package com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.dto;

import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.enums.PayoffStrategy;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

/**
 * The coach's attack plan: what to pay off and when, putting {@code extraMonthly} aside every month
 * (plus {@code lumpSum} today) and, with {@code snowball}, every payment that frees up as well.
 *
 * @param totalBalance     what it costs to pay everything off today
 * @param baselineDebtFree first month with nothing to pay without the plan (null when nothing is owed)
 * @param planDebtFree     same, with the plan
 * @param monthsSaved      months between both dates
 * @param interestSaved    interest no longer paid
 */
public record PayoffPlanDto(
        PayoffStrategy strategy,
        BigDecimal extraMonthly,
        BigDecimal lumpSum,
        boolean snowball,
        BigDecimal totalBalance,
        YearMonth baselineDebtFree,
        YearMonth planDebtFree,
        int monthsSaved,
        BigDecimal interestSaved,
        List<PayoffStepDto> steps,
        List<PayoffMonthDto> months
) implements Serializable {
    public boolean hasDebts() { return baselineDebtFree != null; }
    public boolean hasSteps() { return !steps.isEmpty(); }
}
