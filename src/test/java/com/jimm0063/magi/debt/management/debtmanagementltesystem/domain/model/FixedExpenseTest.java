package com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FixedExpenseTest {

    private static final YearMonth NOV = YearMonth.of(2026, 11);

    private static FixedExpense item(String cost, YearMonth last) {
        FixedExpense e = new FixedExpense();
        e.setMonthlyCost(new BigDecimal(cost));
        e.setEndDate(last == null ? null : last.atDay(1));
        return e;
    }

    @Test
    void without_end_month_it_keeps_going() {
        FixedExpense rent = item("6000", null);
        assertThat(rent.chargedIn(NOV.plusYears(10))).isTrue();
        assertThat(rent.paymentsLeft(NOV)).isNull();
    }

    @Test
    void with_end_month_it_is_charged_up_to_and_including_it() {
        FixedExpense car = item("4000", YearMonth.of(2027, 1));
        assertThat(car.chargedIn(NOV)).isTrue();
        assertThat(car.chargedIn(YearMonth.of(2027, 1))).isTrue();
        assertThat(car.chargedIn(YearMonth.of(2027, 2))).isFalse();
        assertThat(car.paymentsLeft(NOV)).isEqualTo(3);
        assertThat(car.paymentsLeft(YearMonth.of(2027, 1))).isEqualTo(1);
        assertThat(car.paymentsLeft(YearMonth.of(2027, 3))).isZero();
    }

    @Test
    void monthly_total_drops_items_after_their_last_payment() {
        List<FixedExpense> items = List.of(item("6000", null), item("4000", NOV));
        assertThat(FixedExpense.monthlyTotal(items, NOV)).isEqualByComparingTo("10000");
        assertThat(FixedExpense.monthlyTotal(items, NOV.plusMonths(1))).isEqualByComparingTo("6000");
    }
}
