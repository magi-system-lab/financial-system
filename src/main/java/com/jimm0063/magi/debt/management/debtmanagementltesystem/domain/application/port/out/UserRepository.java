package com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.application.port.out;

import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.model.DebtSysUser;

import java.util.Optional;

public interface UserRepository {
    Optional<DebtSysUser> findUserByEmail(String email);
    DebtSysUser updateFinancials(String email, Double salary, Double savings);
    /** 0 clears the limit. */
    DebtSysUser updateDebtLimit(String email, Integer debtLimitPercent);
}
