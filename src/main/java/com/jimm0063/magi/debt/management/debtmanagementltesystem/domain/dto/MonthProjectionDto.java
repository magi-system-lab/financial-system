package com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.dto;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Expected monthly commitments for one future month. */
@Setter
@Getter
public class MonthProjectionDto implements Serializable {
    private YearMonth month;
    /** Installments of debts already in the system that are still running that month. */
    private double currentDebts;
    /** Installments of pending upcoming debts charged that month. */
    private double upcomingDebts;
    private double fixedExpenses;
    private double totalCommitments;
    /** salary - totalCommitments (null when no salary is set). */
    private Double availableIncome;
    /** Upcoming debts whose FIRST installment lands in this month. */
    private List<String> startingThisMonth = new ArrayList<>();
    /** Monthly amount freed this month by current debts that finished the month before. */
    private double relief;

    public String getLabel() {
        return month == null ? "" : month.format(DateTimeFormatter.ofPattern("MMM yyyy", Locale.ENGLISH));
    }
}
