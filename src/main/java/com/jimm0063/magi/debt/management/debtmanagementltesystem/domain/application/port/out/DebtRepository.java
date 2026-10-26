package com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.application.port.out;

import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.model.Debt;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.model.DebtAccount;

import java.util.List;

public interface DebtRepository {
    List<Debt> findAllDebtsByDebtAccountAndActiveTrue(String debtAccountCode);
    List<Debt> findAllDebtsByUser(String email);
    /** Every debt of the user, active or not (finished / deleted ones included). */
    List<Debt> findDebtHistoryByUser(String email);
    List<Debt> saveAll(List<Debt> debt, DebtAccount debtAccount);
    List<Debt> saveAll(List<Debt> debt);
    Debt save(Debt debt, String debtAccountCode);
    Debt update(Debt debt);
    void delete(Integer debtId);
}
