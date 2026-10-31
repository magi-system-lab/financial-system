package com.jimm0063.magi.debt.management.debtmanagementltesystem.application.service;

import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.application.port.in.GetCoachTipsUseCase;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.application.port.in.GetDebtActivityUseCase;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.application.port.in.ManageSavingsGoalUseCase;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.application.port.out.DebtRepository;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.application.port.out.FixedExpenseRepository;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.application.port.out.UpcomingDebtRepository;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.application.port.out.UserRepository;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.application.port.out.WishItemRepository;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.dto.CoachTipDto;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.dto.DebtActivityDto;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.dto.PayoffPlanDto;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.dto.SavingsGoalStatusDto;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.enums.CoachTipSeverity;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.enums.CoachTipType;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.enums.PayoffStrategy;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.model.Debt;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.model.DebtSysUser;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.model.FixedExpense;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.model.UpcomingDebt;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.model.WishItem;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.utils.PayoffSimulator;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Rule-based financial coach: looks at salary, savings, debts, this month's activity, savings goals, the cool-down
 * list and upcoming purchases, and turns what it sees into advice (no AI, nothing leaves the server).
 */
@Service
public class CoachService implements GetCoachTipsUseCase {

    /** Recommended emergency fund, in months of commitments (same goal as the Insights tile). */
    static final int SAFETY_GOAL_MONTHS = 3;

    private final UserRepository userRepository;
    private final DebtRepository debtRepository;
    private final FixedExpenseRepository fixedExpenseRepository;
    private final GetDebtActivityUseCase getDebtActivityUseCase;
    private final ManageSavingsGoalUseCase manageSavingsGoalUseCase;
    private final WishItemRepository wishItemRepository;
    private final UpcomingDebtRepository upcomingDebtRepository;

    public CoachService(UserRepository userRepository, DebtRepository debtRepository,
                        FixedExpenseRepository fixedExpenseRepository, GetDebtActivityUseCase getDebtActivityUseCase,
                        ManageSavingsGoalUseCase manageSavingsGoalUseCase, WishItemRepository wishItemRepository,
                        UpcomingDebtRepository upcomingDebtRepository) {
        this.userRepository = userRepository;
        this.debtRepository = debtRepository;
        this.fixedExpenseRepository = fixedExpenseRepository;
        this.getDebtActivityUseCase = getDebtActivityUseCase;
        this.manageSavingsGoalUseCase = manageSavingsGoalUseCase;
        this.wishItemRepository = wishItemRepository;
        this.upcomingDebtRepository = upcomingDebtRepository;
    }

    /** Everything the rules look at. */
    record Snapshot(LocalDate today, Double salary, Double savings, Integer debtLimitPercent, BigDecimal fixedExpenses,
                    List<Debt> debts, DebtActivityDto thisMonth, List<SavingsGoalStatusDto> goals,
                    long wishesReady, List<UpcomingDebt> pendingUpcoming) {}

    @Override
    public List<CoachTipDto> getTips(String email) {
        DebtSysUser user = userRepository.findUserByEmail(email).orElse(null);
        LocalDate today = LocalDate.now();
        List<DebtActivityDto> activity = getDebtActivityUseCase.getActivity(email, 1);
        BigDecimal fixed = fixedExpenseRepository.findAllFixedExpenseByEmailAndActiveTrue(email).stream()
                .map(FixedExpense::getMonthlyCost).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
        long wishesReady = wishItemRepository.findAllActiveByUser(email).stream()
                .filter(w -> w.isWaiting() && w.daysLeft(today, ZoneId.systemDefault()) == 0).count();
        return tips(new Snapshot(today,
                user != null ? user.getSalary() : null,
                user != null ? user.getSavings() : null,
                user != null ? user.getDebtLimitPercent() : null,
                fixed,
                debtRepository.findAllDebtsByUser(email),
                activity.isEmpty() ? null : activity.get(activity.size() - 1),
                manageSavingsGoalUseCase.getByEmail(email),
                wishesReady,
                upcomingDebtRepository.findAllPendingByUser(email)));
    }

