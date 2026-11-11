package com.jimm0063.magi.debt.management.debtmanagementltesystem.infrastructure.adapter;

import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.application.port.out.FixedExpenseRepository;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.enums.FixedExpenseKind;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.exceptions.EntityNotFoundException;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.model.DebtSysUser;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.model.FixedExpense;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.infrastructure.entity.DebtSysUserEntity;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.infrastructure.entity.FixedExpenseCatalogEntity;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.infrastructure.entity.FixedExpenseEntity;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.infrastructure.mapper.DebtSysUserMapper;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.infrastructure.mapper.FixedExpenseMapper;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.infrastructure.model.FixedExpenseReq;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.infrastructure.persistence.DebtSysUserJpaRepository;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.infrastructure.persistence.FixedExpenseCatalogJpaRepository;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.infrastructure.persistence.FixedExpenseJpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

@Component
public class FixedExpenseRepositoryAdapter implements FixedExpenseRepository {
    private final FixedExpenseJpaRepository fixedExpenseJpaRepository;
    private final FixedExpenseMapper fixedExpenseMapper;
    private final DebtSysUserJpaRepository debtSysUserJpaRepository;
    private final DebtSysUserMapper debtSysUserMapper;
    private final FixedExpenseCatalogJpaRepository fixedExpenseCatalogJpaRepository;

    public FixedExpenseRepositoryAdapter(FixedExpenseJpaRepository fixedExpenseJpaRepository,
                                         FixedExpenseMapper fixedExpenseMapper,
                                         DebtSysUserJpaRepository debtSysUserJpaRepository,
                                         DebtSysUserMapper debtSysUserMapper, FixedExpenseCatalogJpaRepository fixedExpenseCatalogJpaRepository) {
        this.fixedExpenseJpaRepository = fixedExpenseJpaRepository;
        this.fixedExpenseMapper = fixedExpenseMapper;
        this.debtSysUserJpaRepository = debtSysUserJpaRepository;
        this.debtSysUserMapper = debtSysUserMapper;
        this.fixedExpenseCatalogJpaRepository = fixedExpenseCatalogJpaRepository;
    }

    @Override
    public List<FixedExpense> findAllFixedExpenseBySystemUserAndActiveTrue(DebtSysUser systemUser) {
        DebtSysUserEntity debtSysUserEntity = debtSysUserMapper.toEntity(systemUser);
        return fixedExpenseJpaRepository.findAllByDebtSysUserAndActiveTrue(debtSysUserEntity)
                .stream()
                .map(fixedExpenseMapper::toModel)
                .toList();
    }

    @Override
    public List<FixedExpense> findAllFixedExpenseByEmailAndActiveTrue(String email) {
        return this.fixedExpenseJpaRepository.findAllByDebtSysUser_EmailAndActiveTrue(email)
                .stream()
                .map(fixedExpenseMapper::toModel)
                .toList();
    }

    @Override
    public Optional<FixedExpense> findByIdAndActiveTrue(Integer fixedExpenseId) {
        return this.fixedExpenseJpaRepository.findByIdAndActiveTrue(fixedExpenseId)
                .map(fixedExpenseMapper::toModel);
    }

    @Override
    public FixedExpense save(FixedExpenseReq fixedExpense, Integer fixedExpenseCatalogId) {
        DebtSysUserEntity debtSysUserEntity = debtSysUserJpaRepository.findByEmailAndActiveTrue(fixedExpense.getUserEmail())
                .orElseThrow(()->new EntityNotFoundException("User " + fixedExpense.getUserEmail() + " not found"));

        FixedExpenseCatalogEntity fixedExpenseCatalog = fixedExpenseCatalogJpaRepository.findById(fixedExpenseCatalogId)
                .orElseThrow(()->new EntityNotFoundException("Catalog " + fixedExpenseCatalogId + " not found"));

        FixedExpenseEntity fixedExpenseEntity = fixedExpenseMapper.toEntity(fixedExpense);
        fixedExpenseEntity.setDebtSysUser(debtSysUserEntity);
        fixedExpenseEntity.setFixedExpenseCatalog(fixedExpenseCatalog);
        fixedExpenseEntity.setKind(FixedExpenseKind.orDefault(fixedExpense.getKind()));
        fixedExpenseEntity.setEndDate(endDate(fixedExpense.getEndMonth()));
        fixedExpenseEntity.setActive(true);

        return this.fixedExpenseMapper.toModel(this.fixedExpenseJpaRepository.save(fixedExpenseEntity));
    }

