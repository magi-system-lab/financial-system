package com.jimm0063.magi.debt.management.debtmanagementltesystem.infrastructure.adapter;

import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.application.port.out.SavingsGoalRepository;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.exceptions.EntityNotFoundException;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.model.SavingsGoal;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.infrastructure.entity.DebtSysUserEntity;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.infrastructure.entity.SavingsGoalEntity;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.infrastructure.mapper.SavingsGoalMapper;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.infrastructure.persistence.DebtSysUserJpaRepository;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.infrastructure.persistence.SavingsGoalJpaRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class SavingsGoalRepositoryAdapter implements SavingsGoalRepository {
    private final SavingsGoalJpaRepository savingsGoalJpaRepository;
    private final SavingsGoalMapper savingsGoalMapper;
    private final DebtSysUserJpaRepository debtSysUserJpaRepository;

    public SavingsGoalRepositoryAdapter(SavingsGoalJpaRepository savingsGoalJpaRepository, SavingsGoalMapper savingsGoalMapper,
                                        DebtSysUserJpaRepository debtSysUserJpaRepository) {
        this.savingsGoalJpaRepository = savingsGoalJpaRepository;
        this.savingsGoalMapper = savingsGoalMapper;
        this.debtSysUserJpaRepository = debtSysUserJpaRepository;
    }

    @Override
    public List<SavingsGoal> findAllActiveByUser(String email) {
        return savingsGoalJpaRepository.findAllByDebtSysUser_EmailAndActiveTrueOrderByCreatedAtDesc(email)
                .stream()
                .map(savingsGoalMapper::toModel)
                .toList();
    }

    @Override
    public Optional<SavingsGoal> findById(Integer id) {
        return savingsGoalJpaRepository.findById(id)
                .filter(e -> Boolean.TRUE.equals(e.getActive()))
                .map(savingsGoalMapper::toModel);
    }

    @Override
    public SavingsGoal save(SavingsGoal goal, String userEmail) {
        DebtSysUserEntity user = debtSysUserJpaRepository.findByEmailAndActiveTrue(userEmail)
                .orElseThrow(() -> new EntityNotFoundException("User " + userEmail + " not found"));
        SavingsGoalEntity entity = savingsGoalMapper.toEntity(goal);
        entity.setId(null);
        entity.setDebtSysUser(user);
        entity.setActive(true);
        return savingsGoalMapper.toModel(savingsGoalJpaRepository.save(entity));
    }

    @Override
    public SavingsGoal update(SavingsGoal goal) {
        SavingsGoalEntity entity = find(goal.getId());
        entity.setName(goal.getName());
        entity.setTargetAmount(goal.getTargetAmount());
        entity.setTargetDate(goal.getTargetDate());
        entity.setDebtId(goal.getDebtId());
        entity.setDebtHash(goal.getDebtHash());
        entity.setStatus(goal.getStatus());
        entity.setCompletedAt(goal.getCompletedAt());
        return savingsGoalMapper.toModel(savingsGoalJpaRepository.save(entity));
    }

    @Override
    public void delete(Integer id) {
        SavingsGoalEntity entity = find(id);
        entity.setActive(false);
        savingsGoalJpaRepository.save(entity);
    }

    private SavingsGoalEntity find(Integer id) {
        return savingsGoalJpaRepository.findById(id)
                .filter(e -> Boolean.TRUE.equals(e.getActive()))
                .orElseThrow(() -> new EntityNotFoundException("Savings goal " + id + " not found"));
    }
}
