package com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.dto;

import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.model.Debt;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.model.UpcomingDebt;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

public record MonthProgressionDto(
        YearMonth month,
        String displayName,
        BigDecimal totalPayment,
        List<CardProgressionDto> cards
) {
    public boolean hasClosingDebts() {
        return cards.stream().anyMatch(c -> !c.closingDebts().isEmpty());
    }

    public List<Debt> allClosingDebts() {
        return cards.stream()
                .flatMap(c -> c.closingDebts().stream())
                .toList();
    }

    public int totalActiveDebtCount() {
        return cards.stream().mapToInt(CardProgressionDto::activeDebtCount).sum();
    }

    public BigDecimal totalRemainingAmount() {
        return cards.stream()
                .map(CardProgressionDto::remainingAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** Part of totalPayment that comes from upcoming (deferred) purchases. */
    public BigDecimal totalUpcomingPayment() {
        return cards.stream()
                .map(CardProgressionDto::upcomingPayment)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** Upcoming purchases whose first installment is charged this month. */
    public List<UpcomingDebt> startingUpcomingDebts() {
        return cards.stream()
                .flatMap(c -> c.upcomingDebts().stream())
                .filter(u -> month.equals(u.firstPaymentMonth()))
                .toList();
    }

    public boolean hasStartingUpcomingDebts() {
        return !startingUpcomingDebts().isEmpty();
    }
}
