package com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.enums;

/**
 * Order in which the coach's attack plan pays debts off. Debts that charge interest always go first
 * (highest rate first): paying them early is the only case that saves real money.
 * <ul>
 *   <li>FLOW: most monthly payment freed per peso (fewest months left; interest-free installments).</li>
 *   <li>SMALLEST: smallest balance first ("snowball": quick wins, fewer debts sooner).</li>
 *   <li>LONGEST: the debts that end last first (brings the debt-free date closer).</li>
 * </ul>
 */
public enum PayoffStrategy {
    FLOW, SMALLEST, LONGEST
}
