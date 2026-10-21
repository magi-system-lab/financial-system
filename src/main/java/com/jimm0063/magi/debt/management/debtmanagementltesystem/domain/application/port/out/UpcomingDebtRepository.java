package com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.application.port.out;

import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.model.UpcomingDebt;

import java.util.List;
import java.util.Optional;

public interface UpcomingDebtRepository {
    List<UpcomingDebt> findAllActiveByUser(String email);
    List<UpcomingDebt> findAllPendingByUser(String email);
    List<UpcomingDebt> findAllPendingByDebtAccount(String debtAccountCode);
    Optional<UpcomingDebt> findById(Integer id);
    UpcomingDebt save(UpcomingDebt upcomingDebt, String userEmail, String debtAccountCode);
    UpcomingDebt update(UpcomingDebt upcomingDebt, String debtAccountCode);
    void delete(Integer upcomingDebtId);
}
