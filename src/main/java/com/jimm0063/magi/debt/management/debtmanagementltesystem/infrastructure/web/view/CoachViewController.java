package com.jimm0063.magi.debt.management.debtmanagementltesystem.infrastructure.web.view;

import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.application.port.in.FindAllDebtsUseCase;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.application.port.in.GetCoachTipsUseCase;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.application.port.in.GetFinancialStatusUseCase;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.application.port.in.GetPayoffPlanUseCase;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.application.port.in.ManageSavingsGoalUseCase;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.dto.PayoffPlanDto;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.dto.SavingsGoalStatusDto;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.dto.UserStatusDashboard;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.enums.PayoffStrategy;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.model.Debt;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.utils.PayoffSimulator;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Coach (Plan tab): advice, the attack plan to pay debts off sooner, and savings goals.
 * The plan is recalculated on the server from the form (GET), so it works without JavaScript.
 */
@Controller
@RequestMapping("/ui/coach")
public class CoachViewController {

    private static final String BASE = "/ui/coach";
    /** Months drawn in the plan chart. */
    static final int CHART_MONTHS = 36;
    /** Suggested extra: this share of what is left from the salary each month. */
    static final BigDecimal SUGGESTED_SHARE = new BigDecimal("0.10");

    private final GetCoachTipsUseCase getCoachTipsUseCase;
    private final GetPayoffPlanUseCase getPayoffPlanUseCase;
    private final ManageSavingsGoalUseCase manageSavingsGoalUseCase;
    private final FindAllDebtsUseCase findAllDebtsUseCase;
    private final GetFinancialStatusUseCase getFinancialStatusUseCase;
    private final UiFormat fmt;
    private final ActivityLogHelper activityLogHelper;

    public CoachViewController(GetCoachTipsUseCase getCoachTipsUseCase, GetPayoffPlanUseCase getPayoffPlanUseCase,
                               ManageSavingsGoalUseCase manageSavingsGoalUseCase, FindAllDebtsUseCase findAllDebtsUseCase,
                               GetFinancialStatusUseCase getFinancialStatusUseCase, UiFormat fmt,
                               ActivityLogHelper activityLogHelper) {
        this.getCoachTipsUseCase = getCoachTipsUseCase;
        this.getPayoffPlanUseCase = getPayoffPlanUseCase;
        this.manageSavingsGoalUseCase = manageSavingsGoalUseCase;
        this.findAllDebtsUseCase = findAllDebtsUseCase;
        this.getFinancialStatusUseCase = getFinancialStatusUseCase;
        this.fmt = fmt;
        this.activityLogHelper = activityLogHelper;
    }

