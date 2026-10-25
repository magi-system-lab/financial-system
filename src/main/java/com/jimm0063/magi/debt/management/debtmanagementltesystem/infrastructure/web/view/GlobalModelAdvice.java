package com.jimm0063.magi.debt.management.debtmanagementltesystem.infrastructure.web.view;

import com.jimm0063.magi.debt.management.debtmanagementltesystem.infrastructure.model.ActivityEntry;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.List;

@ControllerAdvice
public class GlobalModelAdvice {

    @ModelAttribute("activityLog")
    public List<ActivityEntry> activityLog(HttpSession session) {
        Object log = session.getAttribute("activityLog");
        if (log instanceof List<?> list) {
            return list.stream()
                    .filter(ActivityEntry.class::isInstance)
                    .map(ActivityEntry.class::cast)
                    .toList();
        }
        return List.of();
    }

    /** Request path without the context path; drives the active nav item and the language links. */
    @ModelAttribute("currentPath")
    public String currentPath(HttpServletRequest request) {
        return request.getRequestURI().substring(request.getContextPath().length());
    }

    /** Which tab / sidebar section the page belongs to: insights | cards | import | plan | more. */
    @ModelAttribute("navSection")
    public String navSection(HttpServletRequest request) {
        return sectionFor(currentPath(request));
    }

    @ModelAttribute("currentUser")
    public String currentUser(HttpSession session) {
        return (String) session.getAttribute("userEmail");
    }

    static String sectionFor(String path) {
        if (path.startsWith("/ui/insights")) return "insights";
        if (path.startsWith("/ui/providers") || path.startsWith("/ui/debt-accounts")
                || path.startsWith("/ui/debts") || path.startsWith("/ui/payments")
                || path.startsWith("/ui/statements") || path.startsWith("/ui/v2/statements")) return "cards";
        if (path.startsWith("/ui/bulk-statements")) return "import";
        if (path.startsWith("/ui/upcoming-debts")) return "plan";
        return "more";
    }
}
