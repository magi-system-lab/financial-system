package com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.utils;

import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.dto.DebtFreeOptionsDto;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.dto.DebtFreeOptionsDto.Option;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.dto.DebtFreeOptionsDto.Zone;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.model.Debt;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DebtFreeAdvisorTest {

    private static final YearMonth OCT_2026 = YearMonth.of(2026, 10);

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

    /** A: 100 × 2 left · B: 300 × 10 left · C: 500 × 4 left — next month 900, debt-free Sep 2027. */
    private static List<Debt> debts() {
        return List.of(debt(1, "A", "100", 10, 12), debt(2, "B", "300", 2, 12), debt(3, "C", "500", 8, 12));
    }

    @Test
    void more_money_never_shows_a_later_date_and_presets_follow_the_free_money() {
        DebtFreeOptionsDto exit = DebtFreeAdvisor.options(debts(), OCT_2026, new BigDecimal("5000"));

        assertThat(exit.baselineDebtFree()).isEqualTo(YearMonth.of(2027, 9));
        assertThat(exit.paymentNow()).isEqualByComparingTo("900");
        // 5,000 / 100 = 50 points + the zero one
        assertThat(exit.options()).hasSize(51);
        assertThat(exit.options().get(0).extra()).isEqualByComparingTo("0");
        assertThat(exit.options().get(50).extra()).isEqualByComparingTo("5000");
        for (int i = 1; i < exit.options().size(); i++) {
            assertThat(exit.options().get(i).debtFree()).isBeforeOrEqualTo(exit.options().get(i - 1).debtFree());
        }
        // keep paying 900: what frees up rolls into the next debt, already sooner than today
        assertThat(exit.options().get(0).monthsSaved()).isPositive();
        Option pick = exit.recommendedOption();
        assertThat(pick.extra()).isGreaterThanOrEqualTo(new BigDecimal("750"));   // 15% of 5,000
        assertThat(pick.monthsSaved()).isGreaterThan(exit.options().get(0).monthsSaved());
        assertThat(pick.totalMonthly()).isEqualByComparingTo(pick.extra().add(new BigDecimal("900")));
        assertThat(pick.zone()).isEqualTo(Zone.COMFORTABLE);
        assertThat(exit.options().get(exit.demanding()).extra()).isGreaterThanOrEqualTo(new BigDecimal("1750"));
        assertThat(exit.options().get(exit.limit()).zone()).isEqualTo(Zone.LIMIT);
        assertThat(pick.firstSteps()).isNotEmpty().hasSizeLessThanOrEqualTo(3);
    }

    @Test
    void without_salary_the_range_follows_the_payments_and_has_no_zones() {
        DebtFreeOptionsDto exit = DebtFreeAdvisor.options(debts(), OCT_2026, null);

        // max(900, 1,000) = 1,000 in steps of 50
        assertThat(exit.options().get(exit.options().size() - 1).extra()).isEqualByComparingTo("1000");
        assertThat(exit.options()).allMatch(o -> o.zone() == Zone.NONE);
    }

    @Test
    void no_debts_no_options() {
        DebtFreeOptionsDto exit = DebtFreeAdvisor.options(List.of(), OCT_2026, new BigDecimal("5000"));
        assertThat(exit.hasOptions()).isFalse();
        assertThat(exit.baselineDebtFree()).isNull();
    }

    @Test
    void zones_and_steps() {
        assertThat(DebtFreeAdvisor.zone(new BigDecimal("1000"), new BigDecimal("4000"))).isEqualTo(Zone.COMFORTABLE);
        assertThat(DebtFreeAdvisor.zone(new BigDecimal("2000"), new BigDecimal("4000"))).isEqualTo(Zone.DEMANDING);
        assertThat(DebtFreeAdvisor.zone(new BigDecimal("2001"), new BigDecimal("4000"))).isEqualTo(Zone.LIMIT);
        assertThat(DebtFreeAdvisor.step(new BigDecimal("13710"))).isEqualTo(500);
        assertThat(DebtFreeAdvisor.step(new BigDecimal("2000"))).isEqualTo(50);
    }
}
