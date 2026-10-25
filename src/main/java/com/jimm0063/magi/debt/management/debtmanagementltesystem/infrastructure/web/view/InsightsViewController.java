package com.jimm0063.magi.debt.management.debtmanagementltesystem.infrastructure.web.view;

import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.application.port.in.GetDebtProgressionUseCase;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.application.port.in.GetFinancialStatusUseCase;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.application.port.out.UserRepository;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.dto.UserStatusDashboard;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.exceptions.EntityNotFoundException;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.YearMonth;
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
    private final ActivityLogHelper activityLogHelper;

    public InsightsViewController(GetFinancialStatusUseCase getFinancialStatusUseCase,
                                  GetDebtProgressionUseCase getDebtProgressionUseCase,
                                  UserRepository userRepository,
                                  ActivityLogHelper activityLogHelper) {
        this.getFinancialStatusUseCase = getFinancialStatusUseCase;
        this.getDebtProgressionUseCase = getDebtProgressionUseCase;
        this.userRepository = userRepository;
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
        model.addAttribute("dashboard", dashboard);
        model.addAttribute("insights", InsightsView.from(progression, range,
                dashboard.getSalary(), dashboard.getMonthlyFixedExpensesAmount()));
        model.addAttribute("range", range);
        return "insights/index";
    }

    @PutMapping("/financials")
    public String updateFinancials(@RequestParam(required = false) Double salary,
                                   @RequestParam(required = false) Double savings,
                                   HttpSession session) {
        String email = (String) session.getAttribute("userEmail");
        if (email == null) return "redirect:/ui";
        var updated = userRepository.updateFinancials(email, salary, savings);
        activityLogHelper.log(session, "Update Salary & Savings", updated);
        return "redirect:/ui/insights";
    }
}
