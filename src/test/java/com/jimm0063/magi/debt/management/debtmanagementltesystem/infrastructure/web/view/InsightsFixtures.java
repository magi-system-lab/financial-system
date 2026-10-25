package com.jimm0063.magi.debt.management.debtmanagementltesystem.infrastructure.web.view;

import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.dto.CardProgressionDto;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.dto.MonthProgressionDto;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.model.Debt;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.model.DebtAccount;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

/** Six projected months starting Nov 2026: 1000, 800 (200 upcoming), 800, 500, 0, 0. "Phone" closes in Dec. */
final class InsightsFixtures {

    static final YearMonth NOV_2026 = YearMonth.of(2026, 11);

    private InsightsFixtures() {}

    static Debt phone() {
        DebtAccount visa = new DebtAccount();
        visa.setCode("VISA");
        visa.setName("Visa Oro");
        Debt d = new Debt();
        d.setDescription("Phone");
        d.setMonthlyPayment(new BigDecimal("200.00"));
        d.setCurrentInstallment(10);
        d.setMaxFinancingTerm(12);
        d.setDebtAccount(visa);
        return d;
    }

    static List<MonthProgressionDto> progression() {
        BigDecimal[] totals = {bd("1000"), bd("800"), bd("800"), bd("500"), bd("0"), bd("0")};
        BigDecimal[] upcoming = {bd("0"), bd("200"), bd("0"), bd("0"), bd("0"), bd("0")};
        List<MonthProgressionDto> months = new ArrayList<>();
        for (int i = 0; i < totals.length; i++) {
            List<CardProgressionDto> cards = totals[i].signum() == 0 ? List.of() : List.of(
                    new CardProgressionDto("VISA", "Visa Oro", i == 1 ? List.of(phone()) : List.of(), 2,
                            totals[i], totals[i].multiply(bd("3")), List.of(), upcoming[i]));
            months.add(new MonthProgressionDto(NOV_2026.plusMonths(i), "x", totals[i], cards));
        }
        return months;
    }

    private static BigDecimal bd(String v) {
        return new BigDecimal(v);
    }
}
