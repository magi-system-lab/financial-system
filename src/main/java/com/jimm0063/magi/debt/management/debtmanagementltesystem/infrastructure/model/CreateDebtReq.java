package com.jimm0063.magi.debt.management.debtmanagementltesystem.infrastructure.model;

import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.enums.DebtTypeEnum;
import lombok.Getter;
import lombok.Setter;


@Setter @Getter
public class CreateDebtReq {
    private String description;
    private String operationDate;
    private Integer currentInstallment;
    private Integer maxFinancingTerm;
    private Double originalAmount;
    private Double monthlyPayment;
    private Boolean active;
    private DebtTypeEnum debtType;
    private Double annualInterestRate;
}
