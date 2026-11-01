package com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.utils;

import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.dto.PayoffMonthDto;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.dto.PayoffPlanDto;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.dto.PayoffStepDto;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.enums.PayoffStrategy;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.model.Debt;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Month-by-month simulation of the coach's attack plan.
 * <p>
 * Every month the extra money (and, with snowball, every payment that frees up) is put aside; as soon as what is
 * put aside covers the whole balance of the next debt in the strategy's order, that debt is paid off and its monthly
 * payment joins the money put aside from the next month on. The one-off amount is used today.
 * <p>
 * Timeline as in the progression: a debt with {@code n} payments left pays in the next {@code n} months.
 * A debt with a single payment left is never a target (paying it off early only moves that payment a few days).
 */
public final class PayoffSimulator {

    private PayoffSimulator() {}

    /** Payments left (0 when the debt is incomplete or finished). */
    public static int monthsLeft(Debt d) {
        if (d.getMaxFinancingTerm() == null || d.getCurrentInstallment() == null) return 0;
        return Math.max(0, d.getMaxFinancingTerm() - d.getCurrentInstallment());
    }

    /** What it costs to pay the debt off today: payments left, minus the interest they include. */
    public static BigDecimal payoffBalance(Debt d) {
        return balance(d.getMonthlyPayment(), monthsLeft(d), d.getAnnualInterestRate());
    }

    /** At least 2 payments left: paying it off changes something. */
    public static boolean worthPayingOff(Debt d) {
        return valid(d) && monthsLeft(d) >= 2;
    }

    public static boolean chargesInterest(Debt d) {
        return d.getAnnualInterestRate() != null && d.getAnnualInterestRate().signum() > 0;
    }

    static BigDecimal balance(BigDecimal monthly, int left, BigDecimal annualRate) {
        if (monthly == null || left <= 0) return BigDecimal.ZERO;
        if (annualRate == null || annualRate.signum() <= 0)
            return monthly.multiply(BigDecimal.valueOf(left)).setScale(2, RoundingMode.HALF_UP);
        double r = annualRate.doubleValue() / 1200;
        double pv = monthly.doubleValue() * (1 - Math.pow(1 + r, -left)) / r;
        return BigDecimal.valueOf(pv).setScale(2, RoundingMode.HALF_UP);
    }

    private static boolean valid(Debt d) {
        return d.getMonthlyPayment() != null && d.getMonthlyPayment().signum() > 0 && monthsLeft(d) >= 1;
    }

    public static PayoffPlanDto simulate(List<Debt> debts, YearMonth current, BigDecimal extraMonthly,
                                         BigDecimal lumpSum, PayoffStrategy strategy, boolean snowball) {
        BigDecimal extra = positive(extraMonthly);
        BigDecimal lump = positive(lumpSum);
        PayoffStrategy order = strategy == null ? PayoffStrategy.FLOW : strategy;

        List<Slot> slots = debts.stream().filter(PayoffSimulator::valid).map(Slot::new).toList();
        int maxLeft = slots.stream().mapToInt(s -> s.initialLeft).max().orElse(0);
        BigDecimal totalBalance = slots.stream().map(Slot::balance).reduce(BigDecimal.ZERO, BigDecimal::add);
        YearMonth baselineDebtFree = maxLeft == 0 ? null : current.plusMonths(maxLeft + 1L);

        List<PayoffStepDto> steps = new ArrayList<>();
        BigDecimal[] saved = {lump};
        BigDecimal rolled = payOff(current, current, slots, saved, order, steps);

        List<PayoffMonthDto> months = new ArrayList<>();
        YearMonth lastPlanPayment = null;
        for (int m = 1; m <= maxLeft; m++) {
            YearMonth month = current.plusMonths(m);
            boolean anyOpen = slots.stream().anyMatch(s -> !s.closed);
            BigDecimal setAside = anyOpen ? extra.add(rolled) : BigDecimal.ZERO;
            saved[0] = saved[0].add(setAside);

            BigDecimal planPayment = BigDecimal.ZERO;
            BigDecimal finishedNow = BigDecimal.ZERO;
            for (Slot s : slots) {
                if (s.closed) continue;
                planPayment = planPayment.add(s.monthly());
                s.left--;
                if (s.left == 0) {
                    s.closed = true;
                    finishedNow = finishedNow.add(s.monthly());
                }
            }
            if (planPayment.signum() > 0) lastPlanPayment = month;
            final int offset = m;
            BigDecimal baselinePayment = slots.stream().filter(s -> s.initialLeft >= offset)
                    .map(Slot::monthly).reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal freed = payOff(month, current, slots, saved, order, steps);
            rolled = rolled.add(freed);
            if (snowball) rolled = rolled.add(finishedNow);
            months.add(new PayoffMonthDto(month, baselinePayment, planPayment, setAside, saved[0]));
        }

        YearMonth planDebtFree = slots.isEmpty() ? null
                : lastPlanPayment == null ? current.plusMonths(1) : lastPlanPayment.plusMonths(1);
        int monthsSaved = baselineDebtFree == null ? 0 : (int) planDebtFree.until(baselineDebtFree, ChronoUnit.MONTHS);
        BigDecimal interestSaved = steps.stream().map(PayoffStepDto::interestSaved).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new PayoffPlanDto(order, extra, lump, snowball, totalBalance, baselineDebtFree, planDebtFree,
                monthsSaved, interestSaved, steps, months);
    }

