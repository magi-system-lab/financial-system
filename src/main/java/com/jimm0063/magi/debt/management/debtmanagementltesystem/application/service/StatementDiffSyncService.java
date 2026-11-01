package com.jimm0063.magi.debt.management.debtmanagementltesystem.application.service;

import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.application.port.in.SyncStatementDiffUseCase;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.application.port.out.DebtAccountRepository;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.application.port.out.DebtRepository;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.enums.SyncMode;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.exceptions.EntityNotFoundException;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.model.Debt;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.model.DebtAccount;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.model.DebtDiff;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.model.StatementDiffResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

@Service
public class StatementDiffSyncService implements SyncStatementDiffUseCase {

    private final DebtRepository debtRepository;
    private final DebtAccountRepository debtAccountRepository;

    public StatementDiffSyncService(DebtRepository debtRepository, DebtAccountRepository debtAccountRepository) {
        this.debtRepository = debtRepository;
        this.debtAccountRepository = debtAccountRepository;
    }

    @Override
    @Transactional
    public void sync(StatementDiffResult diff, SyncMode mode) {
        String code = diff.debtAccountCode();
        DebtAccount debtAccount = debtAccountRepository
                .findDebtAccountByCodeAndActiveTrue(code)
                .orElseThrow(() -> new EntityNotFoundException("DebtAccount " + code));

        if (mode == SyncMode.SOURCE_OF_TRUTH) {
            applySourceOfTruth(diff, debtAccount, code);
        } else {
            applyUpdate(diff, debtAccount);
        }
    }

    private void applyUpdate(StatementDiffResult diff, DebtAccount debtAccount) {
        // Insert new debts
        List<Debt> toInsert = diff.newDebts().stream()
                .map(DebtDiff::getExtractedDebt)
                .peek(d -> {
                    d.setDebtAccount(debtAccount);
                    d.setActive(true);
                })
                .toList();
        if (!toInsert.isEmpty()) debtRepository.saveAll(toInsert);

        // Advance installment on existing debts
        List<Debt> toUpdate = diff.updatedDebts().stream()
                .map(d -> {
                    Debt existing = d.getExistingDebt();
                    existing.setCurrentInstallment(d.getExtractedDebt().getCurrentInstallment());
                    return existing;
                })
                .toList();
        if (!toUpdate.isEmpty()) debtRepository.saveAll(toUpdate);

        // Soft-delete debts that reached their max term
        List<Debt> toDeactivate = diff.completedDebts().stream()
                .map(DebtDiff::getExistingDebt)
                .filter(Objects::nonNull)
                .peek(d -> d.setActive(false))
                .toList();
        if (!toDeactivate.isEmpty()) debtRepository.saveAll(toDeactivate);
    }

    private void applySourceOfTruth(StatementDiffResult diff, DebtAccount debtAccount, String code) {
        // Deactivate every current active debt for this account
        List<Debt> allExisting = debtRepository.findAllDebtsByDebtAccountAndActiveTrue(code);
        allExisting.forEach(d -> d.setActive(false));
        if (!allExisting.isEmpty()) debtRepository.saveAll(allExisting);

        // Insert all non-completed extracted debts as fresh records
        List<Debt> toInsert = Stream.of(diff.newDebts(), diff.updatedDebts(), diff.unchangedDebts())
                .flatMap(List::stream)
                .map(StatementDiffSyncService::withManualFields)
                .peek(d -> {
                    d.setDebtAccount(debtAccount);
                    d.setActive(true);
                    d.setId(null); // ensure new insert, not an update
                })
                .toList();
        if (!toInsert.isEmpty()) debtRepository.saveAll(toInsert);
    }

    /** Statements don't carry what the user typed by hand (the interest rate): keep it from the matching record. */
    private static Debt withManualFields(DebtDiff diff) {
        Debt extracted = diff.getExtractedDebt();
        Debt existing = diff.getExistingDebt();
        if (existing != null && extracted.getAnnualInterestRate() == null)
            extracted.setAnnualInterestRate(existing.getAnnualInterestRate());
        return extracted;
    }
}
