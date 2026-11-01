package com.jimm0063.magi.debt.management.debtmanagementltesystem.infrastructure.adapter;

import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.application.port.out.SavingsGoalContributionRepository;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.exceptions.EntityNotFoundException;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.model.SavingsGoalContribution;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.infrastructure.entity.SavingsGoalContributionEntity;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.infrastructure.entity.SavingsGoalEntity;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.infrastructure.mapper.SavingsGoalContributionMapper;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.infrastructure.persistence.SavingsGoalContributionJpaRepository;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.infrastructure.persistence.SavingsGoalJpaRepository;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class SavingsGoalContributionRepositoryAdapter implements SavingsGoalContributionRepository {
    private final SavingsGoalContributionJpaRepository contributionJpaRepository;
    private final SavingsGoalJpaRepository savingsGoalJpaRepository;
    private final SavingsGoalContributionMapper contributionMapper;

    public SavingsGoalContributionRepositoryAdapter(SavingsGoalContributionJpaRepository contributionJpaRepository,
                                                    SavingsGoalJpaRepository savingsGoalJpaRepository,
                                                    SavingsGoalContributionMapper contributionMapper) {
        this.contributionJpaRepository = contributionJpaRepository;
        this.savingsGoalJpaRepository = savingsGoalJpaRepository;
        this.contributionMapper = contributionMapper;
    }

    @Override
    public List<SavingsGoalContribution> findAllByGoal(Integer goalId) {
        return contributionJpaRepository.findAllBySavingsGoal_IdAndActiveTrueOrderByContributionDateDescIdDesc(goalId)
                .stream()
                .map(contributionMapper::toModel)
                .toList();
    }

    @Override
    public Map<Integer, List<SavingsGoalContribution>> findAllByUser(String email) {
        Map<Integer, List<SavingsGoalContribution>> byGoal = new LinkedHashMap<>();
        for (SavingsGoalContributionEntity e : contributionJpaRepository
                .findAllBySavingsGoal_DebtSysUser_EmailAndSavingsGoal_ActiveTrueAndActiveTrueOrderByContributionDateDescIdDesc(email)) {
            byGoal.computeIfAbsent(e.getSavingsGoal().getId(), k -> new ArrayList<>()).add(contributionMapper.toModel(e));
        }
        return byGoal;
    }

    @Override
    public Optional<Integer> findGoalIdOf(Integer contributionId) {
        return contributionJpaRepository.findById(contributionId)
                .filter(e -> Boolean.TRUE.equals(e.getActive()))
                .map(e -> e.getSavingsGoal().getId());
    }

    @Override
    public SavingsGoalContribution save(SavingsGoalContribution contribution, Integer goalId) {
        SavingsGoalEntity goal = savingsGoalJpaRepository.findById(goalId)
                .filter(e -> Boolean.TRUE.equals(e.getActive()))
                .orElseThrow(() -> new EntityNotFoundException("Savings goal " + goalId + " not found"));
        SavingsGoalContributionEntity entity = contributionMapper.toEntity(contribution);
        entity.setId(null);
        entity.setSavingsGoal(goal);
        entity.setActive(true);
        return contributionMapper.toModel(contributionJpaRepository.save(entity));
    }

    @Override
    public void delete(Integer id) {
        SavingsGoalContributionEntity entity = contributionJpaRepository.findById(id)
                .filter(e -> Boolean.TRUE.equals(e.getActive()))
                .orElseThrow(() -> new EntityNotFoundException("Contribution " + id + " not found"));
        entity.setActive(false);
        contributionJpaRepository.save(entity);
    }
}
