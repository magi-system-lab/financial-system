package com.jimm0063.magi.debt.management.debtmanagementltesystem.infrastructure.adapter;

import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.application.port.out.DebtRepository;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.exceptions.EntityNotFoundException;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.model.Debt;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.model.DebtAccount;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.infrastructure.entity.DebtAccountEntity;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.infrastructure.entity.DebtEntity;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.infrastructure.entity.DebtSysUserEntity;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.infrastructure.mapper.DebtAccountMapper;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.infrastructure.mapper.DebtMapper;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.infrastructure.persistence.DebtAccountJpaRepository;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.infrastructure.persistence.DebtJpaRepository;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.infrastructure.persistence.DebtSysUserJpaRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class DebtRepositoryAdapter implements DebtRepository {
    private final DebtJpaRepository debtJpaRepository;
    private final DebtMapper debtMapper;
    private final DebtAccountMapper debtAccountMapper;
    private final DebtAccountJpaRepository debtAccountJpaRepository;
    private final DebtSysUserJpaRepository debtSysUserJpaRepository;

    public DebtRepositoryAdapter(DebtJpaRepository debtJpaRepository, DebtMapper debtMapper,
                                 DebtAccountMapper debtAccountMapper, DebtAccountJpaRepository debtAccountJpaRepository, DebtSysUserJpaRepository debtSysUserJpaRepository) {
        this.debtJpaRepository = debtJpaRepository;
        this.debtMapper = debtMapper;
        this.debtAccountMapper = debtAccountMapper;
        this.debtAccountJpaRepository = debtAccountJpaRepository;
        this.debtSysUserJpaRepository = debtSysUserJpaRepository;
    }

    @Override
    public List<Debt> findAllDebtsByDebtAccountAndActiveTrue(String debtAccountCode) {
        return this.debtJpaRepository.findAllDebtsByDebtAccount_CodeAndActiveTrue(debtAccountCode)
                .stream()
                .map(debtMapper::toModel)
                .toList();
    }

    @Override
    public List<Debt> findDebtHistoryByUser(String email) {
        return this.debtJpaRepository.findAllByDebtAccount_FinancialProvider_DebtSysUser_Email(email)
                .stream()
                .map(debtMapper::toModel)
                .toList();
    }

    @Override
    public List<Debt> findAllDebtsByUser(String email) {
        DebtSysUserEntity user = this.debtSysUserJpaRepository.findByEmailAndActiveTrue(email)
                .orElseThrow(() -> new EntityNotFoundException("User " + email + " not found"));

        return user.getFinancialProviders().stream()
                .flatMap(financialProviderEntity -> financialProviderEntity.getDebtAccounts().stream())
                .flatMap(debtAccountEntity -> debtAccountEntity.getDebt().stream())
                .filter(DebtEntity::getActive)
                .map(debtMapper::toModel)
                .toList();
    }

    @Override
    public List<Debt> saveAll(List<Debt> debt, DebtAccount debtAccount) {
        DebtAccountEntity debtAccountEntity = debtAccountMapper.toEntity(debtAccount);
        List<DebtEntity> debtEntities = debt.stream().map(debtMapper::toEntity).toList();
        debtEntities.forEach(debtEntity -> debtEntity.setDebtAccount(debtAccountEntity));
        return this.debtJpaRepository.saveAllAndFlush(debtEntities)
                .stream().map(debtMapper::toModel).collect(Collectors.toList());
    }

    @Override
    public List<Debt> saveAll(List<Debt> debt) {
        List<DebtEntity> listEntity = debt.stream().map(debtMapper::toEntity).toList();
        List<DebtEntity> savedList = this.debtJpaRepository.saveAllAndFlush(listEntity);
        List<Debt> listModel = savedList.stream().map(debtMapper::toModel).toList();
        return listModel;
    }

    @Override
    public Debt save(Debt debt, String debtAccountCode) {
        DebtAccountEntity debtAccountEntity = debtAccountJpaRepository.findById(debtAccountCode)
                .orElseThrow(() -> new EntityNotFoundException("DebtAccount " + debtAccountCode + " not found"));
        DebtEntity debtEntity = debtMapper.toEntity(debt);
        debtEntity.setDebtAccount(debtAccountEntity);
        debtEntity.setActive(true);

        debtJpaRepository.save(debtEntity);
        return debtMapper.toModel(this.debtJpaRepository.save(debtEntity));
    }

    @Override
    public Debt update(Debt debt) {
        DebtEntity existing = debtJpaRepository.findById(debt.getId())
                .orElseThrow(() -> new EntityNotFoundException("Debt " + debt.getId() + " not found"));
        existing.setDescription(debt.getDescription());
        existing.setOperationDate(debt.getOperationDate());
        existing.setOriginalAmount(debt.getOriginalAmount());
        existing.setMonthlyPayment(debt.getMonthlyPayment());
        existing.setCurrentInstallment(debt.getCurrentInstallment());
        existing.setMaxFinancingTerm(debt.getMaxFinancingTerm());
        existing.setDebtType(debt.getDebtType());
        existing.setActive(debt.getActive());
        return debtMapper.toModel(debtJpaRepository.save(existing));
    }

    @Override
    public void delete(Integer debtId) {
        DebtEntity debtEntity = debtJpaRepository.findById(debtId)
                        .orElseThrow(() -> new EntityNotFoundException("Debt " + debtId + " not found"));
        debtEntity.setActive(false);
        this.debtJpaRepository.save(debtEntity);
    }
}
