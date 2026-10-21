package com.jimm0063.magi.debt.management.debtmanagementltesystem.infrastructure.web.view;

import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.application.port.in.FindAllDebtAccountUseCase;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.application.port.in.FindAllFinancialProviderUseCase;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.application.port.in.FindAllUpcomingDebtsUseCase;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.application.port.in.GetPaymentProjectionUseCase;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.application.port.in.ManageUpcomingDebtUseCase;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.enums.DebtTypeEnum;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.model.DebtAccount;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.model.FinancialProvider;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.model.UpcomingDebt;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.infrastructure.model.CreateUpcomingDebtReq;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.YearMonth;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * "Upcoming debts" pile: purchases whose first payment is in a future month.
 * Planning only — nothing here writes to the debt table.
 */
@Controller
@RequestMapping("/ui/upcoming-debts")
public class UpcomingDebtViewController {

    private static final String BASE = "/ui/upcoming-debts";

    private final FindAllUpcomingDebtsUseCase findAllUpcomingDebtsUseCase;
    private final ManageUpcomingDebtUseCase manageUpcomingDebtUseCase;
    private final GetPaymentProjectionUseCase getPaymentProjectionUseCase;
    private final FindAllFinancialProviderUseCase findAllFinancialProviderUseCase;
    private final FindAllDebtAccountUseCase findAllDebtAccountUseCase;
    private final ActivityLogHelper activityLogHelper;

    public UpcomingDebtViewController(FindAllUpcomingDebtsUseCase findAllUpcomingDebtsUseCase,
                                      ManageUpcomingDebtUseCase manageUpcomingDebtUseCase,
                                      GetPaymentProjectionUseCase getPaymentProjectionUseCase,
                                      FindAllFinancialProviderUseCase findAllFinancialProviderUseCase,
                                      FindAllDebtAccountUseCase findAllDebtAccountUseCase,
                                      ActivityLogHelper activityLogHelper) {
        this.findAllUpcomingDebtsUseCase = findAllUpcomingDebtsUseCase;
        this.manageUpcomingDebtUseCase = manageUpcomingDebtUseCase;
        this.getPaymentProjectionUseCase = getPaymentProjectionUseCase;
        this.findAllFinancialProviderUseCase = findAllFinancialProviderUseCase;
        this.findAllDebtAccountUseCase = findAllDebtAccountUseCase;
        this.activityLogHelper = activityLogHelper;
    }

    @GetMapping
    public String list(@RequestParam(defaultValue = "12") int months, HttpSession session, Model model) {
        String email = (String) session.getAttribute("userEmail");
        if (email == null) return "redirect:/ui";

        Map<String, List<DebtAccount>> accountsByProvider = new LinkedHashMap<>();
        for (FinancialProvider provider : findAllFinancialProviderUseCase.getActiveByEmail(email)) {
            List<DebtAccount> accounts = findAllDebtAccountUseCase.getActiveByFinancialProvider(provider.getCode());
            if (!accounts.isEmpty()) accountsByProvider.put(provider.getName(), accounts);
        }

        List<UpcomingDebt> all = findAllUpcomingDebtsUseCase.getByEmail(email);
        List<UpcomingDebt> pending = all.stream().filter(UpcomingDebt::isPending).toList();
        YearMonth now = YearMonth.now();

        model.addAttribute("pending", pending);
        model.addAttribute("history", all.stream().filter(u -> !u.isPending()).toList());
        model.addAttribute("dueCount", pending.stream().filter(u -> u.isDue(now)).count());
        model.addAttribute("pendingMonthlyTotal", pending.stream()
                .filter(u -> u.getMonthlyPayment() != null)
                .mapToDouble(u -> u.getMonthlyPayment().doubleValue()).sum());
        model.addAttribute("pendingTotal", pending.stream()
                .filter(u -> u.getTotalAmount() != null)
                .mapToDouble(u -> u.getTotalAmount().doubleValue()).sum());
        model.addAttribute("currentMonth", now);
        model.addAttribute("projection", getPaymentProjectionUseCase.getProjection(email, months));
        model.addAttribute("months", months);
        model.addAttribute("accountsByProvider", accountsByProvider);
        model.addAttribute("debtTypes", DebtTypeEnum.values());
        model.addAttribute("newUpcomingDebt", new CreateUpcomingDebtReq());
        model.addAttribute("email", email);
        return "upcoming-debts/list";
    }

    @PostMapping
    public String create(@ModelAttribute CreateUpcomingDebtReq req, HttpSession session, RedirectAttributes ra) {
        String email = (String) session.getAttribute("userEmail");
        if (email == null) return "redirect:/ui";
        return run(session, ra, "Create Upcoming Debt", null,
                () -> manageUpcomingDebtUseCase.create(email, req.toCommand()));
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Integer id, @ModelAttribute CreateUpcomingDebtReq req,
                         HttpSession session, RedirectAttributes ra) {
        if (session.getAttribute("userEmail") == null) return "redirect:/ui";
        return run(session, ra, "Update Upcoming Debt", null,
                () -> manageUpcomingDebtUseCase.update(id, req.toCommand()));
    }

    /** Taken off the pile because the purchase now shows on a statement. */
    @PostMapping("/{id}/start")
    public String start(@PathVariable Integer id, @RequestParam(required = false) String returnTo,
                        HttpSession session, RedirectAttributes ra) {
        if (session.getAttribute("userEmail") == null) return "redirect:/ui";
        return run(session, ra, "Upcoming Debt Started", returnTo, () -> manageUpcomingDebtUseCase.markAsStarted(id));
    }

    @PostMapping("/{id}/cancel")
    public String cancel(@PathVariable Integer id, HttpSession session, RedirectAttributes ra) {
        if (session.getAttribute("userEmail") == null) return "redirect:/ui";
        return run(session, ra, "Upcoming Debt Cancelled", null, () -> manageUpcomingDebtUseCase.cancel(id));
    }

    @PostMapping("/{id}/reopen")
    public String reopen(@PathVariable Integer id, HttpSession session, RedirectAttributes ra) {
        if (session.getAttribute("userEmail") == null) return "redirect:/ui";
        return run(session, ra, "Upcoming Debt Reopened", null, () -> manageUpcomingDebtUseCase.reopen(id));
    }

    @DeleteMapping("/{id}")
    public String delete(@PathVariable Integer id, HttpSession session, RedirectAttributes ra) {
        if (session.getAttribute("userEmail") == null) return "redirect:/ui";
        return run(session, ra, "Delete Upcoming Debt", null, () -> {
            manageUpcomingDebtUseCase.delete(id);
            return Map.of("deleted", true, "id", id);
        });
    }

    private String run(HttpSession session, RedirectAttributes ra, String action, String returnTo,
                       Supplier<Object> operation) {
        try {
            activityLogHelper.log(session, action, operation.get());
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("formError", e.getMessage());
        }
        return "redirect:" + safeReturnTo(returnTo);
    }

    /** Only same-app UI paths are allowed as redirect targets. */
    private static String safeReturnTo(String returnTo) {
        if (returnTo != null && returnTo.startsWith("/ui/") && !returnTo.startsWith("//") && !returnTo.contains("\\"))
            return returnTo;
        return BASE;
    }
}
