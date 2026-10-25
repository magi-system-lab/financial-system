package com.jimm0063.magi.debt.management.debtmanagementltesystem.infrastructure.web.view;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/** Login / session entry points. The old dashboard now lives on as the Insights tab. */
@Controller
@RequestMapping("/ui")
public class DashboardViewController {

    @Value("${app.default-user-email:}")
    private String defaultUserEmail;

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
            return "redirect:/ui/insights";
        }
        return "home";
    }

    @PostMapping("/session")
    public String setEmail(@RequestParam String email, HttpSession session, HttpServletResponse response) {
        String trimmed = email.trim();
        session.setAttribute("userEmail", trimmed);
        RememberedUser.remember(response, trimmed);
        return "redirect:/ui/insights";
    }

    /** "Switch user": forgets the remembered email and shows the email screen without auto-login. */
    @PostMapping("/session/end")
    public String endSession(HttpSession session, HttpServletResponse response) {
        RememberedUser.forget(session, response);
        return "redirect:/ui?switch";
    }

    /** Old bookmarks / Home Screen shortcuts. */
    @GetMapping("/dashboard")
    public String dashboard() {
        return "redirect:/ui/insights";
    }
}
