package com.jimm0063.magi.debt.management.debtmanagementltesystem.infrastructure.web.view;

import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.application.port.in.FindAllFixedExpenseCatalogsUseCase;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.application.port.in.FindAllFixedExpenseUseCase;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.application.port.in.MoveBetweenSectionsUseCase;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.application.port.out.FixedExpenseCatalogRepository;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.application.port.out.FixedExpenseRepository;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.enums.FixedExpenseKind;
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

/**
 * Fixed expenses and credits: the same kind of flat monthly item (same table, same page), kept in two sections so the
 * charts can tell them apart. {@code section} is {@code fixed-expenses} or {@code credits}; items can be moved between them.
 */
@Controller
@RequestMapping("/ui/{section:fixed-expenses|credits}")
public class FixedExpenseViewController {

    static final String CREDITS = "credits";

    private final FixedExpenseRepository fixedExpenseRepository;
    private final FixedExpenseCatalogRepository fixedExpenseCatalogRepository;
    private final FixedExpenseCatalogMapper fixedExpenseCatalogMapper;
    private final FindAllFixedExpenseUseCase findAllFixedExpenseUseCase;
    private final FindAllFixedExpenseCatalogsUseCase findAllFixedExpenseCatalogsUseCase;
    private final ActivityLogHelper activityLogHelper;
    private final MoveBetweenSectionsUseCase moveBetweenSectionsUseCase;

    public FixedExpenseViewController(
            FixedExpenseRepository fixedExpenseRepository,
            FixedExpenseCatalogRepository fixedExpenseCatalogRepository,
            FixedExpenseCatalogMapper fixedExpenseCatalogMapper,
            FindAllFixedExpenseUseCase findAllFixedExpenseUseCase,
            FindAllFixedExpenseCatalogsUseCase findAllFixedExpenseCatalogsUseCase,
            ActivityLogHelper activityLogHelper,
            MoveBetweenSectionsUseCase moveBetweenSectionsUseCase) {
        this.fixedExpenseRepository = fixedExpenseRepository;
        this.fixedExpenseCatalogRepository = fixedExpenseCatalogRepository;
        this.fixedExpenseCatalogMapper = fixedExpenseCatalogMapper;
        this.findAllFixedExpenseUseCase = findAllFixedExpenseUseCase;
        this.findAllFixedExpenseCatalogsUseCase = findAllFixedExpenseCatalogsUseCase;
        this.activityLogHelper = activityLogHelper;
        this.moveBetweenSectionsUseCase = moveBetweenSectionsUseCase;
    }

    static FixedExpenseKind kindOf(String section) {
        return CREDITS.equals(section) ? FixedExpenseKind.CREDIT : FixedExpenseKind.EXPENSE;
    }

    /** The section the items go to when moved from this one. */
    static FixedExpenseKind otherKind(String section) {
        return kindOf(section) == FixedExpenseKind.CREDIT ? FixedExpenseKind.EXPENSE : FixedExpenseKind.CREDIT;
    }

    private static String back(String section) {
        return "redirect:/ui/" + section;
    }

