package com.jimm0063.magi.debt.management.debtmanagementltesystem.infrastructure.web.view;

import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.dto.AccountSummaryDto;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.enums.AccountStatementType;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.enums.DebtTypeEnum;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.model.Debt;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.model.DebtAccount;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.model.FinancialProvider;

import java.math.BigDecimal;

final class CardsFixtures {

    private CardsFixtures() {}

    static FinancialProvider bbva() {
        FinancialProvider p = new FinancialProvider();
        p.setCode("prov-bbva");
        p.setName("BBVA");
        p.setActive(true);
        return p;
    }

    static DebtAccount azul() {
        DebtAccount a = new DebtAccount();
        a.setCode("acc-azul");
        a.setName("BBVA Azul");
        a.setActive(true);
        a.setPayDay(5);
        a.setCredit(new BigDecimal("60000"));
        a.setAccountStatementType(AccountStatementType.BBVA);
        a.setFinancialProvider(bbva());
        return a;
    }

    static DebtAccount spare() {
        DebtAccount a = new DebtAccount();
        a.setCode("acc-spare");
        a.setName("Spare card");
        a.setActive(true);
        a.setAccountStatementType(AccountStatementType.UNIVERSAL);
        a.setColor("#0F766E");
        return a;
    }

    static AccountSummaryDto azulSummary() {
        AccountSummaryDto s = new AccountSummaryDto();
        s.setCode("acc-azul");
        s.setName("BBVA Azul");
        s.setMonthlyTotal(4773.75);
        s.setEstimatedRemainingBalance(31250.0);
        s.setActiveDebtCount(4);
        s.setMaxMonthsRemaining(8);
        return s;
    }

    static Debt macbook() {
        Debt d = new Debt();
        d.setId(7);
        d.setDescription("MacBook Air");
        d.setMonthlyPayment(new BigDecimal("2083.25"));
        d.setOriginalAmount(new BigDecimal("24999"));
        d.setCurrentInstallment(9);
        d.setMaxFinancingTerm(12);
        d.setDebtType(DebtTypeEnum.CARD);
        d.setDebtAccount(azul());
        return d;
    }
}