    static List<CoachTipDto> tips(Snapshot s) {
        List<CoachTipDto> tips = new ArrayList<>();
        YearMonth current = YearMonth.from(s.today());
        List<Debt> debts = s.debts().stream().filter(d -> PayoffSimulator.monthsLeft(d) > 0
                && d.getMonthlyPayment() != null).toList();
        BigDecimal nextMonth = paymentsIn(debts, 1);
        BigDecimal monthAfter = paymentsIn(debts, 2);
        BigDecimal salary = s.salary() == null ? null : BigDecimal.valueOf(s.salary());
        BigDecimal savings = BigDecimal.valueOf(s.savings() == null ? 0 : s.savings());
        BigDecimal commitments = s.fixedExpenses().add(nextMonth);

        if (debts.isEmpty()) tips.add(CoachTipDto.of(CoachTipType.DEBT_FREE, CoachTipSeverity.GOOD));

        // ── Budget ──
        if (salary == null || salary.signum() <= 0) {
            tips.add(CoachTipDto.of(CoachTipType.NO_SALARY, CoachTipSeverity.TIP));
        } else {
            BigDecimal free = salary.subtract(s.fixedExpenses()).subtract(nextMonth);
            if (free.signum() < 0) tips.add(CoachTipDto.of(CoachTipType.OVERSPENT, CoachTipSeverity.ALERT, free.negate()));
            int share = nextMonth.multiply(BigDecimal.valueOf(100)).divide(salary, 0, RoundingMode.HALF_UP).intValue();
            if (s.debtLimitPercent() != null && s.debtLimitPercent() > 0 && share > s.debtLimitPercent())
                tips.add(CoachTipDto.of(CoachTipType.OVER_LIMIT, CoachTipSeverity.ALERT, share, s.debtLimitPercent()));
        }

        // ── Safety net before paying anything off early ──
        BigDecimal safetyGoal = commitments.multiply(BigDecimal.valueOf(SAFETY_GOAL_MONTHS));
        if (commitments.signum() > 0 && savings.compareTo(safetyGoal) < 0) {
            double months = savings.divide(commitments, 1, RoundingMode.DOWN).doubleValue();
            tips.add(CoachTipDto.of(CoachTipType.SAFETY_LOW, months < 1 ? CoachTipSeverity.ALERT : CoachTipSeverity.WARN,
                    months, safetyGoal.subtract(savings)));
        }

        debts.stream().filter(PayoffSimulator::chargesInterest)
                .max(Comparator.comparing(Debt::getAnnualInterestRate))
                .ifPresent(d -> tips.add(CoachTipDto.of(CoachTipType.INTEREST_FIRST, CoachTipSeverity.WARN,
                        d.getDescription(), d.getAnnualInterestRate().stripTrailingZeros().toPlainString())));

        // ── This month ──
        DebtActivityDto month = s.thisMonth();
        if (month != null && month.addedMonthly().signum() > 0 && month.addedMonthly().compareTo(month.freedMonthly()) > 0)
            tips.add(CoachTipDto.of(CoachTipType.NEW_OUTPACING, CoachTipSeverity.WARN, month.addedMonthly(), month.freedMonthly()));

        // ── Savings goals ──
        List<SavingsGoalStatusDto> open = s.goals().stream().filter(g -> g.goal().isOpen()).toList();
        for (SavingsGoalStatusDto g : open) {
            String name = g.goal().getName();
            if (g.canPayOffDebt()) tips.add(CoachTipDto.of(CoachTipType.GOAL_READY_PAYOFF, CoachTipSeverity.GOOD, name, g.target()));
            else if (g.debtGone()) tips.add(CoachTipDto.of(CoachTipType.GOAL_DEBT_GONE, CoachTipSeverity.TIP, name));
            else if (g.reached()) tips.add(CoachTipDto.of(CoachTipType.GOAL_REACHED, CoachTipSeverity.GOOD, name));
            else if (g.behind() && g.suggestedMonthly() != null)
                tips.add(CoachTipDto.of(CoachTipType.GOAL_BEHIND, CoachTipSeverity.WARN, name, g.suggestedMonthly()));
        }

        if (month != null && month.freedMonthly().signum() > 0)
            tips.add(CoachTipDto.of(CoachTipType.FREED_THIS_MONTH, CoachTipSeverity.GOOD, month.freedMonthly()));
        BigDecimal relief = nextMonth.subtract(monthAfter);
        if (relief.signum() > 0)
            tips.add(CoachTipDto.of(CoachTipType.RELIEF_NEXT, CoachTipSeverity.GOOD, current.plusMonths(2), relief));

        // ── Paying off ──
        BigDecimal spare = savings.subtract(safetyGoal);
        debts.stream().filter(PayoffSimulator::worthPayingOff)
                .min(Comparator.comparing(PayoffSimulator::payoffBalance))
                .filter(d -> spare.compareTo(PayoffSimulator.payoffBalance(d)) >= 0)
                .ifPresent(d -> tips.add(CoachTipDto.of(CoachTipType.QUICK_WIN, CoachTipSeverity.TIP,
                        d.getDescription(), PayoffSimulator.payoffBalance(d), d.getMonthlyPayment())));
        if (!debts.isEmpty()) {
            PayoffPlanDto rollOnly = PayoffSimulator.simulate(debts, current, BigDecimal.ZERO, BigDecimal.ZERO,
                    PayoffStrategy.LONGEST, true);
            if (rollOnly.monthsSaved() > 0)
                tips.add(CoachTipDto.of(CoachTipType.SNOWBALL_ONLY, CoachTipSeverity.TIP, rollOnly.monthsSaved(), rollOnly.planDebtFree()));
            if (open.isEmpty()) tips.add(CoachTipDto.of(CoachTipType.NO_GOALS, CoachTipSeverity.TIP));
        }

        // ── Purchases ──
        if (s.wishesReady() > 0) tips.add(CoachTipDto.of(CoachTipType.WISH_READY, CoachTipSeverity.TIP, s.wishesReady()));
        List<UpcomingDebt> startingNext = s.pendingUpcoming().stream()
                .filter(u -> current.plusMonths(1).equals(u.firstPaymentMonth()) && u.getMonthlyPayment() != null).toList();
        if (!startingNext.isEmpty())
            tips.add(CoachTipDto.of(CoachTipType.UPCOMING_NEXT, CoachTipSeverity.TIP, startingNext.size(),
                    startingNext.stream().map(UpcomingDebt::getMonthlyPayment).reduce(BigDecimal.ZERO, BigDecimal::add)));

        return tips.stream().sorted(Comparator.comparing(CoachTipDto::severity)).toList();
    }

    /** Debt payments {@code offset} months from now. */
    private static BigDecimal paymentsIn(List<Debt> debts, int offset) {
        return debts.stream().filter(d -> PayoffSimulator.monthsLeft(d) >= offset)
                .map(Debt::getMonthlyPayment).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
