package com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.application.port.out;

import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.model.Debt;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.model.DebtAccount;

import java.util.List;
import java.util.Optional;

public interface DebtRepository {
    List<Debt> findAllDebtsByDebtAccountAndActiveTrue(String debtAccountCode);
    List<Debt> findAllDebtsByUser(String email);
    /** Every debt of the user, active or not (finished / deleted ones included). */
    List<Debt> findDebtHistoryByUser(String email);
    List<Debt> saveAll(List<Debt> debt, DebtAccount debtAccount);
    List<Debt> saveAll(List<Debt> debt);
    Debt save(Debt debt, String debtAccountCode);
    Debt update(Debt debt);
    Optional<Debt> findActiveById(Integer debtId);
    void delete(Integer debtId);
}
