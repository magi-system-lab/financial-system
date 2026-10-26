package com.jimm0063.magi.debt.management.debtmanagementltesystem.infrastructure.web.view;

import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.model.Debt;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static com.jimm0063.magi.debt.management.debtmanagementltesystem.infrastructure.web.view.InsightsFixtures.NOV_2026;
import static com.jimm0063.magi.debt.management.debtmanagementltesystem.infrastructure.web.view.InsightsFixtures.progression;
import static org.assertj.core.api.Assertions.assertThat;

class InsightsViewTest {

    @Test
    void months_are_cut_to_the_visible_range() {
        InsightsView view = InsightsView.from(progression(), 3, null, null);
        assertThat(view.months()).hasSize(3);
        assertThat(view.months().get(0).month()).isEqualTo(NOV_2026);
    }

    @Test
    void first_month_compares_with_the_next_one_and_the_rest_with_the_previous() {
        InsightsView view = InsightsView.from(progression(), 6, null, null);
        var nov = view.months().get(0);
        assertThat(nov.changeVsNext()).isTrue();
        assertThat(nov.change()).isEqualByComparingTo("-200");
        assertThat(nov.paysLess()).isTrue();
        var jan = view.months().get(2);
        assertThat(jan.changeVsNext()).isFalse();
        assertThat(jan.unchanged()).isTrue();
        assertThat(view.months().get(3).compareMonth()).isEqualTo(NOV_2026.plusMonths(2));
    }

    @Test
    void bars_are_relative_to_the_tallest_month_and_split_upcoming() {
        InsightsView view = InsightsView.from(progression(), 6, null, null);
        assertThat(view.months().get(0).barPercent()).isEqualTo(100);
        assertThat(view.months().get(1).barPercent()).isEqualTo(80);
        assertThat(view.months().get(1).upcomingPercent()).isEqualTo(25);
        assertThat(view.months().get(4).barPercent()).isZero();
    }

    @Test
    void debt_free_is_the_month_after_the_last_payment() {
        InsightsView view = InsightsView.from(progression(), 6, null, null);
        assertThat(view.lastPayment()).isEqualTo(NOV_2026.plusMonths(3));
        assertThat(view.debtFree()).isEqualTo(NOV_2026.plusMonths(4));
        assertThat(view.hasDebts()).isTrue();
    }

    @Test
    void nothing_owed_has_no_debt_free_date() {
        InsightsView view = InsightsView.from(progression().subList(4, 6), 12, null, null);
        assertThat(view.hasDebts()).isFalse();
        assertThat(view.debtFree()).isNull();
    }

    @Test
    void relief_compares_the_first_month_with_three_months_later() {
        InsightsView view = InsightsView.from(progression(), 6, null, null);
        assertThat(view.reliefMonth()).isEqualTo(NOV_2026.plusMonths(3));
        assertThat(view.relief()).isEqualByComparingTo("500");
    }

    @Test
    void left_from_salary_subtracts_fixed_expenses_and_the_month_total() {
        InsightsView view = InsightsView.from(progression(), 6, 5000.0, 1500.0);
        assertThat(view.months().get(0).leftFromSalary()).isEqualByComparingTo("2500");
        assertThat(InsightsView.from(progression(), 6, null, 1500.0).months().get(0).leftFromSalary()).isNull();
    }

    @Test
    void finishing_soon_covers_the_selected_month_and_the_next_two() {
        InsightsView view = InsightsView.from(progression(), 6, null, null);
        List<InsightsView.Finishing> nov = view.months().get(0).finishing();
        assertThat(nov).hasSize(1);
        assertThat(nov.get(0).description()).isEqualTo("Phone");
        assertThat(nov.get(0).lastPayment()).isEqualTo(NOV_2026.plusMonths(1));
        assertThat(nov.get(0).paidAtMonth()).isEqualTo(11);   // 10 paid + November's installment
        assertThat(view.months().get(1).finishing().get(0).paidAtMonth()).isEqualTo(12);
        assertThat(view.months().get(2).finishing()).isEmpty();
    }

    @Test
    void cards_count_closing_debts_as_still_paying_that_month() {
        InsightsView view = InsightsView.from(progression(), 6, null, null);
        var dec = view.months().get(1);
        assertThat(dec.cards().get(0).activeDebts()).isEqualTo(3);
        assertThat(dec.cards().get(0).hasUpcoming()).isTrue();
        assertThat(dec.activeDebts()).isEqualTo(3);
        assertThat(dec.upcoming()).isEqualByComparingTo(BigDecimal.valueOf(200));
    }

    @Test
    void payoff_options_skip_last_payments_and_sort_by_balance() {
        Debt last = InsightsFixtures.phone();      // 1 payment left: paying it early changes nothing
        last.setCurrentInstallment(11);
        last.setMaxFinancingTerm(12);
        Debt big = InsightsFixtures.phone();
        big.setDescription("Laptop");
        big.setMonthlyPayment(new BigDecimal("1000"));
        big.setCurrentInstallment(6);
        big.setMaxFinancingTerm(12);
        Debt small = InsightsFixtures.phone();
        small.setDescription("Shoes");
        small.setCurrentInstallment(1);
        small.setMaxFinancingTerm(3);

        var options = InsightsView.payoffOptions(List.of(last, big, small));

        assertThat(options).extracting(InsightsView.PayoffOption::description).containsExactly("Shoes", "Laptop");
        assertThat(options.get(0).monthsLeft()).isEqualTo(2);
        assertThat(options.get(0).balance()).isEqualByComparingTo("400");
        assertThat(options.get(1).balance()).isEqualByComparingTo("6000");
    }
}
