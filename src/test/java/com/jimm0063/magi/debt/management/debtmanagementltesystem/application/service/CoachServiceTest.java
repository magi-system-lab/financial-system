package com.jimm0063.magi.debt.management.debtmanagementltesystem.application.service;

import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.dto.CoachTipDto;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.dto.DebtActivityDto;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.dto.SavingsGoalStatusDto;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.enums.CoachTipSeverity;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.enums.CoachTipType;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.enums.SavingsGoalStatus;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.model.Debt;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.model.SavingsGoal;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CoachServiceTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 31);

    private static Debt debt(String desc, String monthly, int paid, int term) {
        Debt d = new Debt();
        d.setId(desc.hashCode());
        d.setDescription(desc);
        d.setMonthlyPayment(new BigDecimal(monthly));
        d.setCurrentInstallment(paid);
        d.setMaxFinancingTerm(term);
        d.setActive(true);
        return d;
    }

    private static CoachService.Snapshot snapshot(Double salary, Double savings, Integer limit, List<Debt> debts,
                                                  DebtActivityDto month, List<SavingsGoalStatusDto> goals) {
        return new CoachService.Snapshot(TODAY, salary, savings, limit, new BigDecimal("3000"), debts, month, goals, 0, List.of());
    }

    private static List<CoachTipType> types(List<CoachTipDto> tips) {
        return tips.stream().map(CoachTipDto::type).toList();
    }

    @Test
    void alerts_come_first_and_the_budget_rules_fire() {
        // next month 600 (phone 100 ends in Nov), then 500; commitments 3,600; savings cover 0.2 months
        List<CoachTipDto> tips = CoachService.tips(snapshot(10000.0, 1000.0, 5,
                List.of(debt("Phone", "100", 11, 12), debt("Laptop", "500", 2, 12)), null, List.of()));

        assertThat(types(tips)).contains(CoachTipType.OVER_LIMIT, CoachTipType.SAFETY_LOW, CoachTipType.RELIEF_NEXT,
                CoachTipType.NO_GOALS);
        assertThat(tips.get(0).severity()).isEqualTo(CoachTipSeverity.ALERT);
        CoachTipDto limit = tips.stream().filter(t -> t.type() == CoachTipType.OVER_LIMIT).findFirst().orElseThrow();
        assertThat(limit.args()).containsExactly(6, 5);
        CoachTipDto relief = tips.stream().filter(t -> t.type() == CoachTipType.RELIEF_NEXT).findFirst().orElseThrow();
        assertThat(relief.args().get(0)).isEqualTo(YearMonth.of(2026, 12));
        assertThat((BigDecimal) relief.args().get(1)).isEqualByComparingTo("100");
        assertThat(types(tips)).doesNotContain(CoachTipType.QUICK_WIN, CoachTipType.OVERSPENT);
    }

    @Test
    void overspent_without_salary_room_and_no_salary_tip() {
        assertThat(types(CoachService.tips(snapshot(3200.0, 0.0, null, List.of(debt("Laptop", "500", 2, 12)), null, List.of()))))
                .contains(CoachTipType.OVERSPENT);
        assertThat(types(CoachService.tips(snapshot(null, 0.0, null, List.of(debt("Laptop", "500", 2, 12)), null, List.of()))))
                .contains(CoachTipType.NO_SALARY);
    }

    @Test
    void quick_win_only_with_money_beyond_the_safety_net() {
        // commitments 3,500 → 10,500 safety net; 12,000 saved leaves 1,500 ≥ phone's 300
        List<CoachTipDto> tips = CoachService.tips(snapshot(20000.0, 12000.0, null,
                List.of(debt("Phone", "100", 9, 12), debt("Laptop", "400", 2, 12)), null, List.of()));

        CoachTipDto win = tips.stream().filter(t -> t.type() == CoachTipType.QUICK_WIN).findFirst().orElseThrow();
        assertThat(win.args().get(0)).isEqualTo("Phone");
        assertThat((BigDecimal) win.args().get(1)).isEqualByComparingTo("300");
        assertThat(types(tips)).doesNotContain(CoachTipType.SAFETY_LOW);
    }

    @Test
    void interest_activity_and_goal_tips() {
        Debt loan = debt("Loan", "1000", 0, 24);
        loan.setAnnualInterestRate(new BigDecimal("36.50"));
        DebtActivityDto october = new DebtActivityDto(YearMonth.of(2026, 10), List.of(), new BigDecimal("900"),
                List.of(), new BigDecimal("200"));
        SavingsGoal g = new SavingsGoal();
        g.setName("TV");
        g.setStatus(SavingsGoalStatus.ACTIVE);
        g.setDebtId(1);
        Debt tv = debt("TV", "100", 9, 12);
        SavingsGoalStatusDto ready = new SavingsGoalStatusDto(g, new BigDecimal("300"), new BigDecimal("300"),
                BigDecimal.ZERO, 100, tv, false, null, null, BigDecimal.ZERO, List.of());

        List<CoachTipDto> tips = CoachService.tips(snapshot(50000.0, 100000.0, null, List.of(loan, tv), october, List.of(ready)));

        assertThat(types(tips)).contains(CoachTipType.INTEREST_FIRST, CoachTipType.NEW_OUTPACING,
                CoachTipType.GOAL_READY_PAYOFF, CoachTipType.FREED_THIS_MONTH);
        assertThat(types(tips)).doesNotContain(CoachTipType.NO_GOALS);
        CoachTipDto interest = tips.stream().filter(t -> t.type() == CoachTipType.INTEREST_FIRST).findFirst().orElseThrow();
        assertThat(interest.args()).containsExactly("Loan", "36.5");
    }

    @Test
    void debt_free() {
        List<CoachTipDto> tips = CoachService.tips(snapshot(20000.0, 50000.0, null, List.of(), null, List.of()));
        assertThat(types(tips)).contains(CoachTipType.DEBT_FREE).doesNotContain(CoachTipType.NO_GOALS, CoachTipType.SNOWBALL_ONLY);
    }
}
