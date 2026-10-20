package com.jimm0063.magi.debt.management.debtmanagementltesystem.application.service;

import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.application.port.in.FindAllUpcomingDebtsUseCase;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.application.port.in.GetPaymentProjectionUseCase;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.application.port.in.ManageUpcomingDebtUseCase;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.application.port.out.DebtRepository;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.application.port.out.FixedExpenseRepository;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.application.port.out.UpcomingDebtRepository;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.application.port.out.UserRepository;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.dto.MonthProjectionDto;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.dto.PaymentProjectionDto;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.enums.DebtTypeEnum;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.enums.UpcomingDebtStatus;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.exceptions.EntityNotFoundException;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.model.Debt;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.model.DebtSysUser;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.model.FixedExpense;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.model.UpcomingDebt;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Upcoming debts: purchases already made whose first installment is charged in a future month.
 * They live in their own table and never touch the debt table; they only feed the payment
 * projection so future months don't come as a surprise. Once the purchase shows up on a
 * statement (and is imported as a regular debt), the user marks it as STARTED.
 */
@Service
public class UpcomingDebtService implements FindAllUpcomingDebtsUseCase, ManageUpcomingDebtUseCase, GetPaymentProjectionUseCase {
    private final UpcomingDebtRepository upcomingDebtRepository;
    private final DebtRepository debtRepository;
    private final FixedExpenseRepository fixedExpenseRepository;
    private final UserRepository userRepository;

    public UpcomingDebtService(UpcomingDebtRepository upcomingDebtRepository, DebtRepository debtRepository,
                               FixedExpenseRepository fixedExpenseRepository, UserRepository userRepository) {
        this.upcomingDebtRepository = upcomingDebtRepository;
        this.debtRepository = debtRepository;
        this.fixedExpenseRepository = fixedExpenseRepository;
        this.userRepository = userRepository;
    }

    // ── Queries ───────────────────────────────────────────────────────────────

    @Override
    public List<UpcomingDebt> getByEmail(String email) {
        return upcomingDebtRepository.findAllActiveByUser(email);
    }

    @Override
    public List<UpcomingDebt> getPendingByEmail(String email) {
        return upcomingDebtRepository.findAllPendingByUser(email);
    }

    @Override
    public List<UpcomingDebt> getPendingByDebtAccount(String debtAccountCode) {
        return upcomingDebtRepository.findAllPendingByDebtAccount(debtAccountCode);
    }

    // ── Commands ──────────────────────────────────────────────────────────────

    @Override
    public UpcomingDebt create(String userEmail, UpcomingDebtCommand command) {
        UpcomingDebt upcomingDebt = new UpcomingDebt();
        apply(upcomingDebt, command);
        upcomingDebt.setStatus(UpcomingDebtStatus.PENDING);
        return upcomingDebtRepository.save(upcomingDebt, userEmail, command.debtAccountCode());
    }

    @Override
    public UpcomingDebt update(Integer upcomingDebtId, UpcomingDebtCommand command) {
        UpcomingDebt upcomingDebt = find(upcomingDebtId);
        apply(upcomingDebt, command);
        return upcomingDebtRepository.update(upcomingDebt, command.debtAccountCode());
    }

    @Override
    public UpcomingDebt markAsStarted(Integer upcomingDebtId) {
        return changeStatus(upcomingDebtId, UpcomingDebtStatus.STARTED, Instant.now());
    }

    @Override
    public UpcomingDebt cancel(Integer upcomingDebtId) {
        return changeStatus(upcomingDebtId, UpcomingDebtStatus.CANCELLED, null);
    }

    @Override
    public UpcomingDebt reopen(Integer upcomingDebtId) {
        return changeStatus(upcomingDebtId, UpcomingDebtStatus.PENDING, null);
    }

    @Override
    public void delete(Integer upcomingDebtId) {
        upcomingDebtRepository.delete(upcomingDebtId);
    }

    private UpcomingDebt changeStatus(Integer upcomingDebtId, UpcomingDebtStatus status, Instant startedAt) {
        UpcomingDebt upcomingDebt = find(upcomingDebtId);
        upcomingDebt.setStatus(status);
        upcomingDebt.setStartedAt(startedAt);
        String accountCode = upcomingDebt.getDebtAccount() != null ? upcomingDebt.getDebtAccount().getCode() : null;
        return upcomingDebtRepository.update(upcomingDebt, accountCode);
    }

    private UpcomingDebt find(Integer upcomingDebtId) {
        return upcomingDebtRepository.findById(upcomingDebtId)
                .orElseThrow(() -> new EntityNotFoundException("Upcoming debt " + upcomingDebtId + " not found"));
    }

