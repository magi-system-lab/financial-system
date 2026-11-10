package com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.utils;

import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.dto.DebtFreeOptionsDto;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.dto.DebtFreeOptionsDto.Option;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.dto.DebtFreeOptionsDto.Zone;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.dto.PayoffPlanDto;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.enums.PayoffStrategy;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.model.Debt;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

/**
 * Builds the coach's slider: for every extra amount (0 … what's left from the salary), the earliest debt-free month
 * the attack plan reaches with snowball, trying each strategy and keeping the earliest (ties → most relief first).
 * More money never shows a later date: when a bigger amount lands on a worse plan, the previous plan is kept
 * (the extra money is simply put aside).
 */
public final class DebtFreeAdvisor {

    /** Points on the slider, at most. */
    static final int MAX_POINTS = 50;
    /** Shares of the free money for the recommended, demanding and limit presets. */
    static final BigDecimal COMFORTABLE = new BigDecimal("0.15");
    static final BigDecimal DEMANDING = new BigDecimal("0.35");
    static final BigDecimal LIMIT = new BigDecimal("0.60");
    private static final int[] NICE_STEPS = {50, 100, 200, 250, 500, 1000, 2000, 2500, 5000, 10000, 20000, 25000, 50000};

    private DebtFreeAdvisor() {}

    /** @param free what's left from the salary after fixed expenses and debt payments; null without salary */
    public static DebtFreeOptionsDto options(List<Debt> debts, YearMonth current, BigDecimal free) {
        List<Debt> active = debts.stream()
                .filter(d -> d.getMonthlyPayment() != null && d.getMonthlyPayment().signum() > 0
                        && PayoffSimulator.monthsLeft(d) >= 1)
                .toList();
        BigDecimal paymentNow = active.stream().map(Debt::getMonthlyPayment).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (active.isEmpty()) return new DebtFreeOptionsDto(null, paymentNow, free, List.of(), 0, 0, 0);

        boolean hasFree = free != null && free.signum() > 0;
        BigDecimal max = hasFree ? free : paymentNow.max(BigDecimal.valueOf(1000));
        int step = step(max);

        List<Option> options = new ArrayList<>();
        YearMonth baseline = null;
        PayoffPlanDto best = null;
        for (int extra = 0; extra <= max.intValue(); extra += step) {
            PayoffPlanDto plan = fastest(active, current, BigDecimal.valueOf(extra));
            if (baseline == null) baseline = plan.baselineDebtFree();
            if (best == null || plan.planDebtFree().isBefore(best.planDebtFree())) best = plan;
            BigDecimal x = BigDecimal.valueOf(extra);
            options.add(new Option(x, paymentNow.add(x), best.planDebtFree(), best.monthsSaved(),
                    zone(x, hasFree ? free : null), best.steps().size(),
                    best.steps().subList(0, Math.min(3, best.steps().size()))));
        }

        BigDecimal base = hasFree ? free : max;
        int recommended = recommended(options, base.multiply(COMFORTABLE));
        int demanding = Math.max(recommended, atLeast(options, base.multiply(DEMANDING)));
        int limit = Math.max(demanding, atLeast(options, base.multiply(LIMIT)));
        return new DebtFreeOptionsDto(baseline, paymentNow, free, options, recommended, demanding, limit);
    }

    /** The plan with the earliest debt-free month among the strategies, snowball on. */
    public static PayoffPlanDto fastest(List<Debt> debts, YearMonth current, BigDecimal extra) {
        PayoffPlanDto best = null;
        for (PayoffStrategy s : PayoffStrategy.values()) {   // FLOW first: wins ties
            PayoffPlanDto plan = PayoffSimulator.simulate(debts, current, extra, BigDecimal.ZERO, s, true);
            if (best == null || plan.planDebtFree().isBefore(best.planDebtFree())) best = plan;
        }
        return best;
    }

    /** First option from the comfortable amount on that actually brings the date closer; else the comfortable one. */
    static int recommended(List<Option> options, BigDecimal comfortable) {
        int from = atLeast(options, comfortable);
        for (int i = from; i < options.size(); i++) {
            if (options.get(i).monthsSaved() > options.get(0).monthsSaved()) return i;
        }
        return from;
    }

    /** First option whose extra reaches {@code amount} (the last one when none does). */
    static int atLeast(List<Option> options, BigDecimal amount) {
        for (int i = 0; i < options.size(); i++) {
            if (options.get(i).extra().compareTo(amount) >= 0) return i;
        }
        return options.size() - 1;
    }

    static Zone zone(BigDecimal extra, BigDecimal free) {
        if (free == null) return Zone.NONE;
        BigDecimal share = extra.divide(free, 4, RoundingMode.HALF_UP);
        if (share.compareTo(new BigDecimal("0.25")) <= 0) return Zone.COMFORTABLE;
        if (share.compareTo(new BigDecimal("0.50")) <= 0) return Zone.DEMANDING;
        return Zone.LIMIT;
    }

    /** A round step that keeps the slider at MAX_POINTS points or fewer. */
    static int step(BigDecimal max) {
        for (int s : NICE_STEPS) {
            if (max.divide(BigDecimal.valueOf(s), 0, RoundingMode.DOWN).intValue() <= MAX_POINTS) return s;
        }
        return max.divide(BigDecimal.valueOf(MAX_POINTS), 0, RoundingMode.UP).intValue();
    }
}
