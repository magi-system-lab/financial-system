package com.jimm0063.magi.debt.management.debtmanagementltesystem.infrastructure.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.io.Serializable;
import java.time.Instant;
import java.util.List;

@Table(name = "debt_sys_user")
@Entity
@Setter
@Getter
public class DebtSysUserEntity implements Serializable {
    @Id
    private String email;
    private String name;
    private Double salary;
    private Double savings;
    @Column(name = "debt_limit_percent")
    private Integer debtLimitPercent;
    @CreationTimestamp
    @Column(name = "created_at")
    private Instant createdAt;
    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
    private Boolean active;
    @OneToMany(mappedBy = "debtSysUser")
    private List<FixedExpenseEntity> fixedExpenses;
    @OneToMany(mappedBy = "debtSysUser", fetch = FetchType.EAGER)
    private List<FinancialProviderEntity> financialProviders;
}