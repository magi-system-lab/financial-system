package com.jimm0063.magi.debt.management.debtmanagementltesystem.application.service;

import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.dto.AccountStatementPreviewDto;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.enums.DebtTypeEnum;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.model.Debt;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.model.DebtAccount;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.model.IgnorableDebt;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.application.port.out.DebtAccountRepository;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.application.port.out.DebtRepository;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.application.port.out.IgnorableDebtRepository;
import org.junit.jupiter.api.BeforeEach;
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
import java.util.Collection;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DebtServiceTest {

    @Mock
    DebtRepository debtRepository;

    @Mock
    DebtAccountRepository debtAccountRepository;

    @Mock
    IgnorableDebtRepository ignorableDebtRepository;

    @InjectMocks
    DebtService debtService;

    private static final String ACCOUNT_CODE = "VISA_TEST";

    // ── helpers ────────────────────────────────────────────────────────────────

    private Debt makeDebt(String desc, BigDecimal monthly, int current, int max, DebtTypeEnum type) {
        Debt d = new Debt();
        d.setDescription(desc);
        d.setMonthlyPayment(monthly);
        d.setCurrentInstallment(current);
        d.setMaxFinancingTerm(max);
        d.setDebtType(type);
        d.setActive(true);
        return d;
    }

    private Debt withHash(Debt d, String hash) {
        d.setHashSum(hash);
        return d;
    }

    // ── previewAccountStatement ────────────────────────────────────────────────

    @Test
    void preview_new_debt_when_not_in_db() {
        when(debtRepository.findAllDebtsByDebtAccountAndActiveTrue(ACCOUNT_CODE))
                .thenReturn(List.of());

        Debt incoming = withHash(makeDebt("Coffee Machine", new BigDecimal("500.00"), 1, 6, DebtTypeEnum.CARD), "hash-1");

        AccountStatementPreviewDto result = debtService.previewAccountStatement(List.of(incoming), ACCOUNT_CODE);

        assertThat(result.newDebts()).hasSize(1).extracting(Debt::getDescription).containsExactly("Coffee Machine");
        assertThat(result.installmentUpdates()).isEmpty();
        assertThat(result.completedDebts()).isEmpty();
    }

    @Test
    void preview_moves_ignorable_debt_to_ignored_with_its_reason() {
        when(debtRepository.findAllDebtsByDebtAccountAndActiveTrue(ACCOUNT_CODE)).thenReturn(List.of());
        IgnorableDebt rule = new IgnorableDebt();
        rule.setHashSum("hash-ig");
        rule.setReason("Borrowed by a relative");
        when(ignorableDebtRepository.findByHashSumIn(anyList())).thenReturn(List.of(rule));

        Debt mine = withHash(makeDebt("Coffee Machine", new BigDecimal("500.00"), 1, 6, DebtTypeEnum.CARD), "hash-1");
        Debt theirs = withHash(makeDebt("Phone", new BigDecimal("900.00"), 1, 12, DebtTypeEnum.CARD), "hash-ig");

        AccountStatementPreviewDto result = debtService.previewAccountStatement(List.of(mine, theirs), ACCOUNT_CODE);

        assertThat(result.newDebts()).extracting(Debt::getDescription).containsExactly("Coffee Machine");
        assertThat(result.ignoredDebts()).hasSize(1);
        assertThat(result.ignoredDebts().get(0).getReason()).isEqualTo("Borrowed by a relative");
    }

    @Test
    void preview_detects_installment_advance_as_update() {
        Debt dbDebt = withHash(makeDebt("Coffee Machine", new BigDecimal("500.00"), 1, 6, DebtTypeEnum.CARD), "hash-1");
        when(debtRepository.findAllDebtsByDebtAccountAndActiveTrue(ACCOUNT_CODE)).thenReturn(List.of(dbDebt));

        // Same debt but installment advanced from 1 → 2
        Debt incoming = withHash(makeDebt("Coffee Machine", new BigDecimal("500.00"), 2, 6, DebtTypeEnum.CARD), "hash-1");

        AccountStatementPreviewDto result = debtService.previewAccountStatement(List.of(incoming), ACCOUNT_CODE);

        assertThat(result.newDebts()).isEmpty();
        assertThat(result.installmentUpdates()).hasSize(1);
        assertThat(result.installmentUpdates().get(0).getPreviousInstallment()).isEqualTo(1);
        assertThat(result.installmentUpdates().get(0).getNewInstallment()).isEqualTo(2);
        assertThat(result.completedDebts()).isEmpty();
    }

    @Test
    void preview_detects_completed_debt_when_at_max_installment() {
        // DB has the debt at installment 5 of 6
        Debt dbDebt = withHash(makeDebt("Coffee Machine", new BigDecimal("500.00"), 5, 6, DebtTypeEnum.CARD), "hash-1");
        when(debtRepository.findAllDebtsByDebtAccountAndActiveTrue(ACCOUNT_CODE)).thenReturn(List.of(dbDebt));

        // Statement shows it at 6/6 — fully paid
        Debt incoming = withHash(makeDebt("Coffee Machine", new BigDecimal("500.00"), 6, 6, DebtTypeEnum.CARD), "hash-1");

        AccountStatementPreviewDto result = debtService.previewAccountStatement(List.of(incoming), ACCOUNT_CODE);

        assertThat(result.completedDebts()).hasSize(1).extracting(Debt::getDescription).containsExactly("Coffee Machine");
        assertThat(result.newDebts()).isEmpty();
    }

    @Test
    void preview_unchanged_debt_produces_no_movement() {
        // DB and statement agree on installment 2/6 → nothing to do
        Debt dbDebt = withHash(makeDebt("Laptop", new BigDecimal("200.00"), 2, 6, DebtTypeEnum.LOAN), "hash-2");
        when(debtRepository.findAllDebtsByDebtAccountAndActiveTrue(ACCOUNT_CODE)).thenReturn(List.of(dbDebt));

        Debt incoming = withHash(makeDebt("Laptop", new BigDecimal("200.00"), 2, 6, DebtTypeEnum.LOAN), "hash-2");

        AccountStatementPreviewDto result = debtService.previewAccountStatement(List.of(incoming), ACCOUNT_CODE);

        assertThat(result.newDebts()).isEmpty();
        assertThat(result.installmentUpdates()).isEmpty();
        assertThat(result.completedDebts()).isEmpty();
    }

    // ── saveUnrepeated ─────────────────────────────────────────────────────────

    @Test
    void saveUnrepeated_persists_new_debt() {
        when(debtRepository.findAllDebtsByDebtAccountAndActiveTrue(ACCOUNT_CODE)).thenReturn(List.of());

        DebtAccount account = new DebtAccount();
        account.setCode(ACCOUNT_CODE);
        when(debtAccountRepository.findDebtAccountByCodeAndActiveTrue(ACCOUNT_CODE)).thenReturn(Optional.of(account));
        when(debtRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        Debt incoming = withHash(makeDebt("New Purchase", new BigDecimal("300.00"), 1, 3, DebtTypeEnum.CARD), "hash-new");

        debtService.saveUnrepeated(List.of(incoming), ACCOUNT_CODE);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Debt>> captor = ArgumentCaptor.forClass(List.class);
        verify(debtRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).hasSize(1).extracting(Debt::getDescription).containsExactly("New Purchase");
    }

    @Test
    void saveUnrepeated_updates_installment_and_debt_type_for_existing_debt() {
        // Covers our change: debtType is now updated alongside currentInstallment
        Debt dbDebt = withHash(makeDebt("Laptop", new BigDecimal("200.00"), 1, 6, DebtTypeEnum.CARD), "hash-lap");
        dbDebt.setId(42);
        when(debtRepository.findAllDebtsByDebtAccountAndActiveTrue(ACCOUNT_CODE)).thenReturn(List.of(dbDebt));

        DebtAccount account = new DebtAccount();
        account.setCode(ACCOUNT_CODE);
        when(debtAccountRepository.findDebtAccountByCodeAndActiveTrue(ACCOUNT_CODE)).thenReturn(Optional.of(account));
        when(debtRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        // Statement has the debt at installment 2, type changed to LOAN
        Debt incoming = withHash(makeDebt("Laptop", new BigDecimal("200.00"), 2, 6, DebtTypeEnum.LOAN), "hash-lap");

        debtService.saveUnrepeated(List.of(incoming), ACCOUNT_CODE);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Debt>> captor = ArgumentCaptor.forClass(List.class);
        verify(debtRepository, times(2)).saveAll(captor.capture());

        List<Debt> allSaved = captor.getAllValues().stream().flatMap(Collection::stream).toList();
        assertThat(allSaved).hasSize(1);
        assertThat(allSaved.get(0).getCurrentInstallment()).isEqualTo(2);
        assertThat(allSaved.get(0).getDebtType()).isEqualTo(DebtTypeEnum.LOAN);
    }

    @Test
    void saveUnrepeated_deactivates_completed_debt() {
        Debt dbDebt = withHash(makeDebt("Laptop", new BigDecimal("200.00"), 5, 6, DebtTypeEnum.CARD), "hash-lap");
        dbDebt.setId(42);
        when(debtRepository.findAllDebtsByDebtAccountAndActiveTrue(ACCOUNT_CODE)).thenReturn(List.of(dbDebt));

        DebtAccount account = new DebtAccount();
        account.setCode(ACCOUNT_CODE);
        when(debtAccountRepository.findDebtAccountByCodeAndActiveTrue(ACCOUNT_CODE)).thenReturn(Optional.of(account));
        when(debtRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        // Statement shows final installment
        Debt incoming = withHash(makeDebt("Laptop", new BigDecimal("200.00"), 6, 6, DebtTypeEnum.CARD), "hash-lap");

        debtService.saveUnrepeated(List.of(incoming), ACCOUNT_CODE);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Debt>> captor = ArgumentCaptor.forClass(List.class);
        verify(debtRepository, times(2)).saveAll(captor.capture());

        List<Debt> allSaved = captor.getAllValues().stream().flatMap(Collection::stream).toList();
        assertThat(allSaved).anyMatch(d -> d.getId() == 42 && Boolean.FALSE.equals(d.getActive()));
    }

    // ── getHashSum ─────────────────────────────────────────────────────────────

    @Test
    void getHashSum_is_deterministic_and_64_chars() {
        Debt d = new Debt();
        d.setMonthlyPayment(new BigDecimal("500.00"));
        d.setMaxFinancingTerm(12);

        String h1 = debtService.getHashSum(d, ACCOUNT_CODE);
        String h2 = debtService.getHashSum(d, ACCOUNT_CODE);

        assertThat(h1).isEqualTo(h2).hasSize(64);
    }

    @Test
    void getHashSum_differs_across_accounts() {
        Debt d = new Debt();
        d.setMonthlyPayment(new BigDecimal("500.00"));
        d.setMaxFinancingTerm(12);

        assertThat(debtService.getHashSum(d, "ACC_A")).isNotEqualTo(debtService.getHashSum(d, "ACC_B"));
    }

    @Test
    void getHashSum_throws_when_monthly_payment_is_null() {
        Debt d = new Debt();
        d.setMaxFinancingTerm(12);

        assertThatThrownBy(() -> debtService.getHashSum(d, ACCOUNT_CODE))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("monthlyPayment");
    }
}
