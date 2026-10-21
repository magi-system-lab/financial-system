package com.jimm0063.magi.debt.management.debtmanagementltesystem.application.service;

import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.application.port.out.ReceivablePaymentRepository;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.application.port.out.ReceivableRepository;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.dto.ReceivableHistoryDto;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.model.Receivable;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.model.ReceivablePayment;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.infrastructure.model.ReceivablePaymentReq;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReceivableServiceTest {

    @Mock
    ReceivableRepository receivableRepository;

    @Mock
    ReceivablePaymentRepository receivablePaymentRepository;

    @InjectMocks
    ReceivableService receivableService;

    private static final int RECEIVABLE_ID = 7;

    // ── helpers ────────────────────────────────────────────────────────────────

    private Receivable makeReceivable(String principal, LocalDate lentDate, boolean active) {
        Receivable r = new Receivable();
        r.setId(RECEIVABLE_ID);
        r.setDebtorName("Cousin");
        r.setPrincipalAmount(new BigDecimal(principal));
        r.setLentDate(lentDate);
        r.setActive(active);
        return r;
    }

    private ReceivablePayment makePayment(int id, String amount, LocalDate paymentDate) {
        ReceivablePayment p = new ReceivablePayment();
        p.setId(id);
        p.setAmount(new BigDecimal(amount));
        p.setPaymentDate(paymentDate);
        p.setCreatedAt(Instant.now());
        return p;
    }

    // ── registerPayment ────────────────────────────────────────────────────────

    @Test
    void registerPayment_keepsTheDateTheMoneyArrived() {
        Receivable receivable = makeReceivable("1000", LocalDate.of(2026, 1, 10), true);
        when(receivableRepository.findById(RECEIVABLE_ID)).thenReturn(Optional.of(receivable));
        when(receivablePaymentRepository.findAllByReceivableId(RECEIVABLE_ID)).thenReturn(List.of());

        ReceivablePaymentReq req = new ReceivablePaymentReq();
        req.setAmount(new BigDecimal("250"));
        req.setPaymentDate(LocalDate.of(2026, 3, 2));
        receivableService.registerPayment(RECEIVABLE_ID, req);

        ArgumentCaptor<ReceivablePayment> captor = ArgumentCaptor.forClass(ReceivablePayment.class);
        verify(receivablePaymentRepository).save(captor.capture());
        assertThat(captor.getValue().getPaymentDate()).isEqualTo(LocalDate.of(2026, 3, 2));
    }

    @Test
    void registerPayment_defaultsToTodayWhenNoDateGiven() {
        Receivable receivable = makeReceivable("1000", LocalDate.of(2026, 1, 10), true);
        when(receivableRepository.findById(RECEIVABLE_ID)).thenReturn(Optional.of(receivable));
        when(receivablePaymentRepository.findAllByReceivableId(RECEIVABLE_ID)).thenReturn(List.of());

        ReceivablePaymentReq req = new ReceivablePaymentReq();
        req.setAmount(new BigDecimal("250"));
        receivableService.registerPayment(RECEIVABLE_ID, req);

        ArgumentCaptor<ReceivablePayment> captor = ArgumentCaptor.forClass(ReceivablePayment.class);
        verify(receivablePaymentRepository).save(captor.capture());
        assertThat(captor.getValue().getPaymentDate()).isEqualTo(LocalDate.now());
    }

    // ── getHistory ─────────────────────────────────────────────────────────────

    @Test
    void getHistory_ordersByPaymentDateAndTracksRunningBalance() {
        Receivable receivable = makeReceivable("1000", LocalDate.of(2026, 1, 10), true);
        when(receivableRepository.findById(RECEIVABLE_ID)).thenReturn(Optional.of(receivable));
        // Entered out of order: the March payment was registered before the late-entered February one
        when(receivablePaymentRepository.findAllByReceivableId(RECEIVABLE_ID)).thenReturn(List.of(
                makePayment(2, "300", LocalDate.of(2026, 2, 15)),
                makePayment(1, "200", LocalDate.of(2026, 3, 5))
        ));

        ReceivableHistoryDto history = receivableService.getHistory(RECEIVABLE_ID);

        assertThat(history.getEntries()).extracting(ReceivableHistoryDto.HistoryEntry::getPaymentDate)
                .containsExactly(LocalDate.of(2026, 2, 15), LocalDate.of(2026, 3, 5));
        assertThat(history.getEntries()).extracting(ReceivableHistoryDto.HistoryEntry::getCumulativeRepaid)
                .containsExactly(new BigDecimal("300"), new BigDecimal("500"));
        assertThat(history.getEntries()).extracting(ReceivableHistoryDto.HistoryEntry::getRemainingBalance)
                .containsExactly(new BigDecimal("700"), new BigDecimal("500"));
        assertThat(history.getStatus().getPayments()).extracting(ReceivablePayment::getId).containsExactly(1, 2);
        assertThat(history.getPaymentCount()).isEqualTo(2);
        assertThat(history.getAveragePayment()).isEqualByComparingTo("250");
        assertThat(history.getFirstPaymentDate()).isEqualTo(LocalDate.of(2026, 2, 15));
        assertThat(history.getLastPaymentDate()).isEqualTo(LocalDate.of(2026, 3, 5));
    }

    @Test
    void getHistory_monthlyTotalsAreZeroFilledFromTheLentMonth() {
        Receivable receivable = makeReceivable("500", LocalDate.of(2026, 1, 10), true);
        when(receivableRepository.findById(RECEIVABLE_ID)).thenReturn(Optional.of(receivable));
        when(receivableRepository.update(any())).thenAnswer(inv -> inv.getArgument(0));
        when(receivablePaymentRepository.findAllByReceivableId(RECEIVABLE_ID)).thenReturn(List.of(
                makePayment(3, "200", LocalDate.of(2026, 4, 20)),
                makePayment(2, "100", LocalDate.of(2026, 4, 1)),
                makePayment(1, "200", LocalDate.of(2026, 2, 3))
        ));

        ReceivableHistoryDto history = receivableService.getHistory(RECEIVABLE_ID);

        // Fully repaid in April → the series stops there instead of running to today
        assertThat(history.getMonthlyTotals()).extracting(ReceivableHistoryDto.MonthlyTotal::getMonth)
                .containsExactly("2026-01", "2026-02", "2026-03", "2026-04");
        assertThat(history.getMonthlyTotals()).extracting(ReceivableHistoryDto.MonthlyTotal::getAmount)
                .usingComparatorForType(BigDecimal::compareTo, BigDecimal.class)
                .containsExactly(BigDecimal.ZERO, new BigDecimal("200"), BigDecimal.ZERO, new BigDecimal("300"));
        assertThat(history.getMonthlyTotals().getLast().getPaymentCount()).isEqualTo(2);
        assertThat(history.getStatus().getFullyPaid()).isTrue();
    }

    @Test
    void getHistory_fallsBackToCreatedAtForPaymentsWithoutADate() {
        Receivable receivable = makeReceivable("1000", LocalDate.of(2026, 1, 10), true);
        Instant createdAt = LocalDate.of(2026, 2, 20).atStartOfDay(ZoneId.systemDefault()).plusHours(12).toInstant();
        ReceivablePayment legacy = makePayment(1, "100", null);
        legacy.setCreatedAt(createdAt);
        when(receivableRepository.findById(RECEIVABLE_ID)).thenReturn(Optional.of(receivable));
        when(receivablePaymentRepository.findAllByReceivableId(RECEIVABLE_ID)).thenReturn(List.of(legacy));

        ReceivableHistoryDto history = receivableService.getHistory(RECEIVABLE_ID);

        assertThat(history.getEntries().getFirst().getPaymentDate()).isEqualTo(LocalDate.of(2026, 2, 20));
    }
}
