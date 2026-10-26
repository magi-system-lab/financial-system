package com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.dto;

import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.model.Debt;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

/**
 * What changed in one calendar month: purchases that appeared for the first time
 * and debts that were fully paid off.
 */
public record DebtActivityDto(
        YearMonth month,
        List<Debt> added,
        BigDecimal addedMonthly,
        List<Debt> finished,
        BigDecimal freedMonthly
) implements Serializable {
    public boolean isEmpty() {
        return added.isEmpty() && finished.isEmpty();
    }
}
