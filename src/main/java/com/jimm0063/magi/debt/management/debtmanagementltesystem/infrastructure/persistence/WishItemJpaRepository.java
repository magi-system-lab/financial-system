package com.jimm0063.magi.debt.management.debtmanagementltesystem.infrastructure.persistence;

import com.jimm0063.magi.debt.management.debtmanagementltesystem.infrastructure.entity.WishItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WishItemJpaRepository extends JpaRepository<WishItemEntity, Integer> {
    List<WishItemEntity> findAllByDebtSysUser_EmailAndActiveTrueOrderByCreatedAtDesc(String email);
}
