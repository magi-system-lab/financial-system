package com.jimm0063.magi.debt.management.debtmanagementltesystem.application.service;

import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.application.port.in.MoveBetweenSectionsUseCase;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.application.port.out.DebtRepository;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.application.port.out.FixedExpenseRepository;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.application.port.out.IgnorableDebtRepository;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.enums.FixedExpenseKind;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.exceptions.EntityNotFoundException;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.model.Debt;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.model.FixedExpense;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.model.IgnorableDebt;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.infrastructure.model.FixedExpenseReq;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class MoveBetweenSectionsService implements MoveBetweenSectionsUseCase {
    private final FixedExpenseRepository fixedExpenseRepository;
    private final DebtRepository debtRepository;
    private final IgnorableDebtRepository ignorableDebtRepository;

    public MoveBetweenSectionsService(FixedExpenseRepository fixedExpenseRepository, DebtRepository debtRepository,
                                      IgnorableDebtRepository ignorableDebtRepository) {
        this.fixedExpenseRepository = fixedExpenseRepository;
        this.debtRepository = debtRepository;
        this.ignorableDebtRepository = ignorableDebtRepository;
    }

    @Override
    @Transactional
    public List<FixedExpense> moveFixedExpenses(List<Integer> ids, FixedExpenseKind to) {
        if (ids == null || ids.isEmpty()) return List.of();
        return fixedExpenseRepository.changeKind(ids, FixedExpenseKind.orDefault(to));
    }

    @Override
    @Transactional
    public FixedExpense moveDebt(String userEmail, Integer debtId, FixedExpenseKind to, Integer catalogId) {
        if (catalogId == null) throw new IllegalArgumentException("A category is required to move a debt");
        Debt debt = debtRepository.findActiveById(debtId)
                .orElseThrow(() -> new EntityNotFoundException("Debt " + debtId + " not found"));

        FixedExpenseReq req = new FixedExpenseReq();
        req.setUserEmail(userEmail);
        req.setName(debt.getDescription() == null || debt.getDescription().isBlank() ? "—" : debt.getDescription());
        req.setMonthlyCost(debt.getMonthlyPayment() == null ? 0.0 : debt.getMonthlyPayment().doubleValue());
        req.setPaymentDay(debt.getDebtAccount() != null ? debt.getDebtAccount().getPayDay() : null);
        req.setDescription(debt.getDebtAccount() != null ? debt.getDebtAccount().getName() : null);
        req.setCatalogId(catalogId);
        req.setKind(FixedExpenseKind.orDefault(to));

        FixedExpense saved = fixedExpenseRepository.save(req, catalogId);
        debtRepository.delete(debtId);
        ignoreOnFutureStatements(debt, saved);
        return saved;
    }

    /** The statement still lists the purchase: without this the next import would bring it back as a new debt. */
    private void ignoreOnFutureStatements(Debt debt, FixedExpense movedTo) {
        String hash = debt.getHashSum();
        if (hash == null || hash.isBlank()) return;
        if (!ignorableDebtRepository.findByHashSumIn(List.of(hash)).isEmpty()) return;
        IgnorableDebt ignorable = new IgnorableDebt();
        ignorable.setHashSum(hash);
        ignorable.setReason((movedTo.isCredit() ? "Moved to credits: " : "Moved to fixed expenses: ") + movedTo.getName());
        ignorable.setActive(true);
        ignorableDebtRepository.save(ignorable);
    }
}
