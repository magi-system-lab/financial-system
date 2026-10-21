package com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.model;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Setter
@Getter
public class ReceivablePayment implements Serializable {
    private Integer id;
    private Instant createdAt;
    private LocalDate paymentDate;
    private BigDecimal amount;
    private String note;
    private Receivable receivable;
}
