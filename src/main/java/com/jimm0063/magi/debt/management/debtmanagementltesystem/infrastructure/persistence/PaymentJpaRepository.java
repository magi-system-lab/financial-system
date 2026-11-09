package com.jimm0063.magi.debt.management.debtmanagementltesystem.infrastructure.persistence;

import com.jimm0063.magi.debt.management.debtmanagementltesystem.infrastructure.entity.DebtAccountEntity;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.infrastructure.entity.PaymentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentJpaRepository extends JpaRepository<PaymentEntity, Integer> {

    /**
     * A payment without its backup_data: that column is a PostgreSQL large object (oid) that nothing reads back,
     * and reading it fails when the object no longer exists (e.g. after a dump/restore without large objects).
     */
    interface PaymentRow {
        int getId();
        LocalDateTime getCreatedAt();
        BigDecimal getAmountPaid();
        DebtAccountEntity getDebtAccount();
    }

    List<PaymentRow> findRowsByDebtAccount_Code(String debtAccountCode);

    Optional<PaymentRow> findFirstRowByDebtAccount_CodeOrderByCreatedAtDesc(String debtAccountCode);
}
