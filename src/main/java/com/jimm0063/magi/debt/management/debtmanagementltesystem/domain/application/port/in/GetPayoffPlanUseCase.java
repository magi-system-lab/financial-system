package com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.application.port.in;

import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.dto.PayoffPlanDto;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.enums.PayoffStrategy;

import java.math.BigDecimal;

public interface GetPayoffPlanUseCase {
    /** Attack plan over the user's active debts: extra money every month + a one-off amount today. */
    PayoffPlanDto getPlan(String email, BigDecimal extraMonthly, BigDecimal lumpSum, PayoffStrategy strategy, boolean snowball);
}
