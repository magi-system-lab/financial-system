package com.jimm0063.magi.debt.management.debtmanagementltesystem.application.service;

import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.application.port.out.DebtRepository;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.application.port.out.SavingsGoalContributionRepository;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.application.port.out.SavingsGoalRepository;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.dto.SavingsGoalStatusDto;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.enums.SavingsGoalStatus;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.model.Debt;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.model.SavingsGoal;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.model.SavingsGoalContribution;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SavingsGoalServiceTest {

    private static final String EMAIL = "user@test.com";

    @Mock SavingsGoalRepository savingsGoalRepository;
    @Mock SavingsGoalContributionRepository contributionRepository;
    @Mock DebtRepository debtRepository;

    @InjectMocks SavingsGoalService service;

    private static Debt debt(int id, String hash, String monthly, int paid, int term) {
        Debt d = new Debt();
        d.setId(id);
        d.setHashSum(hash);
        d.setDescription("TV");
        d.setMonthlyPayment(new BigDecimal(monthly));
        d.setCurrentInstallment(paid);
        d.setMaxFinancingTerm(term);
        d.setActive(true);
        return d;
    }

    private static SavingsGoal goal(Integer debtId, String hash, String amount) {
        SavingsGoal g = new SavingsGoal();
        g.setId(1);
        g.setName("Goal");
        g.setDebtId(debtId);
        g.setDebtHash(hash);
        g.setTargetAmount(amount == null ? null : new BigDecimal(amount));
        g.setStatus(SavingsGoalStatus.ACTIVE);
        g.setActive(true);
        return g;
    }

    private static SavingsGoalContribution money(String amount) {
        SavingsGoalContribution c = new SavingsGoalContribution();
        c.setAmount(new BigDecimal(amount));
        c.setContributionDate(LocalDate.of(2026, 3, 1));
        return c;
    }

    @Test
    void free_goal_progress_pace_and_behind() {
        SavingsGoal g = goal(null, null, "1000");
        g.setTargetDate(LocalDate.of(2026, 12, 31));
        g.setCreatedAt(LocalDate.of(2026, 1, 1).atStartOfDay(ZoneId.systemDefault()).toInstant());

        SavingsGoalStatusDto s = SavingsGoalService.status(g, List.of(money("150"), money("50")), List.of(),
                LocalDate.of(2026, 7, 1));

        assertThat(s.saved()).isEqualByComparingTo("200");
        assertThat(s.remaining()).isEqualByComparingTo("800");
        assertThat(s.percent()).isEqualTo(20);
        assertThat(s.monthsLeft()).isEqualTo(5);
        assertThat(s.suggestedMonthly()).isEqualByComparingTo("160");
        // half the time gone (181/364 days) → ~497 expected, 200 saved
        assertThat(s.behind()).isTrue();
        assertThat(s.behindBy()).isEqualByComparingTo("297");
        assertThat(s.canPayOffDebt()).isFalse();
    }

    @Test
    void linked_goal_follows_the_debt_and_finds_it_by_hash_after_a_rebuild() {
        SavingsGoalStatusDto s = SavingsGoalService.status(goal(5, "h1", "900"), List.of(money("300")),
                List.of(debt(9, "h1", "100", 9, 12)), LocalDate.of(2026, 7, 1));

        assertThat(s.debt().getId()).isEqualTo(9);
        assertThat(s.target()).isEqualByComparingTo("300");
        assertThat(s.reached()).isTrue();
        assertThat(s.canPayOffDebt()).isTrue();
        assertThat(s.monthsLeft()).isNull();
    }

    @Test
    void linked_debt_gone_falls_back_to_the_stored_amount() {
        SavingsGoalStatusDto s = SavingsGoalService.status(goal(5, "h1", "900"), List.of(), List.of(),
                LocalDate.of(2026, 7, 1));

        assertThat(s.debtGone()).isTrue();
        assertThat(s.debt()).isNull();
        assertThat(s.target()).isEqualByComparingTo("900");
    }

    @Test
    void create_linked_to_a_debt_uses_its_balance_and_name() {
        when(debtRepository.findAllDebtsByUser(EMAIL)).thenReturn(List.of(debt(5, "h1", "250", 8, 12)));
        when(savingsGoalRepository.save(any(), eq(EMAIL))).thenAnswer(i -> i.getArgument(0));

        SavingsGoal g = service.create(EMAIL, " ", null, LocalDate.of(2027, 3, 31), 5);

        assertThat(g.getName()).isEqualTo("TV");
        assertThat(g.getTargetAmount()).isEqualByComparingTo("1000");
        assertThat(g.getDebtHash()).isEqualTo("h1");
        assertThat(g.getStatus()).isEqualTo(SavingsGoalStatus.ACTIVE);
    }

    @Test
    void create_free_goal_needs_name_and_amount() {
        assertThatThrownBy(() -> service.create(EMAIL, "Trip", null, null, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.create(EMAIL, "", new BigDecimal("100"), null, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void cannot_take_out_more_than_saved() {
        when(savingsGoalRepository.findById(1)).thenReturn(Optional.of(goal(null, null, "1000")));
        when(contributionRepository.findAllByGoal(1)).thenReturn(List.of(money("100")));

        assertThatThrownBy(() -> service.contribute(1, new BigDecimal("-150"), null, null))
                .isInstanceOf(IllegalArgumentException.class);
        verify(contributionRepository, never()).save(any(), any());
    }

    @Test
    void pay_off_closes_the_debt_uses_the_money_and_completes_the_goal() {
        Debt tv = debt(5, "h1", "100", 9, 12);
        when(savingsGoalRepository.findById(1)).thenReturn(Optional.of(goal(5, "h1", "300")));
        when(contributionRepository.findAllByGoal(1)).thenReturn(List.of(money("500")));
        when(debtRepository.findAllDebtsByUser(EMAIL)).thenReturn(List.of(tv));

        service.payOffDebt(EMAIL, 1);

        ArgumentCaptor<Debt> paid = ArgumentCaptor.forClass(Debt.class);
        verify(debtRepository).update(paid.capture());
        assertThat(paid.getValue().getCurrentInstallment()).isEqualTo(12);
        assertThat(paid.getValue().getActive()).isFalse();
        ArgumentCaptor<SavingsGoalContribution> used = ArgumentCaptor.forClass(SavingsGoalContribution.class);
        verify(contributionRepository).save(used.capture(), eq(1));
        assertThat(used.getValue().getAmount()).isEqualByComparingTo("-300");
        ArgumentCaptor<SavingsGoal> updated = ArgumentCaptor.forClass(SavingsGoal.class);
        verify(savingsGoalRepository).update(updated.capture());
        assertThat(updated.getValue().getStatus()).isEqualTo(SavingsGoalStatus.COMPLETED);
    }

    @Test
    void pay_off_needs_enough_money() {
        when(savingsGoalRepository.findById(1)).thenReturn(Optional.of(goal(5, "h1", "300")));
        when(contributionRepository.findAllByGoal(1)).thenReturn(List.of(money("100")));
        when(debtRepository.findAllDebtsByUser(EMAIL)).thenReturn(List.of(debt(5, "h1", "100", 9, 12)));

        assertThatThrownBy(() -> service.payOffDebt(EMAIL, 1)).isInstanceOf(IllegalArgumentException.class);
        verify(debtRepository, never()).update(any());
    }
}
