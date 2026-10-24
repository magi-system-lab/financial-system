package com.jimm0063.magi.debt.management.debtmanagementltesystem.application.service;

import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.application.port.in.*;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.application.port.out.DebtAccountRepository;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.application.port.out.DebtRepository;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.application.port.out.IgnorableDebtRepository;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.dto.AccountStatementPreviewDto;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.dto.DebtInstallmentUpdateDto;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.dto.FuzzyDuplicateDto;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.dto.IgnoredDebtPreviewDto;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.exceptions.EntityNotFoundException;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.model.Debt;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.model.DebtAccount;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.model.IgnorableDebt;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.utils.DebtComparatorUtil;
import org.springframework.stereotype.Service;

import java.math.BigInteger;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class DebtService implements FilterDebtsUseCase, PayOffDebtAccountUseCase, FindAllDebtsUseCase, LoadDebtList, DebtDuplicationPreventUseCase, SourceOfTruthImportUseCase {
    private final DebtRepository debtRepository;
    private final DebtAccountRepository debtAccountRepository;
    private final IgnorableDebtRepository ignorableDebtRepository;

    public DebtService(DebtRepository debtRepository, DebtAccountRepository debtAccountRepository,
                       IgnorableDebtRepository ignorableDebtRepository) {
        this.debtRepository = debtRepository;
        this.debtAccountRepository = debtAccountRepository;
        this.ignorableDebtRepository = ignorableDebtRepository;
    }

    @Override
    public AccountStatementPreviewDto previewAccountStatement(List<Debt> accountStatementDebts, String debtAccountCode) {
        List<Debt> dbDebts = this.debtRepository.findAllDebtsByDebtAccountAndActiveTrue(debtAccountCode)
                .stream()
                .peek(d -> { if (d.getHashSum() == null) d.setHashSum(this.getHashSum(d, debtAccountCode)); })
                .toList();

        Map<String, Debt> dbByHash = dbDebts.stream()
                .filter(d -> d.getHashSum() != null)
                .collect(Collectors.toMap(Debt::getHashSum, d -> d));

        List<String> statementHashes = accountStatementDebts.stream()
                .map(Debt::getHashSum)
                .filter(Objects::nonNull)
                .toList();

        Map<String, String> ignorableByHash = this.ignorableDebtRepository.findByHashSumIn(statementHashes)
                .stream()
                .collect(Collectors.toMap(IgnorableDebt::getHashSum, IgnorableDebt::getReason));

        List<Debt> newDebts = new ArrayList<>();
        List<DebtInstallmentUpdateDto> installmentUpdates = new ArrayList<>();
        List<IgnoredDebtPreviewDto> ignoredDebts = new ArrayList<>();

        for (Debt debt : DebtComparatorUtil.filterAccountStatementDebts(dbDebts, accountStatementDebts)) {
            if (ignorableByHash.containsKey(debt.getHashSum())) {
                ignoredDebts.add(new IgnoredDebtPreviewDto(debt, ignorableByHash.get(debt.getHashSum())));
                continue;
            }
            Debt dbMatch = dbByHash.get(debt.getHashSum());
            if (dbMatch != null) {
                DebtInstallmentUpdateDto update = new DebtInstallmentUpdateDto();
                update.setDebt(debt);
                update.setPreviousInstallment(dbMatch.getCurrentInstallment());
                update.setNewInstallment(debt.getCurrentInstallment());
                installmentUpdates.add(update);
            } else {
                newDebts.add(debt);
            }
        }

        // Debts whose statement entry is at max installment — they will be closed on save
        List<Debt> completedDebts = accountStatementDebts.stream()
                .filter(d -> d.getCurrentInstallment() != null
                        && d.getMaxFinancingTerm() != null
                        && d.getCurrentInstallment().equals(d.getMaxFinancingTerm()))
                .map(d -> dbByHash.get(d.getHashSum()))
                .filter(Objects::nonNull)
                .toList();

        List<FuzzyDuplicateDto> potentialDuplicates = DebtComparatorUtil.findFuzzyDuplicates(newDebts, dbDebts);

        return new AccountStatementPreviewDto(newDebts, installmentUpdates, completedDebts, ignoredDebts, potentialDuplicates);
    }

    @Override
    public void deactivateObsoleteDebts(List<Debt> statementDebts, String debtAccountCode) {
        List<Debt> dbDebts = this.debtRepository.findAllDebtsByDebtAccountAndActiveTrue(debtAccountCode)
                .stream()
                .peek(d -> { if (d.getHashSum() == null) d.setHashSum(this.getHashSum(d, debtAccountCode)); })
                .toList();

        if (dbDebts.isEmpty()) return;

        Set<String> activeStatementHashes = statementDebts.stream()
                .filter(d -> !d.getCurrentInstallment().equals(d.getMaxFinancingTerm()))
                .map(Debt::getHashSum)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        List<Debt> toDeactivate = dbDebts.stream()
                .filter(d -> !activeStatementHashes.contains(d.getHashSum()))
                .peek(d -> d.setActive(false))
                .toList();

        if (!toDeactivate.isEmpty()) {
            this.debtRepository.saveAll(toDeactivate);
        }
    }

    @Override
    public List<Debt> payOffByDebtAccountCode(String debtAccountCode) {
        List<Debt> debtsToPayOff = this.debtRepository.findAllDebtsByDebtAccountAndActiveTrue(debtAccountCode)
                .stream()
                .peek(debt -> debt.setActive(false))
                .toList();

        this.debtRepository.saveAll(debtsToPayOff);
        return debtsToPayOff;
    }

    @Override
    public List<Debt> getActiveByDebtAccount(String debtAccountCode) {
        return debtRepository.findAllDebtsByDebtAccountAndActiveTrue(debtAccountCode);
    }

    @Override
    public List<Debt> saveUnrepeated(List<Debt> debts, String debtAccountCode, List<String> overrideIgnoredHashes) {
        DebtAccount debtAccount = this.debtAccountRepository.findDebtAccountByCodeAndActiveTrue(debtAccountCode)
                .orElseThrow(() -> new EntityNotFoundException("Debt Account " + debtAccountCode));

        List<Debt> debtAccountDebts = debtRepository.findAllDebtsByDebtAccountAndActiveTrue(debtAccountCode)
                .stream()
                .peek(d -> { if (d.getHashSum() == null) d.setHashSum(this.getHashSum(d, debtAccountCode)); })
                .toList();

        debts.stream()
                .filter(debt -> Objects.isNull(debt.getHashSum()))
                .forEach(debt -> debt.setHashSum(this.getHashSum(debt, debtAccountCode)));

        List<String> incomingHashes = debts.stream()
                .map(Debt::getHashSum)
                .filter(Objects::nonNull)
                .toList();

        Set<String> ignoredHashes = this.ignorableDebtRepository.findByHashSumIn(incomingHashes)
                .stream()
                .map(IgnorableDebt::getHashSum)
                .filter(h -> overrideIgnoredHashes == null || !overrideIgnoredHashes.contains(h))
                .collect(Collectors.toSet());

        List<Debt> filteredDebts = debts.stream()
                .filter(d -> !ignoredHashes.contains(d.getHashSum()))
                .toList();

        Map<String, Debt> dbByHash = debtAccountDebts.stream()
                .filter(d -> d.getHashSum() != null)
                .collect(Collectors.toMap(Debt::getHashSum, d -> d));

        List<Debt> toSave = new ArrayList<>();
        List<Debt> toUpdate = new ArrayList<>();

        for (Debt debt : DebtComparatorUtil.filterAccountStatementDebts(debtAccountDebts, filteredDebts)) {
            Debt dbMatch = dbByHash.get(debt.getHashSum());
            if (dbMatch != null) {
                dbMatch.setCurrentInstallment(debt.getCurrentInstallment());
                dbMatch.setDebtType(debt.getDebtType());
                toUpdate.add(dbMatch);
            } else {
                debt.setDebtAccount(debtAccount);
                toSave.add(debt);
            }
        }

        // Deactivate DB debts whose statement entry reached max installment
        List<Debt> toDeactivate = filteredDebts.stream()
                .filter(d -> d.getCurrentInstallment() != null
                        && d.getMaxFinancingTerm() != null
                        && d.getCurrentInstallment().equals(d.getMaxFinancingTerm()))
                .map(d -> dbByHash.get(d.getHashSum()))
                .filter(Objects::nonNull)
                .peek(d -> d.setActive(false))
                .toList();

        if (!toDeactivate.isEmpty()) debtRepository.saveAll(toDeactivate);
        if (!toUpdate.isEmpty()) debtRepository.saveAll(toUpdate);
        return debtRepository.saveAll(toSave);
    }

    @Override
    public List<Debt> replaceAllWithStatement(List<Debt> incomingDebts, String debtAccountCode) {
        payOffByDebtAccountCode(debtAccountCode);

        DebtAccount debtAccount = this.debtAccountRepository.findDebtAccountByCodeAndActiveTrue(debtAccountCode)
                .orElseThrow(() -> new EntityNotFoundException("Debt Account " + debtAccountCode));

        incomingDebts.forEach(d -> {
            d.setDebtAccount(debtAccount);
            d.setActive(true);
            if (d.getHashSum() == null) d.setHashSum(getHashSum(d, debtAccountCode));
        });

        return debtRepository.saveAll(incomingDebts);
    }

    @Override
    public String getHashSum(Debt debt, String debtAccountCode) {
        if (debt.getMonthlyPayment() == null)
            throw new IllegalArgumentException("monthlyPayment is required to compute hash for debt: " + debt.getDescription());
        if (debt.getMaxFinancingTerm() == null)
            throw new IllegalArgumentException("maxFinancingTerm is required to compute hash for debt: " + debt.getDescription());
        String monthAmountTrim = debt.getMonthlyPayment().setScale(2, RoundingMode.UNNECESSARY)
                .movePointRight(2)
                .toPlainString();

        String toHash = String.join("|",
                debtAccountCode.trim(),
                monthAmountTrim,
                debt.getMaxFinancingTerm().toString()
        );

        try {
            // Get an instance of the SHA-256 algorithm
            MessageDigest md = MessageDigest.getInstance("SHA-256");

            // Convert the input string to bytes using UTF-8 encoding
            byte[] hashBytes = md.digest(toHash.getBytes(StandardCharsets.UTF_8));

            // Convert the byte array to a BigInteger for easy hexadecimal conversion
            BigInteger no = new BigInteger(1, hashBytes);

            // Convert the BigInteger to a hexadecimal string
            StringBuilder hashedPayload = new StringBuilder(no.toString(16));

            // Pad with leading zeros to ensure the correct length (64 characters for SHA-256)
            while (hashedPayload.length() < 64) {
                hashedPayload.insert(0, "0");
            }

            return hashedPayload.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }
}
