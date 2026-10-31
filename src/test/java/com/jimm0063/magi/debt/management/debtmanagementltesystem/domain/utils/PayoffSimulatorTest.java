package com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.utils;

import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.dto.PayoffPlanDto;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.dto.PayoffStepDto;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.enums.PayoffStrategy;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.model.Debt;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.model.DebtAccount;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PayoffSimulatorTest {

    private static final YearMonth OCT_2026 = YearMonth.of(2026, 10);

    private static Debt debt(int id, String desc, String monthly, int paid, int term) {
        DebtAccount visa = new DebtAccount();
        visa.setName("Visa");
        Debt d = new Debt();
        d.setId(id);
        d.setDescription(desc);
        d.setMonthlyPayment(new BigDecimal(monthly));
        d.setCurrentInstallment(paid);
        d.setMaxFinancingTerm(term);
        d.setDebtAccount(visa);
        d.setActive(true);
        return d;
    }

    /** A: 100 × 2 left (200) · B: 300 × 10 left (3,000) · C: 500 × 4 left (2,000). */
    private static List<Debt> debts() {
        return List.of(debt(1, "A", "100", 10, 12), debt(2, "B", "300", 2, 12), debt(3, "C", "500", 8, 12));
    }

    private static BigDecimal bd(String v) {
        return new BigDecimal(v);
    }

    @Test
    void without_money_nothing_changes() {
        PayoffPlanDto plan = PayoffSimulator.simulate(debts(), OCT_2026, BigDecimal.ZERO, BigDecimal.ZERO, PayoffStrategy.FLOW, false);

        assertThat(plan.steps()).isEmpty();
        assertThat(plan.baselineDebtFree()).isEqualTo(YearMonth.of(2027, 9));   // last payment Aug 2027
        assertThat(plan.planDebtFree()).isEqualTo(plan.baselineDebtFree());
        assertThat(plan.monthsSaved()).isZero();
        assertThat(plan.totalBalance()).isEqualByComparingTo("5200");
        assertThat(plan.months()).hasSize(10);
        assertThat(plan.months().get(0).baselinePayment()).isEqualByComparingTo("900");
        assertThat(plan.months().get(2).baselinePayment()).isEqualByComparingTo("800");
    }

    @Test
    void lump_sum_pays_off_most_relief_first_and_snowball_rolls_the_freed_payments() {
        PayoffPlanDto plan = PayoffSimulator.simulate(debts(), OCT_2026, BigDecimal.ZERO, bd("2200"), PayoffStrategy.FLOW, true);

        // today: A (0.5 per peso) and C (0.25) fit in 2,200
        assertThat(plan.steps()).extracting(PayoffStepDto::description).containsExactly("A", "C", "B");
        assertThat(plan.steps().get(0).month()).isEqualTo(OCT_2026);
        assertThat(plan.steps().get(1).amount()).isEqualByComparingTo("2000");
        // then 600/month freed: Nov 600, Dec 1,200, Jan 1,800, Feb 2,400 ≥ B's 1,800 left after Feb's payment
        PayoffStepDto b = plan.steps().get(2);
        assertThat(b.month()).isEqualTo(YearMonth.of(2027, 2));
        assertThat(b.amount()).isEqualByComparingTo("1800");
        assertThat(b.monthsEarly()).isEqualTo(6);
        assertThat(b.lastPayment()).isEqualTo(YearMonth.of(2027, 8));
        assertThat(plan.planDebtFree()).isEqualTo(YearMonth.of(2027, 3));
        assertThat(plan.monthsSaved()).isEqualTo(6);
        assertThat(plan.interestSaved()).isEqualByComparingTo("0");
        assertThat(plan.months().get(0).planPayment()).isEqualByComparingTo("300");
        assertThat(plan.months().get(0).setAside()).isEqualByComparingTo("600");
    }

    @Test
    void finish_sooner_saves_for_the_longest_debt_with_the_extra_money() {
        PayoffPlanDto plan = PayoffSimulator.simulate(debts(), OCT_2026, bd("1000"), BigDecimal.ZERO, PayoffStrategy.LONGEST, true);

        // Nov 1,000 · Dec 2,000 (A ends) · Jan 2,000 + 1,100 = 3,100 ≥ B's 2,100 → paid off in Jan
        assertThat(plan.steps()).extracting(PayoffStepDto::description).containsExactly("B");
        assertThat(plan.steps().get(0).month()).isEqualTo(YearMonth.of(2027, 1));
        assertThat(plan.steps().get(0).amount()).isEqualByComparingTo("2100");
        // C has a single payment left in Jan: it just ends in Feb
        assertThat(plan.planDebtFree()).isEqualTo(YearMonth.of(2027, 3));
    }

    @Test
    void smallest_first_and_without_snowball_only_the_extra_is_used() {
        PayoffPlanDto plan = PayoffSimulator.simulate(debts(), OCT_2026, bd("100"), BigDecimal.ZERO, PayoffStrategy.SMALLEST, false);

        // A never qualifies (1 payment left after Nov); 100 a month never catches C before it ends;
        // B: 100·k ≥ 300·(10 − k) after k payments → k = 8 (Jun 2027), 600 left to pay
        assertThat(plan.steps()).extracting(PayoffStepDto::description).containsExactly("B");
        assertThat(plan.steps().get(0).month()).isEqualTo(YearMonth.of(2027, 6));
        assertThat(plan.steps().get(0).amount()).isEqualByComparingTo("600");
        assertThat(plan.months().get(1).setAside()).isEqualByComparingTo("100");   // freed payments are not added
    }

    @Test
    void debts_with_interest_go_first_and_save_interest() {
        Debt loan = debt(10, "Loan", "1000", 0, 12);
        loan.setAnnualInterestRate(bd("24"));
        Debt phone = debt(11, "Phone", "100", 9, 12);

        assertThat(PayoffSimulator.payoffBalance(loan)).isEqualByComparingTo("10575.34");

        PayoffPlanDto small = PayoffSimulator.simulate(List.of(loan, phone), OCT_2026, BigDecimal.ZERO, bd("300"),
                PayoffStrategy.SMALLEST, false);
        assertThat(small.steps()).isEmpty();   // saving for the loan first, even if the phone is cheaper

        PayoffPlanDto plan = PayoffSimulator.simulate(List.of(loan, phone), OCT_2026, BigDecimal.ZERO, bd("11000"),
                PayoffStrategy.SMALLEST, false);
        assertThat(plan.steps()).extracting(PayoffStepDto::description).containsExactly("Loan", "Phone");
        assertThat(plan.interestSaved()).isEqualByComparingTo("1424.66");
        assertThat(plan.planDebtFree()).isEqualTo(YearMonth.of(2026, 11));
    }

    @Test
    void no_debts_no_dates() {
        PayoffPlanDto plan = PayoffSimulator.simulate(List.of(), OCT_2026, bd("500"), bd("1000"), PayoffStrategy.FLOW, true);
        assertThat(plan.hasDebts()).isFalse();
        assertThat(plan.planDebtFree()).isNull();
        assertThat(plan.months()).isEmpty();
    }
}
