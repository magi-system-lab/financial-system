package com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.dto;

import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.model.Debt;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.model.UpcomingDebt;

import java.math.BigDecimal;
import java.util.List;

/**
 * cardPayment / remainingAmount include the installments of upcoming (deferred) purchases
 * charged that month; upcomingPayment is that part on its own.
 */
public record CardProgressionDto(
        String accountCode,
        String accountName,
        List<Debt> closingDebts,
        int activeDebtCount,
        BigDecimal cardPayment,
        BigDecimal remainingAmount,
        List<UpcomingDebt> upcomingDebts,
        BigDecimal upcomingPayment
) {}
