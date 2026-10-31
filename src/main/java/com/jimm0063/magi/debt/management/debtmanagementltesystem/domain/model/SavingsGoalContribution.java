package com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.model;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/** Money put into (positive) or taken out of (negative) a savings goal. */
@Setter
@Getter
public class SavingsGoalContribution implements Serializable {
    private Integer id;
    private BigDecimal amount;
    private LocalDate contributionDate;
    private String note;
    private Instant createdAt;
    private Boolean active;
}
