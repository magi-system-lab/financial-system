package com.jimm0063.magi.debt.management.debtmanagementltesystem.infrastructure.persistence;

import com.jimm0063.magi.debt.management.debtmanagementltesystem.infrastructure.entity.SavingsGoalEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SavingsGoalJpaRepository extends JpaRepository<SavingsGoalEntity, Integer> {
    List<SavingsGoalEntity> findAllByDebtSysUser_EmailAndActiveTrueOrderByCreatedAtDesc(String email);
}
