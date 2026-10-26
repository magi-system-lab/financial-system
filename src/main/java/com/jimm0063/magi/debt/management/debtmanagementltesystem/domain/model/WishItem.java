package com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.model;

import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.enums.WishStatus;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

/**
 * Something the user wants to buy but parks for a few days first ("cool-down list").
 * After {@link #COOL_DOWN_DAYS} days they decide: plan it (it moves to the upcoming pile) or drop it.
 */
@Setter
@Getter
public class WishItem implements Serializable {
    public static final int COOL_DOWN_DAYS = 7;

    private Integer id;
    private String description;
    private BigDecimal price;
    /** Optional: months to pay it in. */
    private Integer installments;
    private WishStatus status;
    private Instant createdAt;
    private Instant decidedAt;
    private Boolean active;

    /** Days still to wait (0 = ready to decide). */
    public long daysLeft(LocalDate today, ZoneId zone) {
        if (createdAt == null) return 0;
        long waited = ChronoUnit.DAYS.between(createdAt.atZone(zone).toLocalDate(), today);
        return Math.max(0, COOL_DOWN_DAYS - waited);
    }

    public boolean isWaiting() {
        return status == WishStatus.WAITING && !Boolean.FALSE.equals(active);
    }
}
