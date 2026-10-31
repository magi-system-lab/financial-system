package com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.model;

import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.enums.SavingsGoalStatus;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Money the user puts aside for something: usually paying a debt off, or an emergency fund.
 * When linked to a debt, the target follows the debt's live payoff balance; {@link #targetAmount} keeps the
 * balance at creation as a fallback. The debt is found by id, or by hash after a "rebuild from 0" re-insert.
 */
@Setter
@Getter
public class SavingsGoal implements Serializable {
    private Integer id;
    private String name;
    private BigDecimal targetAmount;
    /** Optional: when it should be reached. */
    private LocalDate targetDate;
    private Integer debtId;
    private String debtHash;
    private SavingsGoalStatus status;
    private Instant createdAt;
    private Instant completedAt;
    private Boolean active;

    public boolean isLinkedToDebt() {
        return debtId != null || debtHash != null;
    }

    public boolean isOpen() {
        return status == SavingsGoalStatus.ACTIVE;
    }
}
