package com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class WishItemTest {

    @Test
    void counts_down_seven_days_from_the_day_it_was_parked() {
        WishItem item = new WishItem();
        item.setCreatedAt(Instant.parse("2026-10-20T23:30:00Z"));

        assertThat(item.daysLeft(LocalDate.of(2026, 10, 20), ZoneOffset.UTC)).isEqualTo(7);
        assertThat(item.daysLeft(LocalDate.of(2026, 10, 24), ZoneOffset.UTC)).isEqualTo(3);
        assertThat(item.daysLeft(LocalDate.of(2026, 10, 27), ZoneOffset.UTC)).isZero();
        assertThat(item.daysLeft(LocalDate.of(2026, 11, 30), ZoneOffset.UTC)).isZero();
    }
}
