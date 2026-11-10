package com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.application.port.in;

import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.dto.DebtFreeOptionsDto;

public interface GetDebtFreeOptionsUseCase {
    /** For every extra amount per month (0 … what's left from the salary): when the user would be debt-free. */
    DebtFreeOptionsDto getOptions(String email);
}
