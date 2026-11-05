package com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.dto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * The coach's own recommendation: with the money the user has now, which debts to pay off so next month's
 * payment drops the most. All debts are treated as interest-free installments: paying off costs
 * monthly × payments left and frees that monthly payment.
 *
 * @param savings        the user's savings
 * @param reserve        safety net kept aside (3 months of fixed expenses + debt payments)
 * @param budget         savings − reserve (what can be used without touching the safety net)
 * @param picks          the debts to pay off with {@code budget} (the most monthly payment freed)
 * @param cost           what the picks cost today
 * @param monthlyFreed   what next month's payment drops
 * @param paymentNow     debt payments next month without paying anything off
 * @param allSavingsPicks only when {@code picks} is empty: what all the savings would pay off (safety net included)
 * @param allSavingsFreed monthly payment those would free
 * @param ranking        every debt worth paying off, in recommended order (most monthly payment per peso first)
 */
public record PayoffRecommendationDto(
        BigDecimal savings,
        BigDecimal reserve,
        BigDecimal budget,
        List<RecommendedDebt> picks,
        BigDecimal cost,
        BigDecimal monthlyFreed,
        BigDecimal paymentNow,
        List<RecommendedDebt> allSavingsPicks,
        BigDecimal allSavingsFreed,
        List<RecommendedDebt> ranking
) implements Serializable {

    /**
     * @param perThousand monthly payment freed per 1,000 pesos spent (= 1000 / payments left)
     */
    public record RecommendedDebt(Integer debtId, String description, String accountName, BigDecimal monthly,
                                  int monthsLeft, BigDecimal cost, BigDecimal perThousand) implements Serializable {}

    public BigDecimal paymentAfter() { return paymentNow.subtract(monthlyFreed); }
    public boolean hasPicks() { return !picks.isEmpty(); }
    public boolean hasCandidates() { return !ranking.isEmpty(); }
    /** Percent of next month's payment that the picks remove. */
    public int dropPercent() {
        if (paymentNow.signum() <= 0) return 0;
        return monthlyFreed.multiply(BigDecimal.valueOf(100)).divide(paymentNow, 0, java.math.RoundingMode.HALF_UP).intValue();
    }
    public String debtIds() {
        return String.join(",", picks.stream().map(p -> String.valueOf(p.debtId())).toList());
    }
}
