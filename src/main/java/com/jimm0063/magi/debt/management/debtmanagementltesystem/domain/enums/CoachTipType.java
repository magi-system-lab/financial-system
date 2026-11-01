package com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.enums;

/**
 * Every rule the coach knows. The arguments each one carries are listed next to it
 * (money as BigDecimal, months as YearMonth, the rest as numbers or text).
 */
public enum CoachTipType {
    /** (none) */
    NO_SALARY,
    /** (amount short next month) */
    OVERSPENT,
    /** (share of salary %, limit %) */
    OVER_LIMIT,
    /** (months covered, money missing to 3 months) */
    SAFETY_LOW,
    /** (debt, yearly rate %) */
    INTEREST_FIRST,
    /** (new monthly payments this month, freed monthly payments this month) */
    NEW_OUTPACING,
    /** (goal, amount) */
    GOAL_READY_PAYOFF,
    /** (goal) */
    GOAL_REACHED,
    /** (goal, monthly amount needed) */
    GOAL_BEHIND,
    /** (goal) */
    GOAL_DEBT_GONE,
    /** (freed monthly) */
    FREED_THIS_MONTH,
    /** (first month paying less, monthly amount) */
    RELIEF_NEXT,
    /** (debt, balance, monthly) */
    QUICK_WIN,
    /** (months earlier, debt-free month) */
    SNOWBALL_ONLY,
    /** (none) */
    NO_GOALS,
    /** (purchases ready to decide) */
    WISH_READY,
    /** (purchases, monthly) */
    UPCOMING_NEXT,
    /** (none) */
    DEBT_FREE
}
