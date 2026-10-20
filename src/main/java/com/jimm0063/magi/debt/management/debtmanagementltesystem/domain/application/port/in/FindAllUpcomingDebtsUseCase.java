package com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.application.port.in;

import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.model.UpcomingDebt;

import java.util.List;

public interface FindAllUpcomingDebtsUseCase {
    /** Every non-deleted upcoming debt of the user (pending, started and cancelled), soonest first. */
    List<UpcomingDebt> getByEmail(String email);
    List<UpcomingDebt> getPendingByEmail(String email);
    List<UpcomingDebt> getPendingByDebtAccount(String debtAccountCode);
}
