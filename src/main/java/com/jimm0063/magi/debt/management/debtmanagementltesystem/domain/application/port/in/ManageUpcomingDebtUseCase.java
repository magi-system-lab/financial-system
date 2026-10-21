package com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.application.port.in;

import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.model.UpcomingDebt;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;

public interface ManageUpcomingDebtUseCase {
    UpcomingDebt create(String userEmail, UpcomingDebtCommand command);
    UpcomingDebt update(Integer upcomingDebtId, UpcomingDebtCommand command);
    /** The purchase is now on a statement / being charged — take it off the upcoming pile. */
    UpcomingDebt markAsStarted(Integer upcomingDebtId);
    UpcomingDebt cancel(Integer upcomingDebtId);
    /** Put a started/cancelled item back on the pile (undo). */
    UpcomingDebt reopen(Integer upcomingDebtId);
    void delete(Integer upcomingDebtId);

    record UpcomingDebtCommand(
            String description,
            String debtAccountCode,
            BigDecimal totalAmount,
            BigDecimal monthlyPayment,
            Integer installments,
            LocalDate purchaseDate,
            YearMonth firstPaymentMonth,
            String debtType) {
    }
}
