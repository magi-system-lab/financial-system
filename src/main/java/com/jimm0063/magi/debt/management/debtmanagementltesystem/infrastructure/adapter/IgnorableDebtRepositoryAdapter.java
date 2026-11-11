package com.jimm0063.magi.debt.management.debtmanagementltesystem.infrastructure.adapter;

import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.application.port.out.IgnorableDebtRepository;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.exceptions.EntityNotFoundException;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.model.IgnorableDebt;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.infrastructure.entity.IgnorableDebtEntity;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.infrastructure.mapper.IgnorableDebtMapper;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.infrastructure.persistence.IgnorableDebtJpaRepository;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class IgnorableDebtRepositoryAdapter implements IgnorableDebtRepository {
    private final IgnorableDebtJpaRepository jpaRepository;
    private final IgnorableDebtMapper mapper;

    public IgnorableDebtRepositoryAdapter(IgnorableDebtJpaRepository jpaRepository, IgnorableDebtMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public List<IgnorableDebt> findAllActive() {
        return jpaRepository.findAllByActiveTrue().stream()
                .map(mapper::toModel)
                .toList();
    }

    @Override
    public List<IgnorableDebt> findByHashSumIn(List<String> hashSums) {
        if (hashSums == null || hashSums.isEmpty()) return List.of();
        return jpaRepository.findByHashSumInAndActiveTrue(hashSums).stream()
                .map(mapper::toModel)
                .toList();
    }

    @Override
    public IgnorableDebt save(IgnorableDebt ignorableDebt) {
        // hash_sum is unique: a hash that was ignored before and then un-ignored is re-activated, not inserted again
        IgnorableDebtEntity entity = ignorableDebt.getHashSum() == null ? null
                : jpaRepository.findByHashSum(ignorableDebt.getHashSum()).orElse(null);
        if (entity == null) {
            entity = mapper.toEntity(ignorableDebt);
        } else {
            entity.setReason(ignorableDebt.getReason());
            entity.setActive(ignorableDebt.getActive());
        }
        return mapper.toModel(jpaRepository.save(entity));
    }

    @Override
    public void deactivate(String hashSum) {
        IgnorableDebtEntity entity = jpaRepository.findByHashSum(hashSum)
                .orElseThrow(() -> new EntityNotFoundException("IgnorableDebt with hashSum " + hashSum));
        entity.setActive(false);
        jpaRepository.save(entity);
    }
}
