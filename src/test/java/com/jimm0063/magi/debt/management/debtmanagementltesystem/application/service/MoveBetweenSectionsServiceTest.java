package com.jimm0063.magi.debt.management.debtmanagementltesystem.application.service;

import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.application.port.out.DebtRepository;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.application.port.out.FixedExpenseRepository;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.application.port.out.IgnorableDebtRepository;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.enums.FixedExpenseKind;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.exceptions.EntityNotFoundException;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.model.Debt;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.model.DebtAccount;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.model.FixedExpense;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.model.IgnorableDebt;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.infrastructure.model.FixedExpenseReq;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MoveBetweenSectionsServiceTest {

    @Mock FixedExpenseRepository fixedExpenseRepository;
    @Mock DebtRepository debtRepository;
    @Mock IgnorableDebtRepository ignorableDebtRepository;

    @InjectMocks MoveBetweenSectionsService service;

    private static Debt carLoan(String hash) {
        DebtAccount account = new DebtAccount();
        account.setName("BBVA Auto");
        account.setPayDay(15);
        Debt d = new Debt();
        d.setId(7);
        d.setDescription("Auto 2024");
        d.setMonthlyPayment(new BigDecimal("4200.50"));
        d.setDebtAccount(account);
        d.setHashSum(hash);
        d.setActive(true);
        return d;
    }

    private static FixedExpense saved(FixedExpenseReq req) {
        FixedExpense e = new FixedExpense();
        e.setName(req.getName());
        e.setKind(req.getKind());
        return e;
    }

    @Test
    void moves_fixed_expenses_to_the_requested_kind() {
        service.moveFixedExpenses(List.of(1, 2), FixedExpenseKind.CREDIT);
        verify(fixedExpenseRepository).changeKind(List.of(1, 2), FixedExpenseKind.CREDIT);
    }

    @Test
    void nothing_selected_does_nothing() {
        assertThat(service.moveFixedExpenses(List.of(), FixedExpenseKind.CREDIT)).isEmpty();
        verifyNoInteractions(fixedExpenseRepository);
    }

    @Test
    void a_debt_becomes_a_credit_leaves_the_card_and_is_ignored_on_future_statements() {
        when(debtRepository.findActiveById(7)).thenReturn(Optional.of(carLoan("abc")));
        when(fixedExpenseRepository.save(any(), eq(3))).thenAnswer(inv -> saved(inv.getArgument(0)));
        when(ignorableDebtRepository.findByHashSumIn(List.of("abc"))).thenReturn(List.of());

        FixedExpense result = service.moveDebt("me@test.com", 7, FixedExpenseKind.CREDIT, 3);

        ArgumentCaptor<FixedExpenseReq> req = ArgumentCaptor.forClass(FixedExpenseReq.class);
        verify(fixedExpenseRepository).save(req.capture(), eq(3));
        assertThat(req.getValue().getName()).isEqualTo("Auto 2024");
        assertThat(req.getValue().getMonthlyCost()).isEqualTo(4200.50);
        assertThat(req.getValue().getPaymentDay()).isEqualTo(15);
        assertThat(req.getValue().getDescription()).isEqualTo("BBVA Auto");
        assertThat(req.getValue().getUserEmail()).isEqualTo("me@test.com");
        assertThat(req.getValue().getKind()).isEqualTo(FixedExpenseKind.CREDIT);
        assertThat(result.isCredit()).isTrue();
        verify(debtRepository).delete(7);

        ArgumentCaptor<IgnorableDebt> ignored = ArgumentCaptor.forClass(IgnorableDebt.class);
        verify(ignorableDebtRepository).save(ignored.capture());
        assertThat(ignored.getValue().getHashSum()).isEqualTo("abc");
        assertThat(ignored.getValue().getActive()).isTrue();
    }

    @Test
    void an_already_ignored_hash_is_not_saved_twice() {
        when(debtRepository.findActiveById(7)).thenReturn(Optional.of(carLoan("abc")));
        when(fixedExpenseRepository.save(any(), eq(3))).thenAnswer(inv -> saved(inv.getArgument(0)));
        when(ignorableDebtRepository.findByHashSumIn(List.of("abc"))).thenReturn(List.of(new IgnorableDebt()));

        service.moveDebt("me@test.com", 7, FixedExpenseKind.EXPENSE, 3);

        verify(ignorableDebtRepository, never()).save(any());
    }

    @Test
    void a_missing_debt_fails_before_writing_anything() {
        when(debtRepository.findActiveById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.moveDebt("me@test.com", 99, FixedExpenseKind.CREDIT, 3))
                .isInstanceOf(EntityNotFoundException.class);
        verifyNoInteractions(fixedExpenseRepository);
        verify(debtRepository, never()).delete(any());
    }
}
