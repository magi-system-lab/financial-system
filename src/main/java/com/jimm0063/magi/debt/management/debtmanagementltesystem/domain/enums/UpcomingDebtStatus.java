package com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.enums;

/**
 * Lifecycle of a planned purchase whose first payment is in a future month.
 * PENDING → still waiting for its first charge (counted in the payment projection).
 * STARTED → it showed up on a statement / started being charged; the real debt now lives in the debt table.
 * CANCELLED → the purchase was returned or never happened.
 */
public enum UpcomingDebtStatus {
    PENDING, STARTED, CANCELLED
}
