package com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.dto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.YearMonth;

/**
 * One payoff in the attack plan: in {@code month} the money set aside covers the whole balance of a debt.
 *
 * @param month          when it is paid off (the current month for the one-off amount)
 * @param amount         money needed to pay it off that month
 * @param monthlyFreed   its monthly payment, free from the next month on
 * @param monthsEarly    payments that are no longer made
 * @param lastPayment    month of its last payment without the plan
 * @param interestSaved  interest that is no longer paid (0 for interest-free installments)
 */
public record PayoffStepDto(
        YearMonth month,
        Integer debtId,
        String debtHash,
        String description,
        String accountName,
        BigDecimal amount,
        BigDecimal monthlyFreed,
        int monthsEarly,
        YearMonth lastPayment,
        BigDecimal interestSaved
) implements Serializable {}
