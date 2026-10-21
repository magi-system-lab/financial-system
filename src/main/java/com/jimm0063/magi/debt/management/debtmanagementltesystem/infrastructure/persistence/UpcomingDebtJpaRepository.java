package com.jimm0063.magi.debt.management.debtmanagementltesystem.infrastructure.persistence;

import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.enums.UpcomingDebtStatus;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.infrastructure.entity.UpcomingDebtEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UpcomingDebtJpaRepository extends JpaRepository<UpcomingDebtEntity, Integer> {
    List<UpcomingDebtEntity> findAllByDebtSysUser_EmailAndActiveTrueOrderByFirstPaymentDateAsc(String email);
    List<UpcomingDebtEntity> findAllByDebtSysUser_EmailAndStatusAndActiveTrueOrderByFirstPaymentDateAsc(String email, UpcomingDebtStatus status);
    List<UpcomingDebtEntity> findAllByDebtAccount_CodeAndStatusAndActiveTrueOrderByFirstPaymentDateAsc(String debtAccountCode, UpcomingDebtStatus status);
}
