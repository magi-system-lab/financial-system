package com.jimm0063.magi.debt.management.debtmanagementltesystem.infrastructure.web.view;

import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.application.port.in.GetFinancialStatusUseCase;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.application.port.out.UserRepository;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.exceptions.EntityNotFoundException;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

@Controller
@RequestMapping("/ui")
public class DashboardViewController {

    private final GetFinancialStatusUseCase getFinancialStatusUseCase;
    private final UserRepository userRepository;
    private final ActivityLogHelper activityLogHelper;

    @Value("${app.default-user-email:}")
    private String defaultUserEmail;

    public DashboardViewController(GetFinancialStatusUseCase getFinancialStatusUseCase,
                                   UserRepository userRepository,
                                   ActivityLogHelper activityLogHelper) {
        this.getFinancialStatusUseCase = getFinancialStatusUseCase;
        this.userRepository = userRepository;
        this.activityLogHelper = activityLogHelper;
    }

    @GetMapping
    public String home(HttpSession session,
                       @RequestParam(required = false) String error,
                       @RequestParam(name = "switch", required = false) String switchUser) {
        boolean emailNotFound = "email_not_found".equals(error);
        if (switchUser != null) return "home";
        if (!emailNotFound
                && session.getAttribute("userEmail") == null
                && defaultUserEmail != null
                && !defaultUserEmail.isBlank()) {
            session.setAttribute("userEmail", defaultUserEmail);
        }
        if (session.getAttribute("userEmail") != null) {
            return "redirect:/ui/dashboard";
        }
        return "home";
    }

    @PostMapping("/session")
    public String setEmail(@RequestParam String email, HttpSession session, HttpServletResponse response) {
        String trimmed = email.trim();
        session.setAttribute("userEmail", trimmed);
        RememberedUser.remember(response, trimmed);
        return "redirect:/ui/dashboard";
    }

    /** "Switch user": forgets the remembered email and shows the email screen without auto-login. */
    @PostMapping("/session/end")
    public String endSession(HttpSession session, HttpServletResponse response) {
        RememberedUser.forget(session, response);
        return "redirect:/ui?switch";
    }

    @GetMapping("/dashboard")
    public String dashboard(HttpSession session, HttpServletResponse response, Model model) {
        String email = (String) session.getAttribute("userEmail");
        if (email == null) return "redirect:/ui";
        try {
            model.addAttribute("dashboard", getFinancialStatusUseCase.getUserStatus(email));
            model.addAttribute("email", email);
            return "dashboard/index";
        } catch (EntityNotFoundException e) {
            activityLogHelper.log(session, "Login ERROR",
                    Map.of("reason", "email not found", "email", email));
            RememberedUser.forget(session, response);
            return "redirect:/ui?error=email_not_found";
        }
    }

    @PutMapping("/financials")
    public String updateFinancials(@RequestParam(required = false) Double salary,
                                   @RequestParam(required = false) Double savings,
                                   HttpSession session) {
        String email = (String) session.getAttribute("userEmail");
        if (email == null) return "redirect:/ui";
        var updated = userRepository.updateFinancials(email, salary, savings);
        activityLogHelper.log(session, "Update Salary & Savings", updated);
        return "redirect:/ui/dashboard";
    }
}
