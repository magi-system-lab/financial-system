package com.jimm0063.magi.debt.management.debtmanagementltesystem.infrastructure.adapter;

import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.application.port.out.UserRepository;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.exceptions.EntityNotFoundException;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.model.DebtSysUser;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.infrastructure.entity.DebtSysUserEntity;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.infrastructure.mapper.DebtSysUserMapper;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.infrastructure.persistence.DebtSysUserJpaRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class UserRepositoryAdapter implements UserRepository {
    private final DebtSysUserJpaRepository debtSysUserJpaRepository;
    private final DebtSysUserMapper debtSysUserMapper;

    public UserRepositoryAdapter(DebtSysUserJpaRepository debtSysUserJpaRepository, DebtSysUserMapper debtSysUserMapper) {
        this.debtSysUserJpaRepository = debtSysUserJpaRepository;
        this.debtSysUserMapper = debtSysUserMapper;
    }

    @Override
    public Optional<DebtSysUser> findUserByEmail(String email) {
        return this.debtSysUserJpaRepository.findByEmailAndActiveTrue(email)
                .map(debtSysUserMapper::toModel);
    }

    @Override
    public DebtSysUser updateFinancials(String email, Double salary, Double savings) {
        DebtSysUserEntity userEntity = this.debtSysUserJpaRepository.findByEmailAndActiveTrue(email)
                .orElseThrow(() -> new EntityNotFoundException("User " + email + " not found"));

        if (salary != null) userEntity.setSalary(salary);
        if (savings != null) userEntity.setSavings(savings);

        return this.debtSysUserMapper.toModel(this.debtSysUserJpaRepository.save(userEntity));
    }

    @Override
    public DebtSysUser updateDebtLimit(String email, Integer debtLimitPercent) {
        DebtSysUserEntity userEntity = this.debtSysUserJpaRepository.findByEmailAndActiveTrue(email)
                .orElseThrow(() -> new EntityNotFoundException("User " + email + " not found"));
        userEntity.setDebtLimitPercent(debtLimitPercent == null || debtLimitPercent <= 0 ? null : Math.min(debtLimitPercent, 100));
        return this.debtSysUserMapper.toModel(this.debtSysUserJpaRepository.save(userEntity));
    }
}
