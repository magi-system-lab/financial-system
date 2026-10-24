package com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.dto;

import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.model.Debt;

import java.util.List;

public record AccountStatementPreviewDto(
        List<Debt> newDebts,
        List<DebtInstallmentUpdateDto> installmentUpdates,
        List<Debt> completedDebts,
        List<IgnoredDebtPreviewDto> ignoredDebts,
        List<FuzzyDuplicateDto> potentialDuplicates
) {}
