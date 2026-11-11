package com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.enums;

/**
 * What a monthly charge in {@code fixed_expense} is. Both kinds count the same in every budget (free money, safety net,
 * coach); they are only told apart in the sections and charts that show where the money goes.
 */
public enum FixedExpenseKind {
    /** Rent, utilities, subscriptions… (also every row saved before the column existed). */
    EXPENSE,
    /** A credit / loan paid as a flat monthly amount (car, mortgage, personal loan…). */
    CREDIT;

    /** Null (rows saved before the column existed) reads as EXPENSE. */
    public static FixedExpenseKind orDefault(FixedExpenseKind kind) {
        return kind == null ? EXPENSE : kind;
    }
}
