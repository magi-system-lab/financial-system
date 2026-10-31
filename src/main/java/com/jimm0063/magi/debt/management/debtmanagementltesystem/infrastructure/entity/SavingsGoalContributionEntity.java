package com.jimm0063.magi.debt.management.debtmanagementltesystem.infrastructure.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Table(name = "savings_goal_contribution")
@Entity
@Setter
@Getter
public class SavingsGoalContributionEntity implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    private BigDecimal amount;
    @Column(name = "contribution_date")
    private LocalDate contributionDate;
    private String note;
    @CreationTimestamp
    @Column(name = "created_at")
    private Instant createdAt;
    private Boolean active;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "savings_goal", nullable = false)
    private SavingsGoalEntity savingsGoal;
}
