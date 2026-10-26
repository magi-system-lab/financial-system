package com.jimm0063.magi.debt.management.debtmanagementltesystem.infrastructure.persistence;

import com.jimm0063.magi.debt.management.debtmanagementltesystem.infrastructure.entity.DebtEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

//@RepositoryRestResource(collectionResourceRel = "debt", path = "debt")
@Repository
public interface DebtJpaRepository extends JpaRepository<DebtEntity, Integer> {
    List<DebtEntity> findAllDebtsByDebtAccount_CodeAndActiveTrue(String debtAccountCode);

    List<DebtEntity> findAllByDebtAccount_FinancialProvider_DebtSysUser_Email(String email);
}