    /** Pays off debts in order while the money put aside covers the next one; returns the monthly payments freed. */
    private static BigDecimal payOff(YearMonth month, YearMonth current, List<Slot> slots, BigDecimal[] saved,
                                     PayoffStrategy strategy, List<PayoffStepDto> steps) {
        BigDecimal freed = BigDecimal.ZERO;
        while (true) {
            Slot target = slots.stream().filter(s -> !s.closed && s.left >= 2)
                    .min(order(strategy)).orElse(null);
            if (target == null) break;
            BigDecimal amount = target.balance();
            if (saved[0].compareTo(amount) < 0) break;
            saved[0] = saved[0].subtract(amount);
            target.closed = true;
            Debt d = target.debt;
            BigDecimal interest = target.monthly().multiply(BigDecimal.valueOf(target.left)).subtract(amount);
            steps.add(new PayoffStepDto(month, d.getId(), d.getHashSum(), d.getDescription(),
                    d.getDebtAccount() != null ? d.getDebtAccount().getName() : "",
                    amount, target.monthly(), target.left, current.plusMonths(target.initialLeft),
                    interest.max(BigDecimal.ZERO)));
            freed = freed.add(target.monthly());
        }
        return freed;
    }

    private static Comparator<Slot> order(PayoffStrategy strategy) {
        Comparator<Slot> interestFirst = Comparator.comparing(Slot::rate, Comparator.reverseOrder());
        Comparator<Slot> then = switch (strategy) {
            case FLOW -> Comparator.comparing(Slot::freedPerPeso, Comparator.reverseOrder())
                    .thenComparing(Slot::monthly, Comparator.reverseOrder());
            case SMALLEST -> Comparator.comparing(Slot::balance).thenComparingInt(s -> s.left);
            case LONGEST -> Comparator.comparingInt((Slot s) -> s.left).reversed().thenComparing(Slot::balance);
        };
        return interestFirst.thenComparing(then);
    }

    private static BigDecimal positive(BigDecimal v) {
        return v == null || v.signum() < 0 ? BigDecimal.ZERO : v;
    }

    private static final class Slot {
        final Debt debt;
        final int initialLeft;
        int left;
        boolean closed;

        Slot(Debt debt) {
            this.debt = debt;
            this.initialLeft = monthsLeft(debt);
            this.left = initialLeft;
        }

        BigDecimal monthly() { return debt.getMonthlyPayment(); }

        BigDecimal rate() { return chargesInterest(debt) ? debt.getAnnualInterestRate() : BigDecimal.ZERO; }

        BigDecimal balance() { return PayoffSimulator.balance(monthly(), left, debt.getAnnualInterestRate()); }

        double freedPerPeso() {
            BigDecimal b = balance();
            return b.signum() == 0 ? 0 : monthly().doubleValue() / b.doubleValue();
        }
    }
}
