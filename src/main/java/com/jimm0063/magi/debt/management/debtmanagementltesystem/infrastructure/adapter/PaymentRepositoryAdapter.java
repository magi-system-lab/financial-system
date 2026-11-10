package com.jimm0063.magi.debt.management.debtmanagementltesystem.infrastructure.adapter;

import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.application.port.out.PaymentRepository;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.exceptions.EntityNotFoundException;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.model.Payment;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.infrastructure.entity.PaymentEntity;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.infrastructure.mapper.DebtAccountMapper;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.infrastructure.mapper.PaymentMapper;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.infrastructure.persistence.PaymentJpaRepository;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.infrastructure.persistence.PaymentJpaRepository.PaymentRow;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
public class PaymentRepositoryAdapter implements PaymentRepository {
    private final PaymentJpaRepository paymentJpaRepository;
    private final PaymentMapper paymentMapper;
    private final DebtAccountMapper debtAccountMapper;

    public PaymentRepositoryAdapter(PaymentJpaRepository paymentJpaRepository, PaymentMapper paymentMapper,
                                    DebtAccountMapper debtAccountMapper) {
        this.paymentJpaRepository = paymentJpaRepository;
        this.paymentMapper = paymentMapper;
        this.debtAccountMapper = debtAccountMapper;
    }

    @Override
    public Payment save(Payment payment) {
        PaymentEntity paymentEntity = paymentMapper.toEntity(payment);
        return paymentMapper.toModel(this.paymentJpaRepository.save(paymentEntity));
    }

    // Reads skip backup_data (a PostgreSQL large object nothing reads back): see PaymentRow.
    @Override
    @Transactional(readOnly = true)
    public List<Payment> findByDebtAccountCode(String debtAccountCode) {
        return paymentJpaRepository.findRowsByDebtAccount_Code(debtAccountCode)
                .stream()
                .map(this::toModel)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Payment findLatest(String debtAccountCode) {
        return paymentJpaRepository.findFirstRowByDebtAccount_CodeOrderByCreatedAtDesc(debtAccountCode)
                .map(this::toModel)
                .orElseThrow(()-> new EntityNotFoundException("There is no payments associated to " + debtAccountCode + " debt account"));
    }

    private Payment toModel(PaymentRow row) {
        Payment payment = new Payment();
        payment.setId(row.getId());
        payment.setCreatedAt(row.getCreatedAt());
        payment.setAmountPaid(row.getAmountPaid());
        payment.setDebtAccount(row.getDebtAccount() == null ? null : debtAccountMapper.toModel(row.getDebtAccount()));
        return payment;
    }
}
