package com.jimm0063.magi.debt.management.debtmanagementltesystem.infrastructure.web.view;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.LinkedHashMap;
import java.util.Map;

@ControllerAdvice(basePackages =
        "com.jimm0063.magi.debt.management.debtmanagementltesystem.infrastructure.web.view")
public class GlobalViewExceptionHandler {

    private static final GlobalModelAdvice NAV = new GlobalModelAdvice();

    private final ActivityLogHelper activityLogHelper;

    public GlobalViewExceptionHandler(ActivityLogHelper activityLogHelper) {
        this.activityLogHelper = activityLogHelper;
    }

    @ExceptionHandler(Exception.class)
    public String handleViewException(Exception e, HttpSession session, HttpServletRequest request, Model model) {
        Map<String, String> errorInfo = new LinkedHashMap<>();
        errorInfo.put("exception", e.getClass().getName());
        errorInfo.put("message", e.getMessage());
        if (e.getCause() != null) {
            errorInfo.put("cause", e.getCause().getClass().getName() + ": " + e.getCause().getMessage());
        }
        activityLogHelper.log(session, "UNHANDLED ERROR", errorInfo);

        model.addAttribute("errorType", e.getClass().getSimpleName());
        model.addAttribute("errorMessage", e.getMessage());
        model.addAttribute("errorCause", e.getCause() != null ? e.getCause().getMessage() : null);
        // @ModelAttribute advice is not applied to exception handlers; the nav shell needs these.
        model.addAttribute("currentPath", NAV.currentPath(request));
        model.addAttribute("navSection", NAV.navSection(request));
        model.addAttribute("currentUser", NAV.currentUser(session));
        return "error/view-error";
    }
}
