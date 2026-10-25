package com.jimm0063.magi.debt.management.debtmanagementltesystem.infrastructure.web.view;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/** "More" tab: secondary sections, settings, and the session's activity log. */
@Controller
@RequestMapping("/ui")
public class MoreViewController {

    @GetMapping("/more")
    public String more(HttpSession session) {
        if (session.getAttribute("userEmail") == null) return "redirect:/ui";
        return "more/index";
    }

    @GetMapping("/activity")
    public String activity(HttpSession session) {
        if (session.getAttribute("userEmail") == null) return "redirect:/ui";
        return "more/activity";
    }
}
