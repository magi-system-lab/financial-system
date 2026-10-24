package com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.utils;

import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.dto.FuzzyDuplicateDto;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.model.Debt;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class DebtComparatorUtil {
    public static boolean compareDebts(Debt debt1, Debt debt2) {
        if (Optional.ofNullable(debt1.getHashSum()).isEmpty() || Optional.ofNullable(debt2.getHashSum()).isEmpty()) return false;

        return debt1.getHashSum().equals(debt2.getHashSum());
    }

    public static List<Debt> filterAccountStatementDebts(List<Debt> debtAccountDebts, List<Debt> accountStatementDebts) {
        List<Debt> candidates = accountStatementDebts.stream()
                .filter(d -> !d.getCurrentInstallment().equals(d.getMaxFinancingTerm()))
                .collect(Collectors.toCollection(ArrayList::new));

        if (debtAccountDebts.isEmpty()) return candidates;

        for (Debt statementDebt : new ArrayList<>(candidates))
            for (Debt dbDebt : debtAccountDebts)
                if (compareDebts(statementDebt, dbDebt)
                        && statementDebt.getCurrentInstallment().equals(dbDebt.getCurrentInstallment()))
                    candidates.remove(statementDebt);

        return candidates;
    }

    public static List<FuzzyDuplicateDto> findFuzzyDuplicates(List<Debt> newDebts, List<Debt> dbDebts) {
        List<FuzzyDuplicateDto> result = new ArrayList<>();
        for (Debt incoming : newDebts) {
            String normIncoming = normalizeDescription(incoming.getDescription());
            if (normIncoming.isEmpty()) continue;
            for (Debt existing : dbDebts) {
                String normExisting = normalizeDescription(existing.getDescription());
                boolean descSimilar = normIncoming.equals(normExisting)
                        || normIncoming.contains(normExisting)
                        || normExisting.contains(normIncoming);
                boolean sameTerm = Objects.equals(incoming.getMaxFinancingTerm(), existing.getMaxFinancingTerm());
                if (descSimilar && sameTerm) {
                    result.add(new FuzzyDuplicateDto(incoming, existing));
                    break;
                }
            }
        }
        return result;
    }

    private static String normalizeDescription(String s) {
        if (s == null) return "";
        return s.toLowerCase().replaceAll("[^a-z0-9]", " ").trim().replaceAll("\\s+", " ");
    }
}
