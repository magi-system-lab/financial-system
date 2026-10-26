package com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.application.port.in;

import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.dto.DebtActivityDto;

import java.util.List;

public interface GetDebtActivityUseCase {
    /** One entry per month, oldest first, ending with the current month. */
    List<DebtActivityDto> getActivity(String email, int months);
}
