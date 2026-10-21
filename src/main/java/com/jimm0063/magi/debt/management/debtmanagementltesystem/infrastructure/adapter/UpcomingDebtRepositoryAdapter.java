package com.jimm0063.magi.debt.management.debtmanagementltesystem.infrastructure.adapter;

import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.application.port.out.UpcomingDebtRepository;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.enums.UpcomingDebtStatus;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.exceptions.EntityNotFoundException;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.model.UpcomingDebt;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.infrastructure.entity.DebtAccountEntity;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.infrastructure.entity.DebtSysUserEntity;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.infrastructure.entity.UpcomingDebtEntity;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.infrastructure.mapper.UpcomingDebtMapper;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.infrastructure.persistence.DebtAccountJpaRepository;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.infrastructure.persistence.DebtSysUserJpaRepository;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.infrastructure.persistence.UpcomingDebtJpaRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class UpcomingDebtRepositoryAdapter implements UpcomingDebtRepository {
    private final UpcomingDebtJpaRepository upcomingDebtJpaRepository;
    private final UpcomingDebtMapper upcomingDebtMapper;
    private final DebtSysUserJpaRepository debtSysUserJpaRepository;
    private final DebtAccountJpaRepository debtAccountJpaRepository;

    public UpcomingDebtRepositoryAdapter(UpcomingDebtJpaRepository upcomingDebtJpaRepository,
                                         UpcomingDebtMapper upcomingDebtMapper,
                                         DebtSysUserJpaRepository debtSysUserJpaRepository,
                                         DebtAccountJpaRepository debtAccountJpaRepository) {
        this.upcomingDebtJpaRepository = upcomingDebtJpaRepository;
        this.upcomingDebtMapper = upcomingDebtMapper;
        this.debtSysUserJpaRepository = debtSysUserJpaRepository;
        this.debtAccountJpaRepository = debtAccountJpaRepository;
    }

    @Override
    public List<UpcomingDebt> findAllActiveByUser(String email) {
        return upcomingDebtJpaRepository.findAllByDebtSysUser_EmailAndActiveTrueOrderByFirstPaymentDateAsc(email)
                .stream()
                .map(upcomingDebtMapper::toModel)
                .toList();
    }

    @Override
    public List<UpcomingDebt> findAllPendingByUser(String email) {
        return upcomingDebtJpaRepository
                .findAllByDebtSysUser_EmailAndStatusAndActiveTrueOrderByFirstPaymentDateAsc(email, UpcomingDebtStatus.PENDING)
                .stream()
                .map(upcomingDebtMapper::toModel)
                .toList();
    }

    @Override
    public List<UpcomingDebt> findAllPendingByDebtAccount(String debtAccountCode) {
        return upcomingDebtJpaRepository
                .findAllByDebtAccount_CodeAndStatusAndActiveTrueOrderByFirstPaymentDateAsc(debtAccountCode, UpcomingDebtStatus.PENDING)
                .stream()
                .map(upcomingDebtMapper::toModel)
                .toList();
    }

    @Override
    public Optional<UpcomingDebt> findById(Integer id) {
        return upcomingDebtJpaRepository.findById(id)
                .filter(e -> Boolean.TRUE.equals(e.getActive()))
                .map(upcomingDebtMapper::toModel);
    }

    @Override
    public UpcomingDebt save(UpcomingDebt upcomingDebt, String userEmail, String debtAccountCode) {
        DebtSysUserEntity user = debtSysUserJpaRepository.findByEmailAndActiveTrue(userEmail)
                .orElseThrow(() -> new EntityNotFoundException("User " + userEmail + " not found"));
        UpcomingDebtEntity entity = upcomingDebtMapper.toEntity(upcomingDebt);
        entity.setDebtSysUser(user);
        entity.setDebtAccount(resolveAccount(debtAccountCode));
        entity.setActive(true);
        return upcomingDebtMapper.toModel(upcomingDebtJpaRepository.save(entity));
    }

    @Override
    public UpcomingDebt update(UpcomingDebt upcomingDebt, String debtAccountCode) {
        UpcomingDebtEntity existing = upcomingDebtJpaRepository.findById(upcomingDebt.getId())
                .orElseThrow(() -> new EntityNotFoundException("Upcoming debt " + upcomingDebt.getId() + " not found"));
        existing.setDescription(upcomingDebt.getDescription());
        existing.setTotalAmount(upcomingDebt.getTotalAmount());
        existing.setMonthlyPayment(upcomingDebt.getMonthlyPayment());
        existing.setInstallments(upcomingDebt.getInstallments());
        existing.setPurchaseDate(upcomingDebt.getPurchaseDate());
        existing.setFirstPaymentDate(upcomingDebt.getFirstPaymentDate());
        existing.setDebtType(upcomingDebt.getDebtType());
        existing.setStatus(upcomingDebt.getStatus());
        existing.setStartedAt(upcomingDebt.getStartedAt());
        existing.setDebtAccount(resolveAccount(debtAccountCode));
        return upcomingDebtMapper.toModel(upcomingDebtJpaRepository.save(existing));
    }

    @Override
    public void delete(Integer upcomingDebtId) {
        UpcomingDebtEntity entity = upcomingDebtJpaRepository.findById(upcomingDebtId)
                .orElseThrow(() -> new EntityNotFoundException("Upcoming debt " + upcomingDebtId + " not found"));
        entity.setActive(false);
        upcomingDebtJpaRepository.save(entity);
    }

    private DebtAccountEntity resolveAccount(String debtAccountCode) {
        if (debtAccountCode == null || debtAccountCode.isBlank()) return null;
        return debtAccountJpaRepository.findDebtAccountByCodeAndActiveTrue(debtAccountCode)
                .orElseThrow(() -> new EntityNotFoundException("Debt account " + debtAccountCode + " not found"));
    }
}