    /**
     * @param f    present when the plan form was sent (an unchecked "roll" checkbox is not sent at all)
     * @param roll snowball: put every payment that frees up towards the next payoff
     */
    @GetMapping
    public String coach(@RequestParam(required = false) BigDecimal extra,
                        @RequestParam(required = false) BigDecimal lump,
                        @RequestParam(defaultValue = "FLOW") PayoffStrategy strategy,
                        @RequestParam(required = false) String f,
                        @RequestParam(required = false) String roll,
                        HttpSession session, Locale locale, Model model) {
        String email = (String) session.getAttribute("userEmail");
        if (email == null) return "redirect:/ui";

        UserStatusDashboard dashboard = getFinancialStatusUseCase.getUserStatus(email);
        List<Debt> debts = findAllDebtsUseCase.getActiveByUser(email);
        BigDecimal nextMonth = debts.stream().filter(d -> PayoffSimulator.monthsLeft(d) > 0 && d.getMonthlyPayment() != null)
                .map(Debt::getMonthlyPayment).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal fixed = BigDecimal.valueOf(nz(dashboard.getMonthlyFixedExpensesAmount()));
        BigDecimal suggested = suggestedExtra(dashboard.getSalary(), fixed, nextMonth);

        boolean snowball = f == null || roll != null;
        BigDecimal extraMonthly = extra != null ? extra.max(BigDecimal.ZERO) : suggested;
        BigDecimal lumpSum = lump != null ? lump.max(BigDecimal.ZERO) : BigDecimal.ZERO;
        PayoffPlanDto plan = getPayoffPlanUseCase.getPlan(email, extraMonthly, lumpSum, strategy, snowball);

        List<SavingsGoalStatusDto> goals = manageSavingsGoalUseCase.getByEmail(email);
        Set<Integer> goalDebtIds = goals.stream().filter(g -> g.goal().isOpen() && g.debt() != null)
                .map(g -> g.debt().getId()).collect(Collectors.toSet());

        BigDecimal commitments = fixed.add(nextMonth);
        BigDecimal emergencyGap = commitments.multiply(BigDecimal.valueOf(InsightsView.SAFETY_GOAL_MONTHS))
                .subtract(BigDecimal.valueOf(nz(dashboard.getSavings()))).setScale(0, RoundingMode.UP);

        model.addAttribute("tips", CoachTipView.from(getCoachTipsUseCase.getTips(email), fmt, locale));
        model.addAttribute("plan", plan);
        model.addAttribute("chart", plan.months().subList(0, Math.min(CHART_MONTHS, plan.months().size())));
        model.addAttribute("chartMax", plan.months().stream().limit(CHART_MONTHS)
                .map(m -> m.baselinePayment().max(m.planPayment())).max(Comparator.naturalOrder()).orElse(BigDecimal.ZERO));
        model.addAttribute("extra", extraMonthly);
        model.addAttribute("lump", lumpSum);
        model.addAttribute("strategy", strategy);
        model.addAttribute("strategies", PayoffStrategy.values());
        model.addAttribute("snowball", snowball);
        model.addAttribute("suggestedExtra", suggested);
        model.addAttribute("goals", goals.stream().filter(g -> g.goal().isOpen()).toList());
        model.addAttribute("closedGoals", goals.stream().filter(g -> !g.goal().isOpen()).toList());
        model.addAttribute("goalDebtIds", goalDebtIds);
        List<Debt> goalDebts = debts.stream().filter(d -> PayoffSimulator.monthsLeft(d) > 0 && d.getId() != null)
                .sorted(Comparator.comparing(PayoffSimulator::payoffBalance)).toList();
        model.addAttribute("goalDebts", goalDebts);
        model.addAttribute("debtBalances", goalDebts.stream()
                .collect(Collectors.toMap(Debt::getId, PayoffSimulator::payoffBalance, (a, b) -> a)));
        model.addAttribute("emergencyGap", emergencyGap.signum() > 0 ? emergencyGap : null);
        model.addAttribute("currentMonth", YearMonth.now());
        model.addAttribute("today", LocalDate.now());
        return "coach/index";
    }

    /** 10% of what's left from the salary after fixed expenses and debt payments, rounded down to 100. */
    static BigDecimal suggestedExtra(Double salary, BigDecimal fixed, BigDecimal payments) {
        if (salary == null || salary <= 0) return BigDecimal.ZERO;
        BigDecimal free = BigDecimal.valueOf(salary).subtract(fixed).subtract(payments);
        if (free.signum() <= 0) return BigDecimal.ZERO;
        return free.multiply(SUGGESTED_SHARE).divide(BigDecimal.valueOf(100), 0, RoundingMode.DOWN)
                .multiply(BigDecimal.valueOf(100));
    }

    // ── Savings goals ───────────────────────────────────────────────────────

    @PostMapping("/goals")
    public String createGoal(@RequestParam(required = false) String name,
                             @RequestParam(required = false) BigDecimal targetAmount,
                             @RequestParam(required = false) String targetMonth,
                             @RequestParam(required = false) Integer debtId,
                             HttpSession session, RedirectAttributes ra) {
        String email = (String) session.getAttribute("userEmail");
        if (email == null) return "redirect:/ui";
        return run(session, ra, "Coach: new goal",
                () -> manageSavingsGoalUseCase.create(email, name, targetAmount, monthEnd(targetMonth), debtId));
    }

    @PostMapping("/goals/{id}")
    public String updateGoal(@PathVariable Integer id,
                             @RequestParam(required = false) String name,
                             @RequestParam(required = false) BigDecimal targetAmount,
                             @RequestParam(required = false) String targetMonth,
                             @RequestParam(required = false) Integer debtId,
                             HttpSession session, RedirectAttributes ra) {
        String email = (String) session.getAttribute("userEmail");
        if (email == null) return "redirect:/ui";
        return run(session, ra, "Coach: update goal",
                () -> manageSavingsGoalUseCase.update(email, id, name, targetAmount, monthEnd(targetMonth), debtId));
    }