    @Override
    public FixedExpense update(FixedExpense fixedExpense) {
        FixedExpenseEntity fixedExpenseEntity = fixedExpenseMapper.toEntity(fixedExpense);
        return fixedExpenseMapper.toModel(fixedExpenseJpaRepository.save(fixedExpenseEntity));
    }

    @Override
    public void delete(Integer fixedExpenseId) {
        FixedExpenseEntity fixedExpenseEntity = this.fixedExpenseJpaRepository.findById(fixedExpenseId)
                        .orElseThrow(()->new EntityNotFoundException("FixedExpense with id " + fixedExpenseId + " not found"));
        fixedExpenseEntity.setActive(false);
        this.fixedExpenseJpaRepository.save(fixedExpenseEntity);
    }

    @Override
    public FixedExpense updateFromReq(FixedExpenseReq req) {
        FixedExpenseEntity fixedExpenseEntity = applyReq(req);
        return fixedExpenseMapper.toModel(fixedExpenseJpaRepository.save(fixedExpenseEntity));
    }

    @Override
    @Transactional
    public List<FixedExpense> updateMultiple(List<FixedExpenseReq> reqs) {
        return reqs.stream()
                .map(this::applyReq)
                .map(fixedExpenseJpaRepository::save)
                .map(fixedExpenseMapper::toModel)
                .toList();
    }

    @Override
    @Transactional
    public List<FixedExpense> bulkUpdateCategoryAndCost(List<Integer> ids, Integer catalogId, BigDecimal monthlyCost) {
        FixedExpenseCatalogEntity catalog = catalogId != null
                ? fixedExpenseCatalogJpaRepository.findById(catalogId)
                        .orElseThrow(() -> new EntityNotFoundException("Catalog " + catalogId + " not found"))
                : null;

        return ids.stream()
                .map(id -> fixedExpenseJpaRepository.findById(id)
                        .orElseThrow(() -> new EntityNotFoundException("FixedExpense with id " + id + " not found")))
                .map(entity -> {
                    if (catalog != null) entity.setFixedExpenseCatalog(catalog);
                    if (monthlyCost != null) entity.setMonthlyCost(monthlyCost);
                    return entity;
                })
                .map(fixedExpenseJpaRepository::save)
                .map(fixedExpenseMapper::toModel)
                .toList();
    }

    @Override
    @Transactional
    public List<FixedExpense> changeKind(List<Integer> ids, FixedExpenseKind kind) {
        FixedExpenseKind target = FixedExpenseKind.orDefault(kind);
        return ids.stream()
                .map(id -> fixedExpenseJpaRepository.findById(id)
                        .orElseThrow(() -> new EntityNotFoundException("FixedExpense with id " + id + " not found")))
                .peek(entity -> entity.setKind(target))
                .map(fixedExpenseJpaRepository::save)
                .map(fixedExpenseMapper::toModel)
                .toList();
    }

    @Override
    @Transactional
    public void deleteMultiple(List<Integer> ids) {
        ids.forEach(this::delete);
    }

    private FixedExpenseEntity applyReq(FixedExpenseReq req) {
        FixedExpenseEntity fixedExpenseEntity = fixedExpenseJpaRepository.findById(req.getId())
                .orElseThrow(() -> new EntityNotFoundException("FixedExpense with id " + req.getId() + " not found"));

        if (req.getName() != null) fixedExpenseEntity.setName(req.getName());
        if (req.getMonthlyCost() != null) fixedExpenseEntity.setMonthlyCost(BigDecimal.valueOf(req.getMonthlyCost()));
        if (req.getPaymentDay() != null) fixedExpenseEntity.setPaymentDay(req.getPaymentDay());
        if (req.getDescription() != null) fixedExpenseEntity.setDescription(req.getDescription());
        if (req.getKind() != null) fixedExpenseEntity.setKind(req.getKind());
        if (req.getEndMonth() != null) fixedExpenseEntity.setEndDate(endDate(req.getEndMonth()));
        if (req.getCatalogId() != null) {
            FixedExpenseCatalogEntity catalog = fixedExpenseCatalogJpaRepository.findById(req.getCatalogId())
                    .orElseThrow(() -> new EntityNotFoundException("Catalog " + req.getCatalogId() + " not found"));
            fixedExpenseEntity.setFixedExpenseCatalog(catalog);
        }
        return fixedExpenseEntity;
    }

    /** "yyyy-MM" → 1st day of that month; blank → null (no end). */
    static LocalDate endDate(String endMonth) {
        if (endMonth == null || endMonth.isBlank()) return null;
        return YearMonth.parse(endMonth.trim()).atDay(1);
    }
}
