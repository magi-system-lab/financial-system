package com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.model;

import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.enums.FixedExpenseKind;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;

@Setter
@Getter
public class FixedExpense {
    private Integer id;
    private String name;
    private BigDecimal monthlyCost;
    private Integer paymentDay;
    private String description;
    private Boolean active;
    private DebtSysUser debtSysUser;
    private FixedExpenseCatalog fixedExpenseCatalog;
    /** EXPENSE (default, also when null) or CREDIT. */
    private FixedExpenseKind kind;
    /** Optional month of the last payment (stored as its 1st day); null = keeps going. After it the item stops counting. */
    private LocalDate endDate;

    public boolean isCredit() {
        return FixedExpenseKind.orDefault(kind) == FixedExpenseKind.CREDIT;
    }

    /** Month of the last payment, null when it has no end. */
    public YearMonth lastMonth() {
        return endDate == null ? null : YearMonth.from(endDate);
    }

    /** Whether it is paid in {@code month}: always without an end month, otherwise up to and including the last one. */
    public boolean chargedIn(YearMonth month) {
        return endDate == null || !month.isAfter(lastMonth());
    }

    /** Payments left counting {@code month} itself (0 once finished); null when it has no end. */
    public Integer paymentsLeft(YearMonth month) {
        if (endDate == null) return null;
        long left = month.until(lastMonth(), java.time.temporal.ChronoUnit.MONTHS) + 1;
        return (int) Math.max(0, left);
    }

    /** Sum of the monthly cost of the items paid in {@code month}. */
    public static BigDecimal monthlyTotal(java.util.Collection<FixedExpense> items, YearMonth month) {
        return items.stream()
                .filter(e -> e.getMonthlyCost() != null && e.chargedIn(month))
                .map(FixedExpense::getMonthlyCost)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
