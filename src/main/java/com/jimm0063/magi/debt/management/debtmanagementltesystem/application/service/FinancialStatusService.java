package com.jimm0063.magi.debt.management.debtmanagementltesystem.application.service;

import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.application.port.in.GetFinancialStatusUseCase;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.application.port.in.GetReceivableStatusUseCase;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.application.port.out.*;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.dto.AccountSummaryDto;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.dto.AlmostCompletedDebtsDto;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.dto.CategoryAmountDto;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.dto.ReceivableStatusDto;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.dto.UserStatusDashboard;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.enums.DebtTypeEnum;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.exceptions.EntityNotFoundException;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.model.Debt;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.model.DebtAccount;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.model.DebtSysUser;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.model.FixedExpense;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.model.UpcomingDebt;
import org.springframework.stereotype.Service;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class FinancialStatusService implements GetFinancialStatusUseCase {
    private final UserRepository userRepository;
    private final FinancialProviderRepository financialProviderRepository;
    private final DebtAccountRepository debtAccountRepository;
    private final DebtRepository debtRepository;
    private final FixedExpenseRepository fixedExpenseRepository;
    private final GetReceivableStatusUseCase getReceivableStatusUseCase;
    private final UpcomingDebtRepository upcomingDebtRepository;

    public FinancialStatusService(UserRepository userRepository, FinancialProviderRepository financialProviderRepository,
                                  DebtAccountRepository debtAccountRepository, DebtRepository debtRepository,
                                  FixedExpenseRepository fixedExpenseRepository, GetReceivableStatusUseCase getReceivableStatusUseCase,
                                  UpcomingDebtRepository upcomingDebtRepository) {
        this.userRepository = userRepository;
        this.financialProviderRepository = financialProviderRepository;
        this.debtAccountRepository = debtAccountRepository;
        this.debtRepository = debtRepository;
        this.fixedExpenseRepository = fixedExpenseRepository;
        this.getReceivableStatusUseCase = getReceivableStatusUseCase;
        this.upcomingDebtRepository = upcomingDebtRepository;
    }

    @Override
    public UserStatusDashboard getUserStatus(String email) {
        DebtSysUser user = this.userRepository.findUserByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("User " + email + " not found"));

        List<DebtAccount> userDebtAccounts = this.financialProviderRepository.findAllBySystemUser(user).stream()
                .map(debtAccountRepository::findAllByFinancialProvider)
                .flatMap(Collection::stream)
                .toList();

        List<Debt> userDebts = userDebtAccounts.stream()
                .map(DebtAccount::getCode)
                .map(debtRepository::findAllDebtsByDebtAccountAndActiveTrue)
                .flatMap(Collection::stream)
                .toList();

        //Get almost completed debts
        List<AlmostCompletedDebtsDto> almostCompletedDebts = userDebts.stream()
                .filter(debt -> debt.getCurrentInstallment() != null
                        && debt.getMaxFinancingTerm() != null
                        && (debt.getCurrentInstallment() + 1) >= debt.getMaxFinancingTerm())
                .map(debt -> {
                    AlmostCompletedDebtsDto almostCompletedDebtsDto = new AlmostCompletedDebtsDto();
                    almostCompletedDebtsDto.setCode(debt.getDebtAccount().getCode());
                    almostCompletedDebtsDto.setDescription(debt.getDescription());
                    almostCompletedDebtsDto.setMonthlyPayment(debt.getMonthlyPayment());
                    almostCompletedDebtsDto.setCurrentInstallment(debt.getCurrentInstallment());
                    almostCompletedDebtsDto.setMaxFinancingTerm(debt.getMaxFinancingTerm());

                    return almostCompletedDebtsDto;
                })
                .toList();

        //Getting Fixed Expenses
        List<FixedExpense> userFixedExpenses = fixedExpenseRepository.findAllFixedExpenseBySystemUserAndActiveTrue(user);

        // calculate amount — guard against null debtType or null monthlyPayment
        Double debtMonthAmount = userDebts.stream()
                .filter(debt -> debt.getDebtType() != null && debt.getMonthlyPayment() != null)
                .filter(debt -> debt.getDebtType() == DebtTypeEnum.CARD || debt.getDebtType() == DebtTypeEnum.PEOPLE)
                .mapToDouble(debt -> debt.getMonthlyPayment().doubleValue())
                .sum();

        Double debtLoanAmount = userDebts.stream()
                .filter(debt -> debt.getDebtType() == DebtTypeEnum.LOAN && debt.getMonthlyPayment() != null)
                .mapToDouble(debt -> debt.getMonthlyPayment().doubleValue())
                .sum();

        Double debtForLifePlanAmount = userDebts.stream()
                .filter(debt -> debt.getDebtType() == DebtTypeEnum.FOR_LIFE_PLAN && debt.getMonthlyPayment() != null)
                .mapToDouble(debt -> debt.getMonthlyPayment().doubleValue())
                .sum();

        Double fixedExpensesMonthAmount = userFixedExpenses.stream()
                .filter(e -> e.getMonthlyCost() != null)
                .mapToDouble(fixedExpense -> fixedExpense.getMonthlyCost().doubleValue())
                .sum();

        Double totalMonthlyDebt = debtMonthAmount + debtLoanAmount + debtForLifePlanAmount;
        Double availableIncome = (user.getSalary() != null ? user.getSalary() : 0.0)
                - totalMonthlyDebt - fixedExpensesMonthAmount;

        // per-account chart data
        Map<String, Double> paymentByCode = userDebts.stream()
                .filter(d -> d.getMonthlyPayment() != null && d.getDebtAccount() != null)
                .collect(Collectors.groupingBy(
                        d -> d.getDebtAccount().getCode(),
                        Collectors.summingDouble(d -> d.getMonthlyPayment().doubleValue())));

        List<String> chartLabels = new ArrayList<>();
        List<Double> chartAmounts = new ArrayList<>();
        for (DebtAccount acc : userDebtAccounts) {
            double amount = paymentByCode.getOrDefault(acc.getCode(), 0.0);
            if (amount > 0) {
                chartLabels.add(acc.getName() != null ? acc.getName() : acc.getCode());
                chartAmounts.add(Math.round(amount * 100.0) / 100.0);
            }
        }

        // ── Insight fields ────────────────────────────────────────────────────────

        // Group active debts by account code
        Map<String, List<Debt>> debtsByAccount = userDebts.stream()
                .filter(d -> d.getDebtAccount() != null)
                .collect(Collectors.groupingBy(d -> d.getDebtAccount().getCode()));

        double userSavings = user.getSavings() != null ? user.getSavings() : 0.0;

        // Per-account summaries (only accounts with active debts)
        List<AccountSummaryDto> accountSummaries = userDebtAccounts.stream()
                .map(acc -> {
                    List<Debt> accDebts = debtsByAccount.getOrDefault(acc.getCode(), List.of());
                    if (accDebts.isEmpty()) return null;

                    double monthly = accDebts.stream()
                            .filter(d -> d.getMonthlyPayment() != null)
                            .mapToDouble(d -> d.getMonthlyPayment().doubleValue()).sum();

                    double remaining = accDebts.stream()
                            .filter(d -> d.getMonthlyPayment() != null
                                    && d.getCurrentInstallment() != null
                                    && d.getMaxFinancingTerm() != null)
                            .mapToDouble(d -> (d.getMaxFinancingTerm() - d.getCurrentInstallment())
                                    * d.getMonthlyPayment().doubleValue())
                            .sum();

                    int maxMonths = accDebts.stream()
                            .filter(d -> d.getCurrentInstallment() != null && d.getMaxFinancingTerm() != null)
                            .mapToInt(d -> d.getMaxFinancingTerm() - d.getCurrentInstallment())
                            .max().orElse(0);

                    AccountSummaryDto dto = new AccountSummaryDto();
                    dto.setCode(acc.getCode());
                    dto.setName(acc.getName() != null ? acc.getName() : acc.getCode());
                    dto.setPayDay(acc.getPayDay());
                    dto.setCreditLimit(acc.getCredit());
                    dto.setActiveDebtCount(accDebts.size());
                    dto.setMonthlyTotal(Math.round(monthly * 100.0) / 100.0);
                    dto.setEstimatedRemainingBalance(Math.round(remaining * 100.0) / 100.0);
                    dto.setMaxMonthsRemaining(maxMonths);
                    dto.setPayoffFeasible(remaining > 0 && userSavings >= remaining);
                    dto.setSavingsCoveragePercent(remaining > 0
                            ? Math.min(userSavings / remaining * 100.0, 100.0) : 100.0);
                    return dto;
                })
                .filter(Objects::nonNull)
                .toList();

        // Fixed expense breakdown by category
        Map<String, Double> fixedExpenseByCategory = userFixedExpenses.stream()
                .filter(e -> e.getMonthlyCost() != null && e.getFixedExpenseCatalog() != null)
                .collect(Collectors.groupingBy(e -> e.getFixedExpenseCatalog().getName(),
                        Collectors.summingDouble(e -> e.getMonthlyCost().doubleValue())));

        List<CategoryAmountDto> fixedExpenseBreakdown = fixedExpenseByCategory.entrySet().stream()
                .map(en -> {
                    CategoryAmountDto dto = new CategoryAmountDto();
                    dto.setCategory(en.getKey());
                    dto.setAmount(Math.round(en.getValue() * 100.0) / 100.0);
                    return dto;
                })
                .sorted(Comparator.comparing(CategoryAmountDto::getAmount).reversed())
                .toList();

        double salary = user.getSalary() != null ? user.getSalary() : 0.0;
        double totalCommitments = totalMonthlyDebt + fixedExpensesMonthAmount;
        Double fixedExpensePercentOfIncome = salary > 0
                ? Math.round((fixedExpensesMonthAmount / salary) * 10000.0) / 100.0
                : null;

        // How many months of total commitments the current savings could cover
        Double savingsRunwayMonths = totalCommitments > 0
                ? Math.round((userSavings / totalCommitments) * 10.0) / 10.0
                : null;

        // Share of salary left after all commitments (can be negative if over-committed)
        Double disposableIncomeRate = salary > 0
                ? Math.round((availableIncome / salary) * 10000.0) / 100.0
                : null;
        Double dti = salary > 0
                ? Math.round((totalCommitments / salary) * 10000.0) / 100.0
                : null;

        int debtFreeMonths = userDebts.stream()
                .filter(d -> d.getCurrentInstallment() != null && d.getMaxFinancingTerm() != null)
                .mapToInt(d -> d.getMaxFinancingTerm() - d.getCurrentInstallment())
                .max().orElse(0);

        double totalRemainingBalance = accountSummaries.stream()
                .mapToDouble(AccountSummaryDto::getEstimatedRemainingBalance).sum();

        double almostCompletedRelief = almostCompletedDebts.stream()
                .filter(d -> d.getMonthlyPayment() != null)
                .mapToDouble(d -> d.getMonthlyPayment().doubleValue()).sum();

        // Money lent out to others (e.g. relatives) that is still pending repayment
        List<ReceivableStatusDto> userReceivables = getReceivableStatusUseCase.getAllStatusesByEmail(email);
        List<ReceivableStatusDto> activeReceivables = userReceivables.stream()
                .filter(dto -> Boolean.TRUE.equals(dto.getReceivable().getActive()))
                .toList();
        double totalPendingReceivables = activeReceivables.stream()
                .mapToDouble(dto -> dto.getPendingAmount().doubleValue())
                .sum();

        // Upcoming (deferred) purchases — planning info only, kept out of the totals above
        List<UpcomingDebt> pendingUpcoming = upcomingDebtRepository.findAllPendingByUser(email);
        YearMonth currentMonth = YearMonth.now();
        double upcomingMonthly = pendingUpcoming.stream()
                .filter(u -> u.getMonthlyPayment() != null)
                .mapToDouble(u -> u.getMonthlyPayment().doubleValue())
                .sum();
        String nextUpcomingStart = pendingUpcoming.stream()
                .filter(u -> u.firstPaymentMonth() != null && u.firstPaymentMonth().isAfter(currentMonth))
                .min(Comparator.comparing(UpcomingDebt::firstPaymentMonth))
                .map(UpcomingDebt::firstPaymentLabel)
                .orElse(null);

        UserStatusDashboard response = new UserStatusDashboard();
        response.setSalary(user.getSalary());
        response.setSavings(user.getSavings());
        response.setAlmostCompletedDebts(almostCompletedDebts);
        response.setUserFixedExpenses(userFixedExpenses);
        response.setUserDebtAccounts(userDebtAccounts);

        response.setMonthlyDebtPaymentAmount(debtMonthAmount);
        response.setDebtForLifePlanAmount(debtForLifePlanAmount);
        response.setDebtLoanAmount(debtLoanAmount);
        response.setMonthlyFixedExpensesAmount(fixedExpensesMonthAmount);
        response.setTotalMonthlyDebt(totalMonthlyDebt);
        response.setAvailableIncome(availableIncome);
        response.setChartAccountLabels(chartLabels);
        response.setChartAccountAmounts(chartAmounts);

        response.setAccountSummaries(accountSummaries);
        response.setDebtToIncomeRatio(dti);
        response.setDebtFreeMonths(debtFreeMonths);
        response.setTotalEstimatedRemainingBalance(totalRemainingBalance);
        response.setAlmostCompletedMonthlyRelief(Math.round(almostCompletedRelief * 100.0) / 100.0);
        response.setFixedExpenseBreakdownByCategory(fixedExpenseBreakdown);
        response.setFixedExpensePercentOfIncome(fixedExpensePercentOfIncome);
        response.setFixedExpenseCount(userFixedExpenses.size());
        response.setSavingsRunwayMonths(savingsRunwayMonths);
        response.setDisposableIncomeRate(disposableIncomeRate);

        response.setUserReceivables(userReceivables);
        response.setTotalPendingReceivables(Math.round(totalPendingReceivables * 100.0) / 100.0);
        response.setActiveReceivableCount(activeReceivables.size());

        response.setUpcomingDebtCount(pendingUpcoming.size());
        response.setUpcomingMonthlyTotal(Math.round(upcomingMonthly * 100.0) / 100.0);
        response.setNextUpcomingStart(nextUpcomingStart);
        response.setDueUpcomingCount((int) pendingUpcoming.stream().filter(u -> u.isDue(currentMonth)).count());

        return response;
    }
}
