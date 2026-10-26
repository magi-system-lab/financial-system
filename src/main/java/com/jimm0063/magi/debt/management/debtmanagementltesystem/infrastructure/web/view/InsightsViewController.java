package com.jimm0063.magi.debt.management.debtmanagementltesystem.infrastructure.web.view;

import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.application.port.in.FindAllDebtsUseCase;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.application.port.in.GetDebtActivityUseCase;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.application.port.in.GetDebtProgressionUseCase;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.application.port.in.GetFinancialStatusUseCase;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.application.port.out.UserRepository;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.dto.DebtActivityDto;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.dto.UserStatusDashboard;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.exceptions.EntityNotFoundException;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.model.DebtSysUser;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Insights tab (home screen after login): month-by-month payments, debts finishing soon, amounts per card,
 * payoff advisor and spending by category. Replaces the old dashboard and the "progression" page.
 */
@Controller
@RequestMapping("/ui")
public class InsightsViewController {

    private final GetFinancialStatusUseCase getFinancialStatusUseCase;
    private final GetDebtProgressionUseCase getDebtProgressionUseCase;
    private final UserRepository userRepository;
    private final GetDebtActivityUseCase getDebtActivityUseCase;
    private final FindAllDebtsUseCase findAllDebtsUseCase;
    private final ActivityLogHelper activityLogHelper;

    /** Months shown in the activity trend (current month included). */
    static final int ACTIVITY_MONTHS = 6;

    public InsightsViewController(GetFinancialStatusUseCase getFinancialStatusUseCase,
                                  GetDebtProgressionUseCase getDebtProgressionUseCase,
                                  UserRepository userRepository,
                                  GetDebtActivityUseCase getDebtActivityUseCase,
                                  FindAllDebtsUseCase findAllDebtsUseCase,
                                  ActivityLogHelper activityLogHelper) {
        this.getFinancialStatusUseCase = getFinancialStatusUseCase;
        this.getDebtProgressionUseCase = getDebtProgressionUseCase;
        this.userRepository = userRepository;
        this.getDebtActivityUseCase = getDebtActivityUseCase;
        this.findAllDebtsUseCase = findAllDebtsUseCase;
        this.activityLogHelper = activityLogHelper;
    }

    @GetMapping("/insights")
    public String insights(@RequestParam(defaultValue = "12") int months,
                           HttpSession session, HttpServletResponse response, Model model) {
        String email = (String) session.getAttribute("userEmail");
        if (email == null) return "redirect:/ui";

        UserStatusDashboard dashboard;
        try {
            dashboard = getFinancialStatusUseCase.getUserStatus(email);
        } catch (EntityNotFoundException e) {
            activityLogHelper.log(session, "Login ERROR", Map.of("reason", "email not found", "email", email));
            RememberedUser.forget(session, response);
            return "redirect:/ui?error=email_not_found";
        }

        int range = months == 6 || months == 24 ? months : 12;
        var progression = getDebtProgressionUseCase.getProgression(email,
                YearMonth.now().plusMonths(InsightsView.HORIZON_MONTHS));
        Integer debtLimit = userRepository.findUserByEmail(email).map(DebtSysUser::getDebtLimitPercent).orElse(null);
        List<DebtActivityDto> activity = getDebtActivityUseCase.getActivity(email, ACTIVITY_MONTHS);
        BigDecimal maxAdded = activity.stream().map(DebtActivityDto::addedMonthly)
                .max(Comparator.naturalOrder()).orElse(BigDecimal.ZERO);
        model.addAttribute("dashboard", dashboard);
        model.addAttribute("insights", InsightsView.from(progression, range,
                dashboard.getSalary(), dashboard.getMonthlyFixedExpensesAmount(), dashboard.getSavings(), debtLimit));
        model.addAttribute("activity", activity);
        model.addAttribute("payoffOptions", InsightsView.payoffOptions(findAllDebtsUseCase.getActiveByUser(email)));
        model.addAttribute("thisMonth", activity.isEmpty()
                ? new DebtActivityDto(YearMonth.now(), List.of(), BigDecimal.ZERO, List.of(), BigDecimal.ZERO)
                : activity.get(activity.size() - 1));
        model.addAttribute("maxAdded", maxAdded);
        model.addAttribute("range", range);
        return "insights/index";
    }

    @PutMapping("/financials")
    public String updateFinancials(@RequestParam(required = false) Double salary,
                                   @RequestParam(required = false) Double savings,
                                   @RequestParam(required = false) Integer debtLimitPercent,
                                   HttpSession session) {
        String email = (String) session.getAttribute("userEmail");
        if (email == null) return "redirect:/ui";
        var updated = userRepository.updateFinancials(email, salary, savings);
        if (debtLimitPercent != null) updated = userRepository.updateDebtLimit(email, debtLimitPercent);
        activityLogHelper.log(session, "Update Salary & Savings", updated);
        return "redirect:/ui/insights";
    }
}
