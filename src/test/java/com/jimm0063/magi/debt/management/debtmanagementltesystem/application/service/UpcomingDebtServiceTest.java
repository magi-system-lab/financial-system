package com.jimm0063.magi.debt.management.debtmanagementltesystem.application.service;

import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.application.port.in.ManageUpcomingDebtUseCase.UpcomingDebtCommand;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.application.port.out.DebtRepository;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.application.port.out.FixedExpenseRepository;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.application.port.out.UpcomingDebtRepository;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.application.port.out.UserRepository;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.dto.MonthProjectionDto;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.dto.PaymentProjectionDto;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.enums.DebtTypeEnum;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.enums.UpcomingDebtStatus;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.model.Debt;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.model.FixedExpense;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.model.UpcomingDebt;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpcomingDebtServiceTest {

    @Mock UpcomingDebtRepository upcomingDebtRepository;
    @Mock DebtRepository debtRepository;
    @Mock FixedExpenseRepository fixedExpenseRepository;
    @Mock UserRepository userRepository;

    @InjectMocks UpcomingDebtService service;

    private static final YearMonth OCT_2026 = YearMonth.of(2026, 10);

    // ── helpers ────────────────────────────────────────────────────────────────

    private Debt debt(String monthly, int current, int max) {
        Debt d = new Debt();
        d.setMonthlyPayment(new BigDecimal(monthly));
        d.setCurrentInstallment(current);
        d.setMaxFinancingTerm(max);
        d.setDebtType(DebtTypeEnum.CARD);
        d.setActive(true);
        return d;
    }

    private UpcomingDebt upcoming(String desc, String monthly, int installments, YearMonth first) {
        UpcomingDebt u = new UpcomingDebt();
        u.setDescription(desc);
        u.setMonthlyPayment(new BigDecimal(monthly));
        u.setInstallments(installments);
        u.setFirstPaymentDate(first.atDay(1));
        u.setStatus(UpcomingDebtStatus.PENDING);
        u.setActive(true);
        return u;
    }

    private FixedExpense fixed(String cost) {
        FixedExpense e = new FixedExpense();
        e.setMonthlyCost(new BigDecimal(cost));
        return e;
    }

    private UpcomingDebtCommand command(String total, String monthly, Integer installments, YearMonth first) {
        return new UpcomingDebtCommand("TV", null,
                total == null ? null : new BigDecimal(total),
                monthly == null ? null : new BigDecimal(monthly),
                installments, null, first, null);
    }

    // ── projection ─────────────────────────────────────────────────────────────

    @Test
    void projection_adds_upcoming_installments_only_in_their_months() {
        // Bought in October, first payment January 2027, 3 installments
        UpcomingDebt tv = upcoming("TV", "1000.00", 3, YearMonth.of(2027, 1));

        PaymentProjectionDto p = service.buildProjection(OCT_2026, 7, 20000.0,
                List.of(), List.of(), List.of(tv));

        assertThat(p.getMonths()).extracting(MonthProjectionDto::getUpcomingDebts)
                .containsExactly(0.0, 0.0, 0.0, 1000.0, 1000.0, 1000.0, 0.0);
        assertThat(p.getMonths().get(3).getStartingThisMonth()).containsExactly("TV");
        assertThat(p.getMonths().get(4).getStartingThisMonth()).isEmpty();
        assertThat(p.getMonths().get(3).getAvailableIncome()).isEqualTo(19000.0);
    }

    @Test
    void projection_month_zero_matches_current_commitments_and_debts_finish_like_progression_tab() {
        // 10/12 → charged this month and the next 2 months
        Debt laptop = debt("500.00", 10, 12);

        PaymentProjectionDto p = service.buildProjection(OCT_2026, 5, 10000.0,
                List.of(laptop), List.of(fixed("2000.00")), List.of());

        assertThat(p.getMonths()).extracting(MonthProjectionDto::getCurrentDebts)
                .containsExactly(500.0, 500.0, 500.0, 0.0, 0.0);
        assertThat(p.getMonths().get(3).getRelief()).isEqualTo(500.0);
        assertThat(p.getMonths().get(0).getTotalCommitments()).isEqualTo(2500.0);
        assertThat(p.getMonths().get(0).getAvailableIncome()).isEqualTo(7500.0);
    }

    @Test
    void projection_ignores_started_and_cancelled_items() {
        UpcomingDebt started = upcoming("Phone", "800.00", 6, OCT_2026);
        started.setStatus(UpcomingDebtStatus.STARTED);
        UpcomingDebt cancelled = upcoming("Sofa", "300.00", 6, OCT_2026);
        cancelled.setStatus(UpcomingDebtStatus.CANCELLED);

        PaymentProjectionDto p = service.buildProjection(OCT_2026, 3, null,
                List.of(), List.of(), List.of(started, cancelled));

        assertThat(p.getMonths()).allSatisfy(m -> assertThat(m.getUpcomingDebts()).isZero());
        assertThat(p.getMonths().get(0).getAvailableIncome()).isNull();
    }

    @Test
    void projection_reports_peak_month() {
        Debt running = debt("1000.00", 1, 12);
        UpcomingDebt tv = upcoming("TV", "700.00", 2, YearMonth.of(2027, 2));

        PaymentProjectionDto p = service.buildProjection(OCT_2026, 12, 15000.0,
                List.of(running), List.of(), List.of(tv));

        assertThat(p.getPeakMonth().getMonth()).isEqualTo(YearMonth.of(2027, 2));
        assertThat(p.getPeakCommitments()).isEqualTo(1700.0);
    }

    // ── create / amounts ───────────────────────────────────────────────────────

    @Test
    void create_derives_monthly_payment_from_total() {
        when(upcomingDebtRepository.save(any(), eq("me@x.com"), isNull())).thenAnswer(inv -> inv.getArgument(0));

        UpcomingDebt saved = service.create("me@x.com", command("12000.00", null, 12, YearMonth.of(2027, 1)));

        assertThat(saved.getMonthlyPayment()).isEqualByComparingTo("1000.00");
        assertThat(saved.getTotalAmount()).isEqualByComparingTo("12000.00");
        assertThat(saved.getStatus()).isEqualTo(UpcomingDebtStatus.PENDING);
        assertThat(saved.getDebtType()).isEqualTo(DebtTypeEnum.CARD);
        assertThat(saved.firstPaymentMonth()).isEqualTo(YearMonth.of(2027, 1));
        assertThat(saved.lastPaymentMonth()).isEqualTo(YearMonth.of(2027, 12));
    }

    @Test
    void create_derives_total_from_monthly_payment() {
        when(upcomingDebtRepository.save(any(), eq("me@x.com"), isNull())).thenAnswer(inv -> inv.getArgument(0));

        UpcomingDebt saved = service.create("me@x.com", command(null, "333.33", 3, YearMonth.of(2027, 1)));

        assertThat(saved.getTotalAmount()).isEqualByComparingTo("999.99");
    }

    @Test
    void create_rejects_missing_amounts() {
        assertThatThrownBy(() -> service.create("me@x.com", command(null, null, 12, YearMonth.of(2027, 1))))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.create("me@x.com", command("100", null, 0, YearMonth.of(2027, 1))))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.create("me@x.com", command("100", null, 3, null)))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(upcomingDebtRepository);
    }

    // ── status changes ─────────────────────────────────────────────────────────

    @Test
    void mark_as_started_takes_it_off_the_pile_without_touching_debts() {
        UpcomingDebt tv = upcoming("TV", "1000.00", 12, YearMonth.of(2027, 1));
        tv.setId(7);
        when(upcomingDebtRepository.findById(7)).thenReturn(Optional.of(tv));
        when(upcomingDebtRepository.update(any(), isNull())).thenAnswer(inv -> inv.getArgument(0));

        service.markAsStarted(7);

        ArgumentCaptor<UpcomingDebt> captor = ArgumentCaptor.forClass(UpcomingDebt.class);
        verify(upcomingDebtRepository).update(captor.capture(), isNull());
        assertThat(captor.getValue().getStatus()).isEqualTo(UpcomingDebtStatus.STARTED);
        assertThat(captor.getValue().getStartedAt()).isNotNull();
        assertThat(captor.getValue().isPending()).isFalse();
        verifyNoInteractions(debtRepository);
    }

    @Test
    void is_due_once_first_payment_month_arrives() {
        UpcomingDebt tv = upcoming("TV", "1000.00", 12, YearMonth.of(2027, 1));

        assertThat(tv.isDue(YearMonth.of(2026, 12))).isFalse();
        assertThat(tv.isDue(YearMonth.of(2027, 1))).isTrue();
        assertThat(tv.isDue(YearMonth.of(2027, 3))).isTrue();
        assertThat(tv.monthsUntilFirstPayment(OCT_2026)).isEqualTo(3);
    }
}
