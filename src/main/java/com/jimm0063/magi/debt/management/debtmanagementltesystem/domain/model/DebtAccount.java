package com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.model;

import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.enums.AccountStatementType;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;

@Setter
@Getter
public class DebtAccount implements Serializable {
    private String code;
    private String name;
    private Integer payDay;
    private BigDecimal credit;
    private Instant createdAt;
    private Instant updatedAt;
    private Boolean active;
    private AccountStatementType accountStatementType;
    /** Card colour on the UI as "#RRGGBB"; null = picked automatically from the bank. */
    private String color;
    private FinancialProvider financialProvider;
    // List<Debt> debts,
}
