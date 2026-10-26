package com.jimm0063.magi.debt.management.debtmanagementltesystem.application.service;

import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.application.port.out.DebtRepository;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.dto.DebtActivityDto;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.model.Debt;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DebtActivityServiceTest {

    private static final Clock OCT_26 = Clock.fixed(Instant.parse("2026-10-26T12:00:00Z"), ZoneOffset.UTC);

    private static Debt debt(int id, String hash, String desc, String monthly, int current, int max,
                             boolean active, String created, String updated) {
        Debt d = new Debt();
        d.setId(id);
        d.setHashSum(hash);
        d.setDescription(desc);
        d.setMonthlyPayment(new BigDecimal(monthly));
        d.setCurrentInstallment(current);
        d.setMaxFinancingTerm(max);
        d.setActive(active);
        d.setCreatedAt(Instant.parse(created + "T10:00:00Z"));
        d.setUpdatedAt(Instant.parse(updated + "T10:00:00Z"));
        return d;
    }

    @Test
    void counts_first_appearance_as_added_and_fully_paid_as_finished() {
        DebtRepository repo = mock(DebtRepository.class);
        when(repo.findDebtHistoryByUser("me")).thenReturn(List.of(
                // TV bought in August; a "rebuild from 0" in October re-inserted it (same hash) → not new again
                debt(1, "tv", "TV", "500", 2, 12, false, "2026-08-03", "2026-10-02"),
                debt(9, "tv", "TV", "500", 3, 12, true, "2026-10-02", "2026-10-02"),
                // Laptop bought in October
                debt(2, "laptop", "Laptop", "1200", 1, 18, true, "2026-10-05", "2026-10-05"),
                // Fridge paid off in October
                debt(3, "fridge", "Fridge", "890", 12, 12, false, "2025-10-01", "2026-10-10"),
                // Phone deleted in October with installments left → not "finished"
                debt(4, "phone", "Phone", "300", 4, 12, false, "2026-06-01", "2026-10-11")));

        List<DebtActivityDto> activity = new DebtActivityService(repo, OCT_26).getActivity("me", 3);

        assertThat(activity).extracting(DebtActivityDto::month)
                .containsExactly(YearMonth.of(2026, 8), YearMonth.of(2026, 9), YearMonth.of(2026, 10));
        DebtActivityDto aug = activity.get(0);
        assertThat(aug.added()).extracting(Debt::getDescription).containsExactly("TV");
        assertThat(activity.get(1).isEmpty()).isTrue();
        DebtActivityDto oct = activity.get(2);
        assertThat(oct.added()).extracting(Debt::getDescription).containsExactly("Laptop");
        assertThat(oct.addedMonthly()).isEqualByComparingTo("1200");
        assertThat(oct.finished()).extracting(Debt::getDescription).containsExactly("Fridge");
        assertThat(oct.freedMonthly()).isEqualByComparingTo("890");
    }
}
