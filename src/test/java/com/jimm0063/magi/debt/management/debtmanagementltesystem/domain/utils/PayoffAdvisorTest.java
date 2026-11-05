package com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.utils;

import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.dto.PayoffRecommendationDto;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.dto.PayoffRecommendationDto.RecommendedDebt;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.model.Debt;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PayoffAdvisorTest {

    private static Debt debt(int id, String desc, String monthly, int paid, int term) {
        Debt d = new Debt();
        d.setId(id);
        d.setDescription(desc);
        d.setMonthlyPayment(new BigDecimal(monthly));
        d.setCurrentInstallment(paid);
        d.setMaxFinancingTerm(term);
        d.setActive(true);
        return d;
    }

    private static BigDecimal bd(String v) {
        return new BigDecimal(v);
    }

    @Test
    void picks_the_set_that_lowers_the_monthly_payment_most_not_the_greedy_one() {
        // A 300×2 = 600, B 250×2 = 500, C 240×2 = 480; next month 790 → safety net 2,370; budget 1,000
        List<Debt> debts = List.of(debt(1, "A", "300", 10, 12), debt(2, "B", "250", 4, 6), debt(3, "C", "240", 1, 3));

        PayoffRecommendationDto r = PayoffAdvisor.recommend(debts, bd("3370"), BigDecimal.ZERO);

        assertThat(r.reserve()).isEqualByComparingTo("2370");
        assertThat(r.budget()).isEqualByComparingTo("1000");
        // greedy would take A alone (−300); B + C free 490 for 980
        assertThat(r.picks()).extracting(RecommendedDebt::description).containsExactly("B", "C");
        assertThat(r.cost()).isEqualByComparingTo("980");
        assertThat(r.monthlyFreed()).isEqualByComparingTo("490");
        assertThat(r.paymentAfter()).isEqualByComparingTo("300");
        assertThat(r.dropPercent()).isEqualTo(62);
    }

    @Test
    void ranking_puts_most_relief_per_peso_first_and_skips_last_payments() {
        List<Debt> debts = List.of(debt(1, "Long", "1500", 2, 36), debt(2, "Short", "450", 9, 12),
                debt(3, "Last", "600", 11, 12), debt(4, "Mid", "890", 6, 18));

        PayoffRecommendationDto r = PayoffAdvisor.recommend(debts, bd("1000000"), bd("10000"));

        assertThat(r.ranking()).extracting(RecommendedDebt::description).containsExactly("Short", "Mid", "Long");
        assertThat(r.ranking().get(0).perThousand()).isEqualByComparingTo("333");
        // plenty of money: everything worth paying off is picked
        assertThat(r.picks()).hasSize(3);
        assertThat(r.paymentAfter()).isEqualByComparingTo("600");
    }

    @Test
    void nothing_beyond_the_safety_net_shows_what_all_savings_would_do() {
        List<Debt> debts = List.of(debt(1, "Phone", "100", 9, 12), debt(2, "Laptop", "500", 2, 12));

        PayoffRecommendationDto r = PayoffAdvisor.recommend(debts, bd("2000"), bd("3000"));

        assertThat(r.budget()).isEqualByComparingTo("0");
        assertThat(r.hasPicks()).isFalse();
        assertThat(r.allSavingsPicks()).extracting(RecommendedDebt::description).containsExactly("Phone");
        assertThat(r.allSavingsFreed()).isEqualByComparingTo("100");
        assertThat(r.hasCandidates()).isTrue();
    }

    @Test
    void large_budgets_stay_within_the_budget() {
        List<Debt> debts = List.of(debt(1, "A", "12345.67", 0, 10), debt(2, "B", "9876.54", 0, 12),
                debt(3, "C", "5432.10", 0, 6), debt(4, "D", "777.77", 0, 3));
        BigDecimal savings = bd("400000");

        PayoffRecommendationDto r = PayoffAdvisor.recommend(debts, savings, BigDecimal.ZERO);

        assertThat(r.cost()).isLessThanOrEqualTo(r.budget());
        assertThat(r.hasPicks()).isTrue();
    }
}