    /** withdraw: the amount is taken out of the goal. */
    @PostMapping("/goals/{id}/contributions")
    public String contribute(@PathVariable Integer id, @RequestParam BigDecimal amount,
                             @RequestParam(required = false) String date,
                             @RequestParam(required = false) String note,
                             @RequestParam(required = false) String withdraw,
                             HttpSession session, RedirectAttributes ra) {
        if (session.getAttribute("userEmail") == null) return "redirect:/ui";
        BigDecimal signed = withdraw != null ? amount.abs().negate() : amount.abs();
        return run(session, ra, withdraw != null ? "Coach: take money out" : "Coach: put money in",
                () -> manageSavingsGoalUseCase.contribute(id, signed, parseDate(date), note));
    }

    @DeleteMapping("/contributions/{contributionId}")
    public String deleteContribution(@PathVariable Integer contributionId, HttpSession session, RedirectAttributes ra) {
        if (session.getAttribute("userEmail") == null) return "redirect:/ui";
        return run(session, ra, "Coach: delete contribution", () -> {
            manageSavingsGoalUseCase.deleteContribution(contributionId);
            return Map.of("deleted", true, "id", contributionId);
        });
    }

    @PostMapping("/goals/{id}/pay-off")
    public String payOff(@PathVariable Integer id, HttpSession session, RedirectAttributes ra) {
        String email = (String) session.getAttribute("userEmail");
        if (email == null) return "redirect:/ui";
        return run(session, ra, "Coach: debt paid off with goal", () -> {
            SavingsGoalStatusDto status = manageSavingsGoalUseCase.payOffDebt(email, id);
            ra.addFlashAttribute("paidOff", status.goal().getName());
            return status;
        });
    }

    @PostMapping("/goals/{id}/complete")
    public String complete(@PathVariable Integer id, HttpSession session, RedirectAttributes ra) {
        if (session.getAttribute("userEmail") == null) return "redirect:/ui";
        return run(session, ra, "Coach: goal completed", () -> manageSavingsGoalUseCase.complete(id));
    }

    @PostMapping("/goals/{id}/cancel")
    public String cancel(@PathVariable Integer id, HttpSession session, RedirectAttributes ra) {
        if (session.getAttribute("userEmail") == null) return "redirect:/ui";
        return run(session, ra, "Coach: goal cancelled", () -> manageSavingsGoalUseCase.cancel(id));
    }

    @PostMapping("/goals/{id}/reopen")
    public String reopen(@PathVariable Integer id, HttpSession session, RedirectAttributes ra) {
        if (session.getAttribute("userEmail") == null) return "redirect:/ui";
        return run(session, ra, "Coach: goal reopened", () -> manageSavingsGoalUseCase.reopen(id));
    }

    @DeleteMapping("/goals/{id}")
    public String delete(@PathVariable Integer id, HttpSession session, RedirectAttributes ra) {
        if (session.getAttribute("userEmail") == null) return "redirect:/ui";
        return run(session, ra, "Coach: delete goal", () -> {
            manageSavingsGoalUseCase.delete(id);
            return Map.of("deleted", true, "id", id);
        });
    }

    private String run(HttpSession session, RedirectAttributes ra, String action, Supplier<Object> operation) {
        try {
            activityLogHelper.log(session, action, operation.get());
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("formError", e.getMessage());
        }
        return "redirect:" + BASE + "#goals";
    }

    /** "2027-03" → 2027-03-31 (blank = no date). */
    static LocalDate monthEnd(String yearMonth) {
        if (yearMonth == null || yearMonth.isBlank()) return null;
        try {
            return YearMonth.parse(yearMonth.trim()).atEndOfMonth();
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Pick the month as yyyy-MM");
        }
    }

    private static LocalDate parseDate(String date) {
        if (date == null || date.isBlank()) return null;
        try {
            return LocalDate.parse(date.trim());
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Pick a valid date");
        }
    }

    private static double nz(Double v) {
        return Objects.requireNonNullElse(v, 0.0);
    }
}
