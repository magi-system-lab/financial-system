package com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.model;

import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.enums.DebtTypeEnum;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.enums.UpcomingDebtStatus;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

/**
 * A purchase already made whose installments start in a future month
 * (e.g. "buy in October, first payment in January, 12 MSI").
 * Kept apart from {@link Debt} on purpose: it is a planning reminder only and never
 * touches the debt table — the real debt arrives through the statement import as usual.
 */
@Setter
@Getter
public class UpcomingDebt implements Serializable {
    private static final DateTimeFormatter MONTH_LABEL = DateTimeFormatter.ofPattern("MMM yyyy", Locale.ENGLISH);

    private Integer id;
    private String description;
    private BigDecimal totalAmount;
    private BigDecimal monthlyPayment;
    private Integer installments;
    private LocalDate purchaseDate;
    /** Always the first day of the month in which the first installment is charged. */
    private LocalDate firstPaymentDate;
    private DebtTypeEnum debtType;
    private UpcomingDebtStatus status;
    private Instant startedAt;
    private Instant createdAt;
    private Instant updatedAt;
    private Boolean active;
    /** Card the purchase was made with. Optional. */
    private DebtAccount debtAccount;
    private DebtSysUser debtSysUser;

    public YearMonth firstPaymentMonth() {
        return firstPaymentDate == null ? null : YearMonth.from(firstPaymentDate);
    }

    public YearMonth lastPaymentMonth() {
        YearMonth first = firstPaymentMonth();
        if (first == null || installments == null || installments < 1) return first;
        return first.plusMonths(installments - 1L);
    }

    public boolean isPending() {
        return status == UpcomingDebtStatus.PENDING && !Boolean.FALSE.equals(active);
    }

    /** Whether one of its installments falls in the given month. */
    public boolean chargedIn(YearMonth month) {
        YearMonth first = firstPaymentMonth();
        YearMonth last = lastPaymentMonth();
        return first != null && !month.isBefore(first) && !month.isAfter(last);
    }

    /**
     * Its first payment month has arrived (or passed) while it is still pending —
     * it should already be on a statement, so the user should mark it as started.
     */
    public boolean isDue(YearMonth currentMonth) {
        YearMonth first = firstPaymentMonth();
        return isPending() && first != null && !first.isAfter(currentMonth);
    }

    /** Months from the given month until the first installment (0 = this month, negative = overdue). */
    public long monthsUntilFirstPayment(YearMonth currentMonth) {
        YearMonth first = firstPaymentMonth();
        return first == null ? 0 : currentMonth.until(first, ChronoUnit.MONTHS);
    }

    public String firstPaymentLabel() {
        YearMonth first = firstPaymentMonth();
        return first == null ? "" : first.format(MONTH_LABEL);
    }

    public String lastPaymentLabel() {
        YearMonth last = lastPaymentMonth();
        return last == null ? "" : last.format(MONTH_LABEL);
    }
}
