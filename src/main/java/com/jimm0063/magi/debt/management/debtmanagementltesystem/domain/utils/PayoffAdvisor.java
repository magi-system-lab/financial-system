package com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.utils;

import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.dto.PayoffRecommendationDto;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.dto.PayoffRecommendationDto.RecommendedDebt;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.model.Debt;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * "With what you have now, I'd pay off these": picks the debts that lower next month's payment the most with the
 * savings above the safety net. Every debt is treated as interest-free installments, so paying one off saves no money,
 * it only frees its monthly payment; the debt-free date is not a goal here.
 * <p>
 * The picks solve a 0/1 knapsack (maximise monthly payment freed, cost ≤ budget; ties → the cheaper set), so a
 * single bigger payment can win over several small ones when it frees more per month. Debts with a single payment
 * left are never picked (paying it early changes nothing).
 */
public final class PayoffAdvisor {

    /** Safety net kept aside, in months of fixed expenses + debt payments (same goal as the Insights tile). */
    public static final int SAFETY_MONTHS = 3;
    /** The knapsack works in steps of budget / RESOLUTION pesos (costs rounded up, so the budget is never exceeded). */
    static final int RESOLUTION = 20_000;

    private PayoffAdvisor() {}

    /** Interest-free cost to pay a debt off today: monthly × payments left. */
    public static BigDecimal cost(Debt d) {
        return d.getMonthlyPayment().multiply(BigDecimal.valueOf(PayoffSimulator.monthsLeft(d))).setScale(2, RoundingMode.HALF_UP);
    }

    public static PayoffRecommendationDto recommend(List<Debt> debts, BigDecimal savings, BigDecimal fixedExpenses) {
        BigDecimal saved = savings == null ? BigDecimal.ZERO : savings.max(BigDecimal.ZERO);
        BigDecimal fixed = fixedExpenses == null ? BigDecimal.ZERO : fixedExpenses;
        BigDecimal paymentNow = debts.stream()
                .filter(d -> d.getMonthlyPayment() != null && PayoffSimulator.monthsLeft(d) >= 1)
                .map(Debt::getMonthlyPayment).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal reserve = fixed.add(paymentNow).multiply(BigDecimal.valueOf(SAFETY_MONTHS));
        BigDecimal budget = saved.subtract(reserve).max(BigDecimal.ZERO);

        List<RecommendedDebt> ranking = debts.stream()
                .filter(PayoffSimulator::worthPayingOff)
                .map(d -> new RecommendedDebt(d.getId(), d.getDescription(),
                        d.getDebtAccount() != null ? d.getDebtAccount().getName() : "",
                        d.getMonthlyPayment(), PayoffSimulator.monthsLeft(d), cost(d),
                        BigDecimal.valueOf(1000).divide(BigDecimal.valueOf(PayoffSimulator.monthsLeft(d)), 0, RoundingMode.HALF_UP)))
                .sorted(Comparator.comparingInt(RecommendedDebt::monthsLeft)
                        .thenComparing(RecommendedDebt::monthly, Comparator.reverseOrder()))
                .toList();

        List<RecommendedDebt> picks = best(ranking, budget);
        List<RecommendedDebt> allSavings = picks.isEmpty() && saved.signum() > 0 ? best(ranking, saved) : List.of();
        return new PayoffRecommendationDto(saved, reserve, budget, picks, sum(picks, RecommendedDebt::cost),
                sum(picks, RecommendedDebt::monthly), paymentNow, allSavings, sum(allSavings, RecommendedDebt::monthly), ranking);
    }

    /** Knapsack over the candidates; returns the chosen ones in ranking order. */
    static List<RecommendedDebt> best(List<RecommendedDebt> ranking, BigDecimal budget) {
        List<RecommendedDebt> fits = ranking.stream().filter(r -> r.cost().compareTo(budget) <= 0).toList();
        if (fits.isEmpty()) return List.of();
        if (sum(fits, RecommendedDebt::cost).compareTo(budget) <= 0) return fits;   // everything fits

        BigDecimal unit = budget.divide(BigDecimal.valueOf(RESOLUTION), 2, RoundingMode.UP).max(BigDecimal.ONE);
        int capacity = budget.divide(unit, 0, RoundingMode.DOWN).intValue();
        int n = fits.size();
        int[] weight = new int[n];
        long[] value = new long[n];
        for (int i = 0; i < n; i++) {
            weight[i] = fits.get(i).cost().divide(unit, 0, RoundingMode.UP).intValue();
            value[i] = fits.get(i).monthly().movePointRight(2).longValue();
        }
        // dp[i][w] = best monthly freed with the first i candidates and w units
        long[][] dp = new long[n + 1][capacity + 1];
        for (int i = 1; i <= n; i++) {
            for (int w = 0; w <= capacity; w++) {
                dp[i][w] = dp[i - 1][w];
                if (weight[i - 1] <= w) dp[i][w] = Math.max(dp[i][w], dp[i - 1][w - weight[i - 1]] + value[i - 1]);
            }
        }
        // cheapest capacity reaching the best value
        int w = 0;
        while (dp[n][w] < dp[n][capacity]) w++;
        List<RecommendedDebt> chosen = new ArrayList<>();
        for (int i = n; i >= 1; i--) {
            if (dp[i][w] != dp[i - 1][w]) {
                chosen.add(fits.get(i - 1));
                w -= weight[i - 1];
            }
        }
        return ranking.stream().filter(chosen::contains).toList();
    }

    private static BigDecimal sum(List<RecommendedDebt> list, java.util.function.Function<RecommendedDebt, BigDecimal> f) {
        return list.stream().map(f).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
