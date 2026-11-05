package com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.application.port.in;

import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.model.Debt;

import java.util.List;

public interface PayOffWithSavingsUseCase {
    /**
     * The user paid these debts off with their savings: each one is closed (every installment paid, inactive, so it
     * shows as finished in the monthly recap) and what they cost (monthly × payments left) is taken from the savings.
     * Ids that are not active debts of the user are ignored.
     */
    List<Debt> payOffWithSavings(String email, List<Integer> debtIds);
}
