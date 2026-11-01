package com.jimm0063.magi.debt.management.debtmanagementltesystem.infrastructure.entity;

import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.enums.SavingsGoalStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Table(name = "savings_goal")
@Entity
@Setter
@Getter
public class SavingsGoalEntity implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    private String name;
    @Column(name = "target_amount")
    private BigDecimal targetAmount;
    @Column(name = "target_date")
    private LocalDate targetDate;
    @Column(name = "debt_id")
    private Integer debtId;
    @Column(name = "debt_hash")
    private String debtHash;
    @Enumerated(EnumType.STRING)
    private SavingsGoalStatus status;
    @CreationTimestamp
    @Column(name = "created_at")
    private Instant createdAt;
    @Column(name = "completed_at")
    private Instant completedAt;
    private Boolean active;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "debt_sys_user", nullable = false)
    private DebtSysUserEntity debtSysUser;
}
