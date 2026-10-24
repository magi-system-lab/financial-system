package com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.dto;

import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.model.Debt;

public record FuzzyDuplicateDto(Debt incomingDebt, Debt existingDebt) {}
