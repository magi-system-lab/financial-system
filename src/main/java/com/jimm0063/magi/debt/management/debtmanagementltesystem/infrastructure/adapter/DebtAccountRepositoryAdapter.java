package com.jimm0063.magi.debt.management.debtmanagementltesystem.infrastructure.adapter;

import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.application.port.out.DebtAccountRepository;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.enums.AccountStatementType;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.exceptions.EntityNotFoundException;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.model.DebtAccount;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.model.FinancialProvider;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.infrastructure.entity.DebtAccountEntity;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.infrastructure.entity.FinancialProviderEntity;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.infrastructure.mapper.DebtAccountMapper;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.infrastructure.mapper.FinancialProviderMapper;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.infrastructure.persistence.DebtAccountJpaRepository;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.infrastructure.persistence.FinancialProviderJpaRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class DebtAccountRepositoryAdapter implements DebtAccountRepository {
    private final DebtAccountJpaRepository debtAccountJpaRepository;
    private final DebtAccountMapper debtAccountMapper;
    private final FinancialProviderMapper financialProviderMapper;
    private final FinancialProviderJpaRepository financialProviderJpaRepository;

    public DebtAccountRepositoryAdapter(DebtAccountJpaRepository debtAccountJpaRepository,
                                        DebtAccountMapper debtAccountMapper,
                                        FinancialProviderMapper financialProviderMapper,
                                        FinancialProviderJpaRepository financialProviderJpaRepository) {
        this.debtAccountJpaRepository = debtAccountJpaRepository;
        this.debtAccountMapper = debtAccountMapper;
        this.financialProviderMapper = financialProviderMapper;
        this.financialProviderJpaRepository = financialProviderJpaRepository;
    }

    @Override
    public Optional<DebtAccount> findDebtAccountByCodeAndActiveTrue(String code) {
        return this.debtAccountJpaRepository.findDebtAccountByCodeAndActiveTrue(code)
                .map(this.debtAccountMapper::toModel);
    }

    @Override
    public List<DebtAccount> findAllByFinancialProvider(FinancialProvider financialProvider) {
        FinancialProviderEntity financialProviderEntity = financialProviderMapper.toEntity(financialProvider);
        return this.debtAccountJpaRepository.findAllByFinancialProvider(financialProviderEntity)
                .stream()
                .map(this.debtAccountMapper::toModel)
                .toList();
    }

    @Override
    public List<DebtAccount> findAllByFinancialProviderCodeAndActiveTrue(String financialProviderCode) {
        return this.debtAccountJpaRepository.findAllByFinancialProvider_CodeAndActiveTrue(financialProviderCode)
                .stream()
                .map(debtAccountMapper::toModel)
                .toList();
    }

    @Override
    public DebtAccount save(DebtAccount debtAccount, String financialProviderCode) {
        FinancialProviderEntity financialProviderEntity = financialProviderJpaRepository.findByCode(financialProviderCode)
                .orElseThrow(() -> new EntityNotFoundException("Financial provider " + financialProviderCode + " not found"));

        DebtAccountEntity debtAccountEntity = debtAccountMapper.toEntity(debtAccount);
        debtAccountEntity.setActive(true);
        debtAccountEntity.setFinancialProvider(financialProviderEntity);
        return this.debtAccountMapper.toModel(debtAccountJpaRepository.save(debtAccountEntity));
    }

    @Override
    public DebtAccount update(DebtAccount debtAccount) {
        DebtAccountEntity debtAccountEntity = debtAccountMapper.toEntity(debtAccount);
        // Clients that don't know about the card colour must not erase it.
        if (debtAccountEntity.getColor() == null && debtAccount.getCode() != null) {
            debtAccountJpaRepository.findById(debtAccount.getCode())
                    .ifPresent(existing -> debtAccountEntity.setColor(existing.getColor()));
        }
        return this.debtAccountMapper.toModel(debtAccountJpaRepository.save(debtAccountEntity));
    }

    @Override
    public DebtAccount updateStatementType(String code, AccountStatementType accountStatementType) {
        DebtAccountEntity entity = debtAccountJpaRepository.findById(code)
                .orElseThrow(() -> new EntityNotFoundException("Debt account " + code + " not found"));
        entity.setAccountStatementType(accountStatementType);
        return debtAccountMapper.toModel(debtAccountJpaRepository.save(entity));
    }

    @Override
    public DebtAccount updateColor(String code, String color) {
        DebtAccountEntity entity = debtAccountJpaRepository.findById(code)
                .orElseThrow(() -> new EntityNotFoundException("Debt account " + code + " not found"));
        entity.setColor(color);
        return debtAccountMapper.toModel(debtAccountJpaRepository.save(entity));
    }

    @Override
    public void delete(String debtAccountCode) {
        DebtAccountEntity debtAccountEntity = debtAccountJpaRepository.findById(debtAccountCode)
                .orElseThrow(() -> new IllegalArgumentException("Debt account " + debtAccountCode + " not found"));
        debtAccountEntity.setActive(false);
        debtAccountJpaRepository.save(debtAccountEntity);
    }
}
