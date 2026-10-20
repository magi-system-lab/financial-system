package com.jimm0063.magi.debt.management.debtmanagementltesystem.application.service;

import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.application.port.in.FindAllReceivablesUseCase;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.application.port.in.GetReceivableHistoryUseCase;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.application.port.in.GetReceivableStatusUseCase;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.application.port.in.RecordReceivablePaymentUseCase;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.application.port.out.ReceivablePaymentRepository;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.application.port.out.ReceivableRepository;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.dto.ReceivableHistoryDto;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.dto.ReceivableStatusDto;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.exceptions.EntityNotFoundException;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.model.Receivable;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.model.ReceivablePayment;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.infrastructure.model.ReceivablePaymentReq;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ReceivableService implements FindAllReceivablesUseCase, RecordReceivablePaymentUseCase, GetReceivableStatusUseCase,
        GetReceivableHistoryUseCase {
    private final ReceivableRepository receivableRepository;
    private final ReceivablePaymentRepository receivablePaymentRepository;

    public ReceivableService(ReceivableRepository receivableRepository, ReceivablePaymentRepository receivablePaymentRepository) {
        this.receivableRepository = receivableRepository;
        this.receivablePaymentRepository = receivablePaymentRepository;
    }

    @Override
    public List<Receivable> getByEmail(String email) {
        return receivableRepository.findAllByUser(email);
    }

    @Override
    @Transactional(rollbackOn = Exception.class)
    public ReceivableStatusDto registerPayment(Integer receivableId, ReceivablePaymentReq receivablePaymentReq) {
        Receivable receivable = receivableRepository.findById(receivableId)
                .orElseThrow(() -> new EntityNotFoundException("Receivable " + receivableId + " not found"));

        ReceivablePayment payment = new ReceivablePayment();
        payment.setAmount(receivablePaymentReq.getAmount());
        payment.setNote(receivablePaymentReq.getNote());
        payment.setPaymentDate(receivablePaymentReq.getPaymentDate() != null
                ? receivablePaymentReq.getPaymentDate()
                : LocalDate.now());
        payment.setReceivable(receivable);
        receivablePaymentRepository.save(payment);

        return buildStatus(receivable);
    }

    @Override
    public ReceivableStatusDto getStatus(Integer receivableId) {
        Receivable receivable = receivableRepository.findById(receivableId)
                .orElseThrow(() -> new EntityNotFoundException("Receivable " + receivableId + " not found"));
        return buildStatus(receivable);
    }

    @Override
    public List<ReceivableStatusDto> getAllStatusesByEmail(String email) {
        return receivableRepository.findAllByUser(email).stream()
                .map(this::buildStatus)
                .toList();
    }

    @Override
    public ReceivableHistoryDto getHistory(Integer receivableId) {
        ReceivableStatusDto status = getStatus(receivableId);
        Receivable receivable = status.getReceivable();

        List<ReceivablePayment> chronological = status.getPayments().reversed();

        List<ReceivableHistoryDto.HistoryEntry> entries = new ArrayList<>();
        BigDecimal cumulative = BigDecimal.ZERO;
        for (ReceivablePayment payment : chronological) {
            cumulative = cumulative.add(payment.getAmount());
            ReceivableHistoryDto.HistoryEntry entry = new ReceivableHistoryDto.HistoryEntry();
            entry.setPaymentId(payment.getId());
            entry.setPaymentDate(payment.getPaymentDate());
            entry.setAmount(payment.getAmount());
            entry.setNote(payment.getNote());
            entry.setCumulativeRepaid(cumulative);
            entry.setRemainingBalance(receivable.getPrincipalAmount().subtract(cumulative).max(BigDecimal.ZERO));
            entries.add(entry);
        }

        ReceivableHistoryDto dto = new ReceivableHistoryDto();
        dto.setStatus(status);
        dto.setEntries(entries);
        dto.setMonthlyTotals(buildMonthlyTotals(receivable, chronological, status.getFullyPaid()));
        dto.setPaymentCount(chronological.size());
        if (!chronological.isEmpty()) {
            LocalDate last = chronological.getLast().getPaymentDate();
            dto.setFirstPaymentDate(chronological.getFirst().getPaymentDate());
            dto.setLastPaymentDate(last);
            dto.setDaysSinceLastPayment(ChronoUnit.DAYS.between(last, LocalDate.now()));
            dto.setAveragePayment(status.getTotalRepaid()
                    .divide(BigDecimal.valueOf(chronological.size()), 2, RoundingMode.HALF_UP));
        } else {
            dto.setAveragePayment(BigDecimal.ZERO);
        }
        return dto;
    }

    private List<ReceivableHistoryDto.MonthlyTotal> buildMonthlyTotals(Receivable receivable, List<ReceivablePayment> chronological,
                                                                    boolean fullyPaid) {
        Map<YearMonth, ReceivableHistoryDto.MonthlyTotal> buckets = new LinkedHashMap<>();

        YearMonth start = receivable.getLentDate() != null ? YearMonth.from(receivable.getLentDate()) : null;
        if (!chronological.isEmpty()) {
            YearMonth firstPayment = YearMonth.from(chronological.getFirst().getPaymentDate());
            if (start == null || firstPayment.isBefore(start)) start = firstPayment;
        }
        if (start == null) return List.of();

        // Stop at the last payment once fully repaid; otherwise keep counting empty months up to today
        YearMonth end = YearMonth.now();
        if (!chronological.isEmpty()) {
            YearMonth lastPayment = YearMonth.from(chronological.getLast().getPaymentDate());
            if (fullyPaid || lastPayment.isAfter(end)) end = lastPayment;
        }
        if (end.isBefore(start)) end = start;

        for (YearMonth month = start; !month.isAfter(end); month = month.plusMonths(1)) {
            ReceivableHistoryDto.MonthlyTotal total = new ReceivableHistoryDto.MonthlyTotal();
            total.setMonth(month.toString());
            total.setAmount(BigDecimal.ZERO);
            total.setPaymentCount(0);
            buckets.put(month, total);
        }
        for (ReceivablePayment payment : chronological) {
            ReceivableHistoryDto.MonthlyTotal total = buckets.get(YearMonth.from(payment.getPaymentDate()));
            total.setAmount(total.getAmount().add(payment.getAmount()));
            total.setPaymentCount(total.getPaymentCount() + 1);
        }
        return new ArrayList<>(buckets.values());
    }

    private ReceivableStatusDto buildStatus(Receivable receivable) {
        List<ReceivablePayment> payments = receivablePaymentRepository.findAllByReceivableId(receivable.getId()).stream()
                .map(this::withPaymentDate)
                .sorted(Comparator.comparing(ReceivablePayment::getPaymentDate)
                        .thenComparing(ReceivablePayment::getCreatedAt, Comparator.nullsFirst(Comparator.naturalOrder()))
                        .reversed())
                .toList();

        BigDecimal totalRepaid = payments.stream()
                .map(ReceivablePayment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal pendingAmount = receivable.getPrincipalAmount().subtract(totalRepaid);
        boolean fullyPaid = pendingAmount.compareTo(BigDecimal.ZERO) <= 0;

        // Dynamically re-open/close the receivable as payments move it across the fully-paid threshold
        if (fullyPaid != !receivable.getActive()) {
            receivable.setActive(!fullyPaid);
            receivable = receivableRepository.update(receivable);
        }

        double percentPaid = receivable.getPrincipalAmount().compareTo(BigDecimal.ZERO) == 0
                ? 100.0
                : Math.min(totalRepaid.divide(receivable.getPrincipalAmount(), 4, RoundingMode.HALF_UP).doubleValue() * 100, 100.0);

        ReceivableStatusDto dto = new ReceivableStatusDto();
        dto.setReceivable(receivable);
        dto.setTotalRepaid(totalRepaid);
        dto.setPendingAmount(pendingAmount.max(BigDecimal.ZERO));
        dto.setFullyPaid(fullyPaid);
        dto.setPercentPaid(Math.round(percentPaid * 100.0) / 100.0);
        dto.setPayments(payments);
        return dto;
    }

    // Payments recorded before paymentDate existed only carry the time they were entered
    private ReceivablePayment withPaymentDate(ReceivablePayment payment) {
        if (payment.getPaymentDate() == null) {
            payment.setPaymentDate(payment.getCreatedAt() != null
                    ? payment.getCreatedAt().atZone(ZoneId.systemDefault()).toLocalDate()
                    : LocalDate.now());
        }
        return payment;
    }
}
