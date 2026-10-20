package com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.dto;

import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.model.DebtAccount;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.model.FixedExpense;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
public class UserStatusDashboard {
    private Double salary;
    private Double savings;
    private Double monthlyDebtPaymentAmount;
    private Double debtLoanAmount;
    private Double debtForLifePlanAmount;
    private Double monthlyFixedExpensesAmount;
    private Double totalMonthlyDebt;
    private Double availableIncome;
    private List<String> chartAccountLabels;
    private List<Double> chartAccountAmounts;
    private List<DebtAccount> userDebtAccounts;
    private List<AlmostCompletedDebtsDto> almostCompletedDebts;
    private List<FixedExpense> userFixedExpenses;

    // Insight fields
    private Double debtToIncomeRatio;
    private Integer debtFreeMonths;
    private Double totalEstimatedRemainingBalance;
    private Double almostCompletedMonthlyRelief;
    private List<AccountSummaryDto> accountSummaries;
    private List<CategoryAmountDto> fixedExpenseBreakdownByCategory;
    private Double fixedExpensePercentOfIncome;
    private Integer fixedExpenseCount;

    // Salary & savings insights
    private Double savingsRunwayMonths;
    private Double disposableIncomeRate;

    // Receivable insights (money lent out, e.g. to relatives)
    private Double totalPendingReceivables;
    private Integer activeReceivableCount;
    private List<ReceivableStatusDto> userReceivables;

    // Upcoming (deferred) purchases — NOT included in the totals above, which only cover debts already charging
    private Integer upcomingDebtCount;
    private Double upcomingMonthlyTotal;
    private String nextUpcomingStart;
    private Integer dueUpcomingCount;
}
