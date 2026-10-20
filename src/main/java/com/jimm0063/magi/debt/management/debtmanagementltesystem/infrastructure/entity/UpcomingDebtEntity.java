package com.jimm0063.magi.debt.management.debtmanagementltesystem.infrastructure.entity;

import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.enums.DebtTypeEnum;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.enums.UpcomingDebtStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Table(name = "upcoming_debt")
@Entity
@Setter
@Getter
public class UpcomingDebtEntity implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    private String description;
    @Column(name = "total_amount")
    private BigDecimal totalAmount;
    @Column(name = "monthly_payment")
    private BigDecimal monthlyPayment;
    private Integer installments;
    @Column(name = "purchase_date")
    private LocalDate purchaseDate;
    @Column(name = "first_payment_date")
    private LocalDate firstPaymentDate;
    @Column(name = "debt_type")
    @Enumerated(EnumType.STRING)
    private DebtTypeEnum debtType;
    @Enumerated(EnumType.STRING)
    private UpcomingDebtStatus status;
    @Column(name = "started_at")
    private Instant startedAt;
    @CreationTimestamp
    @Column(name = "created_at")
    private Instant createdAt;
    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
    private Boolean active;
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "debt_account")
    private DebtAccountEntity debtAccount;
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "debt_sys_user", nullable = false)
    private DebtSysUserEntity debtSysUser;
}
