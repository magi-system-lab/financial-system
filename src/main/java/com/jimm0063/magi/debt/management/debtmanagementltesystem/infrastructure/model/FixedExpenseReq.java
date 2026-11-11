package com.jimm0063.magi.debt.management.debtmanagementltesystem.infrastructure.model;

import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.enums.FixedExpenseKind;
import lombok.Getter;
import lombok.Setter;

@Setter @Getter
public class FixedExpenseReq {
    private Integer id;
    private String name;
    private Double monthlyCost;
    private Integer paymentDay;
    private String description;
    private String userEmail;
    private Integer catalogId;
    /** EXPENSE (default) or CREDIT. */
    private FixedExpenseKind kind;
    /** Month of the last payment "yyyy-MM" (optional). On update: null = keep, "" = remove the end month. */
    private String endMonth;
}
