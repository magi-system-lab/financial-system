package com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.model;

import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.enums.DebtTypeEnum;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;

@Setter
@Getter
public class Debt implements Serializable {
    private Integer id;
    private String description;
    private String operationDate;
    private Integer currentInstallment;
    private Integer maxFinancingTerm;
    private BigDecimal originalAmount;
    private BigDecimal monthlyPayment;
    private Instant createdAt;
    private Instant updatedAt;
    private Boolean active;
    private DebtAccount debtAccount;
    private DebtTypeEnum debtType;
    private String hashSum;
    /** Optional yearly rate (e.g. 45.5) for loans that charge interest; null/0 = interest-free installments. */
    private BigDecimal annualInterestRate;
}
