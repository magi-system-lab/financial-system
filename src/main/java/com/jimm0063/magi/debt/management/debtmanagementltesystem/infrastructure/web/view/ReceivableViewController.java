package com.jimm0063.magi.debt.management.debtmanagementltesystem.infrastructure.web.view;

import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.application.port.in.GetReceivableHistoryUseCase;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.application.port.in.GetReceivableStatusUseCase;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.application.port.in.RecordReceivablePaymentUseCase;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.application.port.out.ReceivableRepository;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.dto.ReceivableHistoryDto;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.dto.ReceivableStatusDto;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.infrastructure.mapper.ReceivableMapper;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.infrastructure.model.CreateReceivableReq;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.infrastructure.model.ReceivablePaymentReq;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/ui/receivables")
public class ReceivableViewController {

    private final ReceivableRepository receivableRepository;
    private final ReceivableMapper receivableMapper;
    private final GetReceivableStatusUseCase getReceivableStatusUseCase;
    private final RecordReceivablePaymentUseCase recordReceivablePaymentUseCase;
    private final GetReceivableHistoryUseCase getReceivableHistoryUseCase;
    private final ActivityLogHelper activityLogHelper;

    public ReceivableViewController(ReceivableRepository receivableRepository,
                                    ReceivableMapper receivableMapper,
                                    GetReceivableStatusUseCase getReceivableStatusUseCase,
                                    RecordReceivablePaymentUseCase recordReceivablePaymentUseCase,
                                    GetReceivableHistoryUseCase getReceivableHistoryUseCase,
                                    ActivityLogHelper activityLogHelper) {
        this.receivableRepository = receivableRepository;
        this.receivableMapper = receivableMapper;
        this.getReceivableStatusUseCase = getReceivableStatusUseCase;
        this.recordReceivablePaymentUseCase = recordReceivablePaymentUseCase;
        this.getReceivableHistoryUseCase = getReceivableHistoryUseCase;
        this.activityLogHelper = activityLogHelper;
    }

    @GetMapping
    public String list(HttpSession session, Model model) {
        String email = (String) session.getAttribute("userEmail");
        if (email == null) return "redirect:/ui";

        List<ReceivableStatusDto> receivables = getReceivableStatusUseCase.getAllStatusesByEmail(email);
        double totalPending = receivables.stream()
                .filter(dto -> Boolean.TRUE.equals(dto.getReceivable().getActive()))
                .mapToDouble(dto -> dto.getPendingAmount().doubleValue())
                .sum();
        long activeCount = receivables.stream()
                .filter(dto -> Boolean.TRUE.equals(dto.getReceivable().getActive()))
                .count();

        model.addAttribute("receivables", receivables);
        model.addAttribute("totalPending", totalPending);
        model.addAttribute("activeCount", activeCount);
        model.addAttribute("newReceivable", new CreateReceivableReq());
        model.addAttribute("email", email);
        model.addAttribute("today", LocalDate.now());
        return "receivables/list";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Integer id, HttpSession session, Model model) {
        String email = (String) session.getAttribute("userEmail");
        if (email == null) return "redirect:/ui";

        ReceivableHistoryDto history = getReceivableHistoryUseCase.getHistory(id);
        var owner = history.getStatus().getReceivable().getDebtSysUser();
        if (owner != null && !email.equalsIgnoreCase(owner.getEmail())) return "redirect:/ui/receivables";

        model.addAttribute("history", history);
        model.addAttribute("status", history.getStatus());
        model.addAttribute("receivable", history.getStatus().getReceivable());
        addChartData(model, history);
        model.addAttribute("today", LocalDate.now());
        model.addAttribute("email", email);
        return "receivables/detail";
    }

    private void addChartData(Model model, ReceivableHistoryDto history) {
        var receivable = history.getStatus().getReceivable();
        List<String> balanceLabels = new ArrayList<>();
        List<BigDecimal> balanceValues = new ArrayList<>();
        List<BigDecimal> repaidValues = new ArrayList<>();

        // Starting point: the full principal outstanding on the day it was lent
        LocalDate start = receivable.getLentDate() != null ? receivable.getLentDate()
                : history.getFirstPaymentDate();
        if (start != null) {
            balanceLabels.add(start.toString());
            balanceValues.add(receivable.getPrincipalAmount());
            repaidValues.add(BigDecimal.ZERO);
        }
        history.getEntries().forEach(entry -> {
            balanceLabels.add(entry.getPaymentDate().toString());
            balanceValues.add(entry.getRemainingBalance());
            repaidValues.add(entry.getCumulativeRepaid());
        });

        model.addAttribute("balanceLabels", balanceLabels);
        model.addAttribute("balanceValues", balanceValues);
        model.addAttribute("repaidValues", repaidValues);
        model.addAttribute("monthLabels", history.getMonthlyTotals().stream().map(ReceivableHistoryDto.MonthlyTotal::getMonth).toList());
        model.addAttribute("monthAmounts", history.getMonthlyTotals().stream().map(ReceivableHistoryDto.MonthlyTotal::getAmount).toList());
    }

    @PostMapping
    public String createReceivable(@ModelAttribute CreateReceivableReq req, HttpSession session) {
        String email = (String) session.getAttribute("userEmail");
        if (email == null) return "redirect:/ui";

        var saved = receivableRepository.save(receivableMapper.toModel(req), email);
        activityLogHelper.log(session, "Create Receivable", saved);
        return "redirect:/ui/receivables";
    }

    @PostMapping("/{id}/payment")
    public String registerPayment(@PathVariable Integer id, @ModelAttribute ReceivablePaymentReq req,
                                  @RequestParam(required = false) String returnTo, HttpSession session) {
        if (session.getAttribute("userEmail") == null) return "redirect:/ui";

        var status = recordReceivablePaymentUseCase.registerPayment(id, req);
        activityLogHelper.log(session, "Register Receivable Payment", status);
        return "detail".equals(returnTo) ? "redirect:/ui/receivables/" + id : "redirect:/ui/receivables";
    }

    @DeleteMapping("/{id}")
    public String deleteReceivable(@PathVariable Integer id, HttpSession session) {
        if (session.getAttribute("userEmail") == null) return "redirect:/ui";

        receivableRepository.delete(id);
        activityLogHelper.log(session, "Delete Receivable", Map.of("deleted", true, "id", id));
        return "redirect:/ui/receivables";
    }
}