    @GetMapping
    public String list(@PathVariable String section, HttpSession session, Model model) {
        String email = (String) session.getAttribute("userEmail");
        if (email == null) return "redirect:/ui";
        FixedExpenseKind kind = kindOf(section);
        List<FixedExpense> all = findAllFixedExpenseUseCase.getByEmail(email);
        java.time.YearMonth now = java.time.YearMonth.now();
        List<FixedExpense> inSection = all.stream().filter(e -> (kind == FixedExpenseKind.CREDIT) == e.isCredit()).toList();
        // past their last payment month: listed apart, out of the total and the breakdown
        List<FixedExpense> expenses = inSection.stream().filter(e -> e.chargedIn(now)).toList();
        List<FixedExpense> finished = inSection.stream().filter(e -> !e.chargedIn(now)).toList();
        model.addAttribute("finished", finished);
        model.addAttribute("now", now);
        model.addAttribute("section", section);
        model.addAttribute("kind", kind.name());
        // message-key prefix: "fixed" or "credit"
        model.addAttribute("k", kind == FixedExpenseKind.CREDIT ? "credit" : "fixed");
        model.addAttribute("otherSection", kind == FixedExpenseKind.CREDIT ? "fixed-expenses" : CREDITS);
        model.addAttribute("otherCount", all.size() - inSection.size());
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
    public String createExpense(@PathVariable String section, @ModelAttribute FixedExpenseReq req, HttpSession session) {
        String email = (String) session.getAttribute("userEmail");
        if (email == null) return "redirect:/ui";
        req.setUserEmail(email);
        req.setKind(kindOf(section));
        var saved = fixedExpenseRepository.save(req, req.getCatalogId());
        activityLogHelper.log(session, saved.isCredit() ? "Create Credit" : "Create Fixed Expense", saved);
        return back(section);
    }

    @PutMapping("/{id}")
    public String editExpense(@PathVariable String section, @PathVariable Integer id, @ModelAttribute FixedExpenseReq req,
                              HttpSession session) {
        req.setId(id);
        req.setKind(null); // moving between sections goes through /move
        var updated = fixedExpenseRepository.updateFromReq(req);
        activityLogHelper.log(session, "Edit Fixed Expense", updated);
        return back(section);
    }

    @DeleteMapping("/{id}")
    public String deleteExpense(@PathVariable String section, @PathVariable Integer id, HttpSession session) {
        fixedExpenseRepository.delete(id);
        activityLogHelper.log(session, "Delete Fixed Expense", Map.of("deleted", true, "id", id));
        return back(section);
    }

    /** Fixed expense → credits, or credit → fixed expenses. */
    @PostMapping("/{id}/move")
    public String moveExpense(@PathVariable String section, @PathVariable Integer id, HttpSession session) {
        var moved = moveBetweenSectionsUseCase.moveFixedExpenses(List.of(id), otherKind(section));
        activityLogHelper.log(session, "Move to " + otherKind(section), moved);
        return back(section);
    }

    @PostMapping("/bulk-move")
    public String bulkMoveExpenses(@PathVariable String section, @RequestParam List<Integer> ids, HttpSession session) {
        var moved = moveBetweenSectionsUseCase.moveFixedExpenses(ids, otherKind(section));
        activityLogHelper.log(session, "Move to " + otherKind(section), moved);
        return back(section);
    }

    @PutMapping("/bulk-edit")
    public String bulkEditExpenses(@PathVariable String section,
                                    @RequestParam List<Integer> ids,
                                    @RequestParam(required = false) Integer catalogId,
                                    @RequestParam(required = false) Double monthlyCost,
                                    HttpSession session) {
        var updated = fixedExpenseRepository.bulkUpdateCategoryAndCost(
                ids, catalogId, monthlyCost != null ? BigDecimal.valueOf(monthlyCost) : null);
        activityLogHelper.log(session, "Bulk Edit Fixed Expenses", updated);
        return back(section);
    }

    @DeleteMapping("/bulk-delete")
    public String bulkDeleteExpenses(@PathVariable String section, @RequestParam List<Integer> ids, HttpSession session) {
        fixedExpenseRepository.deleteMultiple(ids);
        activityLogHelper.log(session, "Bulk Delete Fixed Expenses", Map.of("deleted", true, "ids", ids));
        return back(section);
    }

    @PostMapping("/catalog")
    public String createCatalog(@PathVariable String section, @ModelAttribute CreateFixedExpenseCatalogReq req,
                                HttpSession session) {
        var saved = fixedExpenseCatalogRepository.save(fixedExpenseCatalogMapper.toModel(req));
        activityLogHelper.log(session, "Create Expense Catalog", saved);
        return back(section);
    }

    @DeleteMapping("/catalog/{id}")
    public String deleteCatalog(@PathVariable String section, @PathVariable Integer id, HttpSession session) {
        fixedExpenseCatalogRepository.delete(id);
        activityLogHelper.log(session, "Delete Expense Catalog", Map.of("deleted", true, "id", id));
        return back(section);
    }
}
