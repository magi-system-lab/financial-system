package com.jimm0063.magi.debt.management.debtmanagementltesystem.infrastructure.model;

import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.enums.AccountStatementType;
import lombok.Getter;
import lombok.Setter;

@Setter @Getter
public class CreateDebtAccountReq {
    private String code;
    private String name;
    private Integer payDay;
    private Double credit;
    private Boolean active;
    private AccountStatementType accountStatementType;
    private String color;
}