    /** Validates the command and fills in whichever of total / monthly amount was left blank. */
    private void apply(UpcomingDebt target, UpcomingDebtCommand command) {
        if (command.description() == null || command.description().isBlank())
            throw new IllegalArgumentException("Description is required");
        if (command.installments() == null || command.installments() < 1)
            throw new IllegalArgumentException("Installments must be at least 1");
        if (command.firstPaymentMonth() == null)
            throw new IllegalArgumentException("First payment month is required");

        BigDecimal installments = BigDecimal.valueOf(command.installments());
        BigDecimal total = positiveOrNull(command.totalAmount());
        BigDecimal monthly = positiveOrNull(command.monthlyPayment());
        if (total == null && monthly == null)
            throw new IllegalArgumentException("Enter the total amount or the monthly payment");
        if (monthly == null) monthly = total.divide(installments, 2, RoundingMode.HALF_UP);
        if (total == null) total = monthly.multiply(installments).setScale(2, RoundingMode.HALF_UP);

        target.setDescription(command.description().trim());
        target.setTotalAmount(total);
        target.setMonthlyPayment(monthly);
        target.setInstallments(command.installments());
        target.setPurchaseDate(command.purchaseDate());
        target.setFirstPaymentDate(command.firstPaymentMonth().atDay(1));
        target.setDebtType(command.debtType() == null || command.debtType().isBlank()
                ? DebtTypeEnum.CARD : DebtTypeEnum.valueOf(command.debtType()));
    }

    private static BigDecimal positiveOrNull(BigDecimal value) {
        return value != null && value.compareTo(BigDecimal.ZERO) > 0 ? value : null;
    }

    // ── Projection ────────────────────────────────────────────────────────────

    @Override
    public PaymentProjectionDto getProjection(String email, int months) {
        DebtSysUser user = userRepository.findUserByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("User " + email + " not found"));
        List<Debt> debts = debtRepository.findAllDebtsByUser(email);
        List<FixedExpense> fixedExpenses = fixedExpenseRepository.findAllFixedExpenseBySystemUserAndActiveTrue(user);
        List<UpcomingDebt> upcoming = upcomingDebtRepository.findAllPendingByUser(email);
        return buildProjection(YearMonth.now(), months, user.getSalary(), debts, fixedExpenses, upcoming);
    }

    /**
     * Month 0 is the current month and matches the dashboard's totals. Same rule as the
     * progression tab: a debt at installment c of n is still charged in each of the next
     * (n - c) months; debts without a term (e.g. for-life plans) are assumed to keep running.
     */
    PaymentProjectionDto buildProjection(YearMonth start, int months, Double salary,
                                         List<Debt> debts, List<FixedExpense> fixedExpenses,
                                         List<UpcomingDebt> upcoming) {
        int horizon = Math.max(1, Math.min(months, 60));
        double fixed = fixedExpenses.stream()
                .filter(e -> e.getMonthlyCost() != null)
                .mapToDouble(e -> e.getMonthlyCost().doubleValue())
                .sum();
        List<Debt> countedDebts = debts.stream()
                .filter(d -> d.getDebtType() != null && d.getMonthlyPayment() != null)
                .toList();
        List<UpcomingDebt> pending = upcoming.stream()
                .filter(UpcomingDebt::isPending)
                .filter(u -> u.getMonthlyPayment() != null && u.getFirstPaymentDate() != null)
                .toList();

        List<MonthProjectionDto> result = new ArrayList<>();
        Double previousCurrent = null;
        for (int k = 0; k < horizon; k++) {
            final int offset = k;
            YearMonth month = start.plusMonths(k);

            double current = countedDebts.stream()
                    .filter(d -> stillRunning(d, offset))
                    .mapToDouble(d -> d.getMonthlyPayment().doubleValue())
                    .sum();
            double upcomingAmount = pending.stream()
                    .filter(u -> u.chargedIn(month))
                    .mapToDouble(u -> u.getMonthlyPayment().doubleValue())
                    .sum();

            MonthProjectionDto dto = new MonthProjectionDto();
            dto.setMonth(month);
            dto.setCurrentDebts(round(current));
            dto.setUpcomingDebts(round(upcomingAmount));
            dto.setFixedExpenses(round(fixed));
            dto.setTotalCommitments(round(current + upcomingAmount + fixed));
            dto.setAvailableIncome(salary != null ? round(salary - current - upcomingAmount - fixed) : null);
            dto.setRelief(previousCurrent != null ? round(Math.max(previousCurrent - current, 0)) : 0);
            dto.setStartingThisMonth(pending.stream()
                    .filter(u -> month.equals(u.firstPaymentMonth()))
                    .map(UpcomingDebt::getDescription)
                    .toList());
            result.add(dto);
            previousCurrent = current;
        }

        PaymentProjectionDto projection = new PaymentProjectionDto();
        projection.setSalary(salary);
        projection.setMonths(result);
        result.stream()
                .max(Comparator.comparingDouble(MonthProjectionDto::getTotalCommitments))
                .ifPresent(peak -> {
                    projection.setPeakMonth(peak);
                    projection.setPeakCommitments(peak.getTotalCommitments());
                });
        return projection;
    }

    private static boolean stillRunning(Debt debt, int monthOffset) {
        if (debt.getCurrentInstallment() == null || debt.getMaxFinancingTerm() == null) return true;
        return monthOffset <= debt.getMaxFinancingTerm() - debt.getCurrentInstallment();
    }

    private static double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
