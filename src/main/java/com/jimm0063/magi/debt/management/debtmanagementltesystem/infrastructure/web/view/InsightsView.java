package com.jimm0063.magi.debt.management.debtmanagementltesystem.infrastructure.web.view;

import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.dto.CardProgressionDto;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.dto.MonthProgressionDto;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.model.Debt;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Everything the Insights page shows, precomputed from the month-by-month progression so the template
 * stays declarative. Each month carries its own panel (total, change, finishing debts, per-card amounts);
 * the page switches panels client-side when a bar is tapped.
 *
 * @param months     the visible months (6, 12 or 24), first = next payment month
 * @param debtFree   first month with nothing left to pay (null when nothing is owed at all)
 * @param lastPayment last month with a payment (null when nothing is owed)
 * @param reliefMonth month used for the "pay less by …" tile (3 months after the first)
 * @param relief     first month total minus reliefMonth total (positive = paying less)
 */
public record InsightsView(
        List<Month> months,
        YearMonth debtFree,
        YearMonth lastPayment,
        YearMonth reliefMonth,
        BigDecimal relief,
        Integer debtLimitPercent,     // user's max share of salary for debt payments; null = not set
        BigDecimal monthlyCommitments, // fixed expenses + this month's payments
        Double safetyNetMonths        // savings / monthlyCommitments; null when it can't be computed
) {
    /** Recommended emergency fund, in months of commitments. */
    public static final int SAFETY_GOAL_MONTHS = 3;

    /** How far ahead the debt-free date is searched. */
    static final int HORIZON_MONTHS = 120;
    /** Finishing soon = the selected month and the next two. */
    static final int FINISHING_WINDOW = 3;

    public record Month(
            int index,
            YearMonth month,
            BigDecimal total,
            BigDecimal upcoming,
            int activeDebts,
            BigDecimal change,            // vs previous month (vs next month for the first one)
            boolean changeVsNext,
            BigDecimal leftFromSalary,    // free to spend: salary - fixed expenses - total; null without salary
            int barPercent,               // height relative to the tallest visible month
            int freePercent,              // |leftFromSalary| relative to the largest visible one
            Integer debtSharePercent,     // total / salary; null without salary
            boolean overLimit,            // debtSharePercent above the user's limit
            int upcomingPercent,          // share of the bar that is upcoming purchases
            List<Card> cards,
            List<Finishing> finishing
    ) {
        public YearMonth compareMonth() { return changeVsNext ? month.plusMonths(1) : month.minusMonths(1); }
        public boolean paysLess() { return change.signum() < 0; }
        public boolean unchanged() { return change.abs().compareTo(BigDecimal.ONE) < 0; }
        public boolean overspent() { return leftFromSalary != null && leftFromSalary.signum() < 0; }
    }

    public record Card(String code, String name, BigDecimal amount, int activeDebts, boolean hasUpcoming,
                       int barPercent, boolean linkable) {}

    public record Finishing(String description, String accountName, BigDecimal monthlyPayment,
                            YearMonth lastPayment, int paidAtMonth, int term, String ringDash) {}

    public boolean hasDebts() { return lastPayment != null; }

    /** Savings still missing to reach {@link #SAFETY_GOAL_MONTHS}; zero once reached. */
    public BigDecimal safetyNetGap(Double savings) {
        if (monthlyCommitments == null) return BigDecimal.ZERO;
        BigDecimal gap = monthlyCommitments.multiply(BigDecimal.valueOf(SAFETY_GOAL_MONTHS))
                .subtract(BigDecimal.valueOf(savings == null ? 0 : savings));
        return gap.signum() > 0 ? gap : BigDecimal.ZERO;
    }

    public int safetyGoalMonths() { return SAFETY_GOAL_MONTHS; }

    public int safetyNetPercent() {
        if (safetyNetMonths == null) return 0;
        return (int) Math.min(100, Math.round(safetyNetMonths * 100 / SAFETY_GOAL_MONTHS));
    }

    public static InsightsView from(List<MonthProgressionDto> progression, int visibleMonths,
                                    Double salary, Double fixedExpenses) {
        return from(progression, visibleMonths, salary, fixedExpenses, null, null);
    }

    public static InsightsView from(List<MonthProgressionDto> progression, int visibleMonths,
                                    Double salary, Double fixedExpenses, Double savings, Integer debtLimitPercent) {
        List<MonthProgressionDto> visible = progression.subList(0, Math.min(visibleMonths, progression.size()));
        BigDecimal max = visible.stream().map(MonthProgressionDto::totalPayment)
                .max(Comparator.naturalOrder()).orElse(BigDecimal.ZERO);

        BigDecimal fixed = BigDecimal.valueOf(fixedExpenses == null ? 0 : fixedExpenses);
        BigDecimal maxFree = salary == null ? BigDecimal.ZERO : visible.stream()
                .map(m -> BigDecimal.valueOf(salary).subtract(fixed).subtract(m.totalPayment()).abs())
                .max(Comparator.naturalOrder()).orElse(BigDecimal.ZERO);

        List<Month> months = new ArrayList<>();
        for (int i = 0; i < visible.size(); i++) {
            MonthProgressionDto m = visible.get(i);
            boolean vsNext = i == 0;
            BigDecimal other = vsNext
                    ? (progression.size() > 1 ? progression.get(1).totalPayment() : BigDecimal.ZERO)
                    : progression.get(i - 1).totalPayment();
            BigDecimal change = vsNext ? other.subtract(m.totalPayment()) : m.totalPayment().subtract(other);
            BigDecimal left = salary == null ? null
                    : BigDecimal.valueOf(salary).subtract(fixed).subtract(m.totalPayment());
            Integer share = salary == null || salary <= 0 ? null
                    : percent(m.totalPayment(), BigDecimal.valueOf(salary));
            boolean overLimit = share != null && debtLimitPercent != null && share > debtLimitPercent;
            months.add(new Month(i, m.month(), m.totalPayment(), m.totalUpcomingPayment(), m.totalActiveDebtCount()
                    + m.allClosingDebts().size(), change, vsNext, left,
                    percent(m.totalPayment(), max), left == null ? 0 : Math.max(2, percent(left.abs(), maxFree)),
                    share, overLimit,
                    percent(m.totalUpcomingPayment(), m.totalPayment()),
                    cards(m), finishing(progression, i)));
        }

        YearMonth last = null;
        for (MonthProgressionDto m : progression) {
            if (m.totalPayment().signum() > 0) last = m.month();
        }
        YearMonth debtFree = last == null ? null : last.plusMonths(1);
        YearMonth reliefMonth = progression.size() > 3 ? progression.get(3).month() : null;
        BigDecimal relief = reliefMonth == null ? BigDecimal.ZERO
                : progression.get(0).totalPayment().subtract(progression.get(3).totalPayment());
        BigDecimal commitments = progression.isEmpty() ? fixed : fixed.add(progression.get(0).totalPayment());
        Double safety = savings == null || commitments.signum() <= 0 ? null
                : savings / commitments.doubleValue();
        return new InsightsView(months, debtFree, last, reliefMonth, relief, debtLimitPercent, commitments, safety);
    }

    private static List<Card> cards(MonthProgressionDto m) {
        BigDecimal top = m.cards().stream().map(CardProgressionDto::cardPayment)
                .max(Comparator.naturalOrder()).orElse(BigDecimal.ZERO);
        return m.cards().stream()
                .sorted(Comparator.comparing(CardProgressionDto::cardPayment).reversed())
                .map(c -> new Card(c.accountCode(), c.accountName(), c.cardPayment(),
                        c.activeDebtCount() + c.closingDebts().size(),
                        c.upcomingPayment().signum() > 0, Math.max(2, percent(c.cardPayment(), top)),
                        !c.accountCode().startsWith("__")))
                .toList();
    }

    /** Debts whose last payment falls in month {@code index} or the following window. */
    private static List<Finishing> finishing(List<MonthProgressionDto> progression, int index) {
        List<Finishing> rows = new ArrayList<>();
        for (int k = index; k < Math.min(progression.size(), index + FINISHING_WINDOW); k++) {
            MonthProgressionDto m = progression.get(k);
            for (CardProgressionDto card : m.cards()) {
                for (Debt d : card.closingDebts()) {
                    int term = d.getMaxFinancingTerm();
                    // installments paid once the selected month's payment is made
                    int paid = Math.min(term, d.getCurrentInstallment() + index + 1);
                    double ratio = term == 0 ? 1 : (double) paid / term;
                    rows.add(new Finishing(d.getDescription(), card.accountName(), d.getMonthlyPayment(),
                            m.month(), paid, term, String.format(java.util.Locale.ROOT, "%.1f 88", ratio * 87.96)));
                }
            }
        }
        return rows;
    }

    private static int percent(BigDecimal part, BigDecimal whole) {
        if (whole == null || whole.signum() <= 0 || part == null) return 0;
        return part.multiply(BigDecimal.valueOf(100)).divide(whole, 0, RoundingMode.HALF_UP).intValue();
    }
}
