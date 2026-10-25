package com.jimm0063.magi.debt.management.debtmanagementltesystem.infrastructure.entity;

import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.enums.AccountStatementType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.io.Serializable;
import java.time.Instant;
import java.util.List;

@Entity
@Table(name = "debt_account")
@Setter
@Getter
public class DebtAccountEntity implements Serializable {
    @Id
    private String code;
    private String name;
    @Column(name = "pay_day")
    private Integer payDay;
    private Double credit;
    @CreationTimestamp
    private Instant createdAt;
    @UpdateTimestamp
    private Instant updatedAt;
    private Boolean active;
    @Column(name = "account_statement_type")
    @Enumerated(EnumType.STRING)
    private AccountStatementType accountStatementType;
    @Column(length = 7)
    private String color;
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "financial_provider", nullable = false)
    private FinancialProviderEntity financialProvider;
    @OneToMany(mappedBy = "debtAccount", fetch = FetchType.EAGER)
    private List<DebtEntity> debt;
    @OneToMany(mappedBy = "debtAccount")
    private List<PaymentEntity> payments;
}
