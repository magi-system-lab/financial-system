package com.jimm0063.magi.debt.management.debtmanagementltesystem.infrastructure.web.view;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/** The month-by-month page was folded into Insights (6 / 12 / 24 months); old links land there. */
@Controller
@RequestMapping("/ui/progression")
public class DebtProgressionViewController {

    @GetMapping
    public String progression() {
        return "redirect:/ui/insights?months=24";
    }
}
