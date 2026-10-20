package com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.dto;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;

/** Month-by-month view of what will be charged, including upcoming (deferred) purchases. */
@Setter
@Getter
public class PaymentProjectionDto implements Serializable {
    private Double salary;
    private List<MonthProjectionDto> months;
    /** Highest totalCommitments across the projected months. */
    private double peakCommitments;
    private MonthProjectionDto peakMonth;
}
