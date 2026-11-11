package com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.application.port.out;

import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.enums.FixedExpenseKind;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.model.DebtSysUser;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.model.FixedExpense;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.infrastructure.model.FixedExpenseReq;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface FixedExpenseRepository {
    List<FixedExpense> findAllFixedExpenseBySystemUserAndActiveTrue(DebtSysUser systemUser);
    List<FixedExpense> findAllFixedExpenseByEmailAndActiveTrue(String email);
    Optional<FixedExpense> findByIdAndActiveTrue(Integer fixedExpenseId);
    FixedExpense save(FixedExpenseReq fixedExpense, Integer fixedExpenseCatalogId);
    FixedExpense update(FixedExpense fixedExpense);
    FixedExpense updateFromReq(FixedExpenseReq req);
    List<FixedExpense> updateMultiple(List<FixedExpenseReq> reqs);
    List<FixedExpense> bulkUpdateCategoryAndCost(List<Integer> ids, Integer catalogId, BigDecimal monthlyCost);
    /** Moves the items to the other section (fixed expenses ↔ credits); everything else stays the same. */
    List<FixedExpense> changeKind(List<Integer> ids, FixedExpenseKind kind);
    void delete(Integer fixedExpenseId);
    void deleteMultiple(List<Integer> ids);
}
