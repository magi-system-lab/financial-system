package com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Setter
@Getter
public class ReceivableHistoryDto {
    private ReceivableStatusDto status;
    // Chronological (oldest first), with the running balance after each payment
    private List<HistoryEntry> entries;
    // One bucket per month from the lent month up to the latest payment (or today), zero-filled
    private List<MonthlyTotal> monthlyTotals;
    private Integer paymentCount;
    private BigDecimal averagePayment;
    private LocalDate firstPaymentDate;
    private LocalDate lastPaymentDate;
    private Long daysSinceLastPayment;

    @Setter
    @Getter
    public static class HistoryEntry {
        private Integer paymentId;
        private LocalDate paymentDate;
        private BigDecimal amount;
        private String note;
        private BigDecimal cumulativeRepaid;
        private BigDecimal remainingBalance;
    }

    @Setter
    @Getter
    public static class MonthlyTotal {
        private String month; // yyyy-MM
        private BigDecimal amount;
        private Integer paymentCount;
    }
}
