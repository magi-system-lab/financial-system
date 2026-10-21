package com.jimm0063.magi.debt.management.debtmanagementltesystem.infrastructure.model;

import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.application.port.in.ManageUpcomingDebtUseCase.UpcomingDebtCommand;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;

/** Either totalAmount or monthlyPayment is enough — the other one is derived from installments. */
@Setter
@Getter
public class CreateUpcomingDebtReq {
    private String description;
    private String debtAccountCode;
    private BigDecimal totalAmount;
    private BigDecimal monthlyPayment;
    private Integer installments;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate purchaseDate;
    /** Format yyyy-MM, e.g. 2027-01. */
    @DateTimeFormat(pattern = "yyyy-MM")
    private YearMonth firstPaymentMonth;
    private String debtType;

    public UpcomingDebtCommand toCommand() {
        return new UpcomingDebtCommand(description, debtAccountCode, totalAmount, monthlyPayment,
                installments, purchaseDate, firstPaymentMonth, debtType);
    }
}
