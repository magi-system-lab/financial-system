package com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.dto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

/**
 * "Pay this much more and you're debt-free in month X": one option per extra amount on the coach's slider.
 * Every option keeps paying what is paid today plus the extra; each payment that frees up goes to the next debt
 * (snowball), so the total paid per month stays at {@code paymentNow + extra} until the last debt is gone.
 *
 * @param baselineDebtFree first month with nothing to pay when nothing changes
 * @param paymentNow       next month's debt payments
 * @param free             what's left from the salary each month after fixed expenses and debt payments; null without salary
 * @param recommended      index of the comfortable option the coach suggests
 * @param demanding        index of a tighter option
 * @param limit            index of the tightest sensible option
 */
public record DebtFreeOptionsDto(
        YearMonth baselineDebtFree,
        BigDecimal paymentNow,
        BigDecimal free,
        List<Option> options,
        int recommended,
        int demanding,
        int limit
) implements Serializable {

    /** How hard an extra amount squeezes the month: share of {@code free} (≤ 25% / ≤ 50% / more); NONE without salary. */
    public enum Zone { COMFORTABLE, DEMANDING, LIMIT, NONE }

    /**
     * @param totalMonthly paymentNow + extra: what is paid every month until debt-free
     * @param firstSteps   the first payoffs of that plan (at most 3)
     */
    public record Option(BigDecimal extra, BigDecimal totalMonthly, YearMonth debtFree, int monthsSaved, Zone zone,
                         int payoffs, List<PayoffStepDto> firstSteps) implements Serializable {}

    public boolean hasOptions() { return !options.isEmpty(); }
    public Option recommendedOption() { return options.get(recommended); }
}
