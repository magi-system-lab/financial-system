package com.jimm0063.magi.debt.management.debtmanagementltesystem.infrastructure.web.view;

import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.application.port.in.DebtAccountStatusUseCase;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.application.port.in.FindAllDebtAccountUseCase;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.application.port.in.FindAllDebtsUseCase;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.application.port.out.DebtAccountRepository;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.enums.AccountStatementType;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.enums.DebtTypeEnum;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.infrastructure.mapper.DebtAccountMapper;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.infrastructure.model.CreateDebtAccountReq;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;

import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.model.Debt;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/ui/debt-accounts")
public class DebtAccountViewController {

    private final DebtAccountRepository debtAccountRepository;
    private final DebtAccountMapper debtAccountMapper;
    private final FindAllDebtAccountUseCase findAllDebtAccountUseCase;
    private final FindAllDebtsUseCase findAllDebtsUseCase;
    private final DebtAccountStatusUseCase debtAccountStatusUseCase;
    private final ActivityLogHelper activityLogHelper;

    public DebtAccountViewController(
            DebtAccountRepository debtAccountRepository,
            DebtAccountMapper debtAccountMapper,
            FindAllDebtAccountUseCase findAllDebtAccountUseCase,
            FindAllDebtsUseCase findAllDebtsUseCase,
            DebtAccountStatusUseCase debtAccountStatusUseCase,
            ActivityLogHelper activityLogHelper) {
        this.debtAccountRepository = debtAccountRepository;
        this.debtAccountMapper = debtAccountMapper;
        this.findAllDebtAccountUseCase = findAllDebtAccountUseCase;
        this.findAllDebtsUseCase = findAllDebtsUseCase;
        this.debtAccountStatusUseCase = debtAccountStatusUseCase;
        this.activityLogHelper = activityLogHelper;
    }

    @GetMapping
    public String list(@RequestParam String providerCode, HttpSession session, Model model) {
        String email = (String) session.getAttribute("userEmail");
        if (email == null) return "redirect:/ui";
        model.addAttribute("accounts", findAllDebtAccountUseCase.getActiveByFinancialProvider(providerCode));
        model.addAttribute("providerCode", providerCode);
        model.addAttribute("newAccount", new CreateDebtAccountReq());
        model.addAttribute("statementTypes", AccountStatementType.values());
        model.addAttribute("email", email);
        return "debt-accounts/list";
    }

    @PostMapping("/{providerCode}")
    public String createAccount(@ModelAttribute CreateDebtAccountReq req,
                                @PathVariable String providerCode,
                                HttpSession session) {
        if (session.getAttribute("userEmail") == null) return "redirect:/ui";
        var saved = debtAccountRepository.save(debtAccountMapper.toModel(req), providerCode);
        activityLogHelper.log(session, "Create Debt Account", saved);
        return "redirect:/ui/debt-accounts?providerCode=" + enc(providerCode);
    }

    @DeleteMapping("/{code}")
    public String deleteAccount(@PathVariable String code,
                                @RequestParam(required = false) String providerCode,
                                HttpSession session) {
        debtAccountRepository.delete(code);
        activityLogHelper.log(session, "Delete Debt Account", Map.of("deleted", true, "code", code));
        return providerCode != null
                ? "redirect:/ui/debt-accounts?providerCode=" + enc(providerCode)
                : "redirect:/ui/insights";
    }

    @GetMapping("/csv-template")
    public ResponseEntity<byte[]> downloadCsvTemplate() {
        String csv = "description,operationDate,originalAmount,monthlyPayment,currentInstallment,maxFinancingTerm,debtType\n";
        byte[] content = csv.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"debts-template.csv\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .contentLength(content.length)
                .body(content);
    }

    @GetMapping("/{code}")
    public String detail(@PathVariable String code, HttpSession session, Model model) {
        String email = (String) session.getAttribute("userEmail");
        if (email == null) return "redirect:/ui";

        List<Debt> debts = findAllDebtsUseCase.getActiveByDebtAccount(code);

        Map<DebtTypeEnum, List<Debt>> debtsByType = debts.stream()
                .collect(Collectors.groupingBy(
                        d -> d.getDebtType() != null ? d.getDebtType() : DebtTypeEnum.CARD,
                        LinkedHashMap::new, Collectors.toList()));

        Map<DebtTypeEnum, BigDecimal> typeSubtotals = new LinkedHashMap<>();
        debtsByType.forEach((type, list) -> {
            BigDecimal subtotal = list.stream()
                    .filter(d -> d.getMonthlyPayment() != null)
                    .map(Debt::getMonthlyPayment)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            typeSubtotals.put(type, subtotal);
        });

        BigDecimal totalMonthlyPayment = typeSubtotals.values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        model.addAttribute("account", debtAccountRepository.findDebtAccountByCodeAndActiveTrue(code).orElse(null));
        model.addAttribute("debts", debts);
        model.addAttribute("debtsByType", debtsByType);
        model.addAttribute("typeSubtotals", typeSubtotals);
        model.addAttribute("totalMonthlyPayment", totalMonthlyPayment);
        model.addAttribute("status", debtAccountStatusUseCase.getStatus(code));
        model.addAttribute("debtAccountCode", code);
        model.addAttribute("debtTypes", DebtTypeEnum.values());
        model.addAttribute("statementTypes", AccountStatementType.values());
        model.addAttribute("email", email);
        return "debt-accounts/detail";
    }

    @PostMapping("/{code}/statement-type")
    public String changeStatementType(@PathVariable String code,
                                      @RequestParam AccountStatementType accountStatementType,
                                      HttpSession session,
                                      RedirectAttributes ra) {
        if (session.getAttribute("userEmail") == null) return "redirect:/ui";
        var updated = debtAccountRepository.updateStatementType(code, accountStatementType);
        activityLogHelper.log(session, "Change Statement Type",
                Map.of("code", code, "newType", accountStatementType.toString()));
        ra.addFlashAttribute("successMessage", "Statement type updated to " + accountStatementType);
        return "redirect:/ui/debt-accounts/" + enc(code);
    }

    private static String enc(String segment) {
        return UriUtils.encodePathSegment(segment, StandardCharsets.UTF_8);
    }
}
