package com.jimm0063.magi.debt.management.debtmanagementltesystem.application.service;

import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.application.port.out.DebtRepository;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.application.port.out.UpcomingDebtRepository;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.dto.CardProgressionDto;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.dto.MonthProgressionDto;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.enums.DebtTypeEnum;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.enums.UpcomingDebtStatus;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.model.Debt;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.model.DebtAccount;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.model.UpcomingDebt;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class DebtProgressionServiceTest {

    @Mock DebtRepository debtRepository;
    @Mock UpcomingDebtRepository upcomingDebtRepository;

    @InjectMocks DebtProgressionService service;

    private static final YearMonth OCT_2026 = YearMonth.of(2026, 10);

    private DebtAccount account(String code, String name) {
        DebtAccount a = new DebtAccount();
        a.setCode(code);
        a.setName(name);
        return a;
    }

    private Debt debt(DebtAccount account, String monthly, int current, int max) {
        Debt d = new Debt();
        d.setDebtAccount(account);
        d.setDescription("Laptop");
        d.setMonthlyPayment(new BigDecimal(monthly));
        d.setCurrentInstallment(current);
        d.setMaxFinancingTerm(max);
        d.setDebtType(DebtTypeEnum.CARD);
        return d;
    }

    private UpcomingDebt upcoming(DebtAccount account, String monthly, int installments, YearMonth first) {
        UpcomingDebt u = new UpcomingDebt();
        u.setDescription("TV");
        u.setDebtAccount(account);
        u.setMonthlyPayment(new BigDecimal(monthly));
        u.setInstallments(installments);
        u.setFirstPaymentDate(first.atDay(1));
        u.setStatus(UpcomingDebtStatus.PENDING);
        u.setActive(true);
        return u;
    }

    @Test
    void upcoming_debts_are_added_to_their_card_from_their_first_payment_month() {
        DebtAccount visa = account("VISA", "Visa");
        Debt laptop = debt(visa, "500.00", 1, 12);
        UpcomingDebt tv = upcoming(visa, "1000.00", 2, YearMonth.of(2027, 1));

        List<MonthProgressionDto> result = service.buildProgression(OCT_2026, YearMonth.of(2027, 3),
                List.of(laptop), List.of(tv));

        // Nov, Dec, Jan, Feb, Mar
        assertThat(result).extracting(m -> m.totalPayment().doubleValue())
                .containsExactly(500.0, 500.0, 1500.0, 1500.0, 500.0);
        MonthProgressionDto january = result.get(2);
        assertThat(january.totalUpcomingPayment()).isEqualByComparingTo("1000.00");
        assertThat(january.hasStartingUpcomingDebts()).isTrue();
        assertThat(result.get(3).hasStartingUpcomingDebts()).isFalse();
        CardProgressionDto visaJan = january.cards().get(0);
        assertThat(visaJan.upcomingDebts()).extracting(UpcomingDebt::getDescription).containsExactly("TV");
        // laptop: 500 x (11 - 3 + 1) = 4500, TV: 1000 x 2 = 2000
        assertThat(visaJan.remainingAmount()).isEqualByComparingTo("6500.00");
    }

    @Test
    void upcoming_debt_without_card_gets_its_own_group_and_non_pending_are_ignored() {
        UpcomingDebt noCard = upcoming(null, "200.00", 1, YearMonth.of(2026, 11));
        UpcomingDebt started = upcoming(null, "999.00", 3, YearMonth.of(2026, 11));
        started.setStatus(UpcomingDebtStatus.STARTED);

        List<MonthProgressionDto> result = service.buildProgression(OCT_2026, YearMonth.of(2026, 12),
                List.of(), List.of(noCard, started));

        assertThat(result.get(0).totalPayment()).isEqualByComparingTo("200.00");
        assertThat(result.get(0).cards()).extracting(CardProgressionDto::accountCode)
                .containsExactly(DebtProgressionService.NO_CARD_CODE);
        assertThat(result.get(1).cards()).isEmpty();
    }
}
