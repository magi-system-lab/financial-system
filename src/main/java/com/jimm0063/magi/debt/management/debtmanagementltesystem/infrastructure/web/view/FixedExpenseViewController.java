package com.jimm0063.magi.debt.management.debtmanagementltesystem.infrastructure.web.view;

import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.application.port.in.FindAllFixedExpenseCatalogsUseCase;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.application.port.in.FindAllFixedExpenseUseCase;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.application.port.out.FixedExpenseCatalogRepository;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.application.port.out.FixedExpenseRepository;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.model.FixedExpense;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.infrastructure.mapper.FixedExpenseCatalogMapper;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.infrastructure.model.CreateFixedExpenseCatalogReq;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.infrastructure.model.FixedExpenseReq;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/ui/fixed-expenses")
public class FixedExpenseViewController {

    private final FixedExpenseRepository fixedExpenseRepository;
    private final FixedExpenseCatalogRepository fixedExpenseCatalogRepository;
    private final FixedExpenseCatalogMapper fixedExpenseCatalogMapper;
    private final FindAllFixedExpenseUseCase findAllFixedExpenseUseCase;
    private final FindAllFixedExpenseCatalogsUseCase findAllFixedExpenseCatalogsUseCase;
    private final ActivityLogHelper activityLogHelper;

    public FixedExpenseViewController(
            FixedExpenseRepository fixedExpenseRepository,
            FixedExpenseCatalogRepository fixedExpenseCatalogRepository,
            FixedExpenseCatalogMapper fixedExpenseCatalogMapper,
            FindAllFixedExpenseUseCase findAllFixedExpenseUseCase,
            FindAllFixedExpenseCatalogsUseCase findAllFixedExpenseCatalogsUseCase,
            ActivityLogHelper activityLogHelper) {
        this.fixedExpenseRepository = fixedExpenseRepository;
        this.fixedExpenseCatalogRepository = fixedExpenseCatalogRepository;
        this.fixedExpenseCatalogMapper = fixedExpenseCatalogMapper;
        this.findAllFixedExpenseUseCase = findAllFixedExpenseUseCase;
        this.findAllFixedExpenseCatalogsUseCase = findAllFixedExpenseCatalogsUseCase;
        this.activityLogHelper = activityLogHelper;
    }

    @GetMapping
    public String list(HttpSession session, Model model) {
        String email = (String) session.getAttribute("userEmail");
        if (email == null) return "redirect:/ui";
        List<FixedExpense> expenses = findAllFixedExpenseUseCase.getByEmail(email);
        model.addAttribute("expenses", expenses);
        model.addAttribute("byCategory", byCategory(expenses));
        model.addAttribute("total", expenses.stream().map(FixedExpenseViewController::cost)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        model.addAttribute("catalogs", findAllFixedExpenseCatalogsUseCase.getCatalogList());
        model.addAttribute("newExpense", new FixedExpenseReq());
        model.addAttribute("newCatalog", new CreateFixedExpenseCatalogReq());
        model.addAttribute("email", email);
        return "fixed-expenses/list";
    }

    /** Expenses of one category, biggest category first (the breakdown that used to live on the dashboard). */
    public record CategoryGroup(String name, BigDecimal total, int percent, List<FixedExpense> expenses) {}

    static List<CategoryGroup> byCategory(List<FixedExpense> expenses) {
        Map<String, List<FixedExpense>> groups = new LinkedHashMap<>();
        expenses.stream()
                .sorted(Comparator.comparing(FixedExpenseViewController::cost).reversed())
                .forEach(e -> groups.computeIfAbsent(
                        e.getFixedExpenseCatalog() != null ? e.getFixedExpenseCatalog().getName() : "",
                        k -> new ArrayList<>()).add(e));
        BigDecimal total = expenses.stream().map(FixedExpenseViewController::cost).reduce(BigDecimal.ZERO, BigDecimal::add);
        return groups.entrySet().stream()
                .map(g -> {
                    BigDecimal sum = g.getValue().stream().map(FixedExpenseViewController::cost)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    int percent = total.signum() > 0
                            ? sum.multiply(BigDecimal.valueOf(100)).divide(total, 0, RoundingMode.HALF_UP).intValue() : 0;
                    return new CategoryGroup(g.getKey(), sum, percent, g.getValue());
                })
                .sorted(Comparator.comparing(CategoryGroup::total).reversed())
                .toList();
    }

    private static BigDecimal cost(FixedExpense e) {
        return e.getMonthlyCost() == null ? BigDecimal.ZERO : e.getMonthlyCost();
    }

    @PostMapping
    public String createExpense(@ModelAttribute FixedExpenseReq req, HttpSession session) {
        String email = (String) session.getAttribute("userEmail");
        if (email == null) return "redirect:/ui";
        req.setUserEmail(email);
        var saved = fixedExpenseRepository.save(req, req.getCatalogId());
        activityLogHelper.log(session, "Create Fixed Expense", saved);
        return "redirect:/ui/fixed-expenses";
    }

    @PutMapping("/{id}")
    public String editExpense(@PathVariable Integer id, @ModelAttribute FixedExpenseReq req, HttpSession session) {
        req.setId(id);
        var updated = fixedExpenseRepository.updateFromReq(req);
        activityLogHelper.log(session, "Edit Fixed Expense", updated);
        return "redirect:/ui/fixed-expenses";
    }

    @DeleteMapping("/{id}")
    public String deleteExpense(@PathVariable Integer id, HttpSession session) {
        fixedExpenseRepository.delete(id);
        activityLogHelper.log(session, "Delete Fixed Expense", Map.of("deleted", true, "id", id));
        return "redirect:/ui/fixed-expenses";
    }

    @PutMapping("/bulk-edit")
    public String bulkEditExpenses(@RequestParam List<Integer> ids,
                                    @RequestParam(required = false) Integer catalogId,
                                    @RequestParam(required = false) Double monthlyCost,
                                    HttpSession session) {
        var updated = fixedExpenseRepository.bulkUpdateCategoryAndCost(
                ids, catalogId, monthlyCost != null ? BigDecimal.valueOf(monthlyCost) : null);
        activityLogHelper.log(session, "Bulk Edit Fixed Expenses", updated);
        return "redirect:/ui/fixed-expenses";
    }

    @DeleteMapping("/bulk-delete")
    public String bulkDeleteExpenses(@RequestParam List<Integer> ids, HttpSession session) {
        fixedExpenseRepository.deleteMultiple(ids);
        activityLogHelper.log(session, "Bulk Delete Fixed Expenses", Map.of("deleted", true, "ids", ids));
        return "redirect:/ui/fixed-expenses";
    }

    @PostMapping("/catalog")
    public String createCatalog(@ModelAttribute CreateFixedExpenseCatalogReq req, HttpSession session) {
        var saved = fixedExpenseCatalogRepository.save(fixedExpenseCatalogMapper.toModel(req));
        activityLogHelper.log(session, "Create Expense Catalog", saved);
        return "redirect:/ui/fixed-expenses";
    }

    @DeleteMapping("/catalog/{id}")
    public String deleteCatalog(@PathVariable Integer id, HttpSession session) {
        fixedExpenseCatalogRepository.delete(id);
        activityLogHelper.log(session, "Delete Expense Catalog", Map.of("deleted", true, "id", id));
        return "redirect:/ui/fixed-expenses";
    }
}
