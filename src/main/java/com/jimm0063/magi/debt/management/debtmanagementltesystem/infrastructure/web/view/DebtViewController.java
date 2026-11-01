package com.jimm0063.magi.debt.management.debtmanagementltesystem.infrastructure.web.view;

import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.application.port.in.PayOffDebtAccountUseCase;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.application.port.out.DebtRepository;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.enums.DebtTypeEnum;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.model.Debt;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.infrastructure.mapper.DebtMapper;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.infrastructure.model.CreateDebtReq;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.util.UriUtils;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Controller
@RequestMapping("/ui/debts")
public class DebtViewController {

    private final DebtRepository debtRepository;
    private final DebtMapper debtMapper;
    private final PayOffDebtAccountUseCase payOffDebtAccountUseCase;
    private final ActivityLogHelper activityLogHelper;

    public DebtViewController(DebtRepository debtRepository,
                              DebtMapper debtMapper,
                              PayOffDebtAccountUseCase payOffDebtAccountUseCase,
                              ActivityLogHelper activityLogHelper) {
        this.debtRepository = debtRepository;
        this.debtMapper = debtMapper;
        this.payOffDebtAccountUseCase = payOffDebtAccountUseCase;
        this.activityLogHelper = activityLogHelper;
    }

    @PostMapping("/{debtAccountCode}")
    public String createDebt(@ModelAttribute CreateDebtReq req,
                             @PathVariable String debtAccountCode,
                             HttpSession session) {
        if (session.getAttribute("userEmail") == null) return "redirect:/ui";
        var saved = debtRepository.save(debtMapper.toModel(req), debtAccountCode);
        activityLogHelper.log(session, "Create Debt", saved);
        return "redirect:/ui/debt-accounts/" + enc(debtAccountCode);
    }

    @DeleteMapping("/{debtId}")
    public String deleteDebt(@PathVariable Integer debtId,
                             @RequestParam String debtAccountCode,
                             HttpSession session) {
        debtRepository.delete(debtId);
        activityLogHelper.log(session, "Delete Debt", Map.of("deleted", true, "id", debtId));
        return "redirect:/ui/debt-accounts/" + enc(debtAccountCode);
    }

    @PostMapping("/{debtId}/update")
    public String updateDebt(
            @PathVariable Integer debtId,
            @RequestParam String debtAccountCode,
            @RequestParam String description,
            @RequestParam(required = false) String operationDate,
            @RequestParam(required = false) BigDecimal originalAmount,
            @RequestParam BigDecimal monthlyPayment,
            @RequestParam Integer currentInstallment,
            @RequestParam Integer maxFinancingTerm,
            @RequestParam DebtTypeEnum debtType,
            @RequestParam(required = false) BigDecimal annualInterestRate,
            HttpSession session) {
        if (session.getAttribute("userEmail") == null) return "redirect:/ui";
        Debt debt = new Debt();
        debt.setId(debtId);
        debt.setDescription(description);
        debt.setOperationDate(operationDate);
        debt.setOriginalAmount(originalAmount);
        debt.setMonthlyPayment(monthlyPayment);
        debt.setCurrentInstallment(currentInstallment);
        debt.setMaxFinancingTerm(maxFinancingTerm);
        debt.setDebtType(debtType);
        debt.setAnnualInterestRate(annualInterestRate);
        debt.setActive(true);
        var updated = debtRepository.update(debt);
        activityLogHelper.log(session, "Update Debt — " + debtId, updated);
        return "redirect:/ui/debt-accounts/" + enc(debtAccountCode);
    }

    @PostMapping("/{debtAccountCode}/pay-off")
    public String payOffDebts(@PathVariable String debtAccountCode, HttpSession session) {
        if (session.getAttribute("userEmail") == null) return "redirect:/ui";
        var paid = payOffDebtAccountUseCase.payOffByDebtAccountCode(debtAccountCode);
        activityLogHelper.log(session, "Pay Off — " + debtAccountCode, paid);
        return "redirect:/ui/debt-accounts/" + enc(debtAccountCode);
    }

    private static String enc(String segment) {
        return UriUtils.encodePathSegment(segment, StandardCharsets.UTF_8);
    }
}
