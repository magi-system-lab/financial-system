package com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.dto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.YearMonth;

/**
 * One month of the attack plan.
 *
 * @param baselinePayment debt payments without the plan
 * @param planPayment     debt payments with the plan
 * @param setAside        money put aside this month (extra + freed payments), before any payoff
 * @param saved           money waiting for the next payoff at the end of the month
 */
public record PayoffMonthDto(
        YearMonth month,
        BigDecimal baselinePayment,
        BigDecimal planPayment,
        BigDecimal setAside,
        BigDecimal saved
) implements Serializable {}
