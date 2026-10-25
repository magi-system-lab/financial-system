package com.jimm0063.magi.debt.management.debtmanagementltesystem.infrastructure.web.view;

import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.application.port.in.FindAllFinancialProviderCatalogsUseCase;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.application.port.in.FindAllFinancialProviderUseCase;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.application.port.out.FinancialProviderCatalogRepository;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.application.port.out.FinancialProviderRepository;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.infrastructure.mapper.FinancialProviderCatalogMapper;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.infrastructure.mapper.FinancialProviderMapper;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.infrastructure.model.CreateFinancialProviderCatalogReq;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.infrastructure.model.CreateFinancialProviderReq;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Map;

@Controller
@RequestMapping("/ui/providers")
public class FinancialProviderViewController {

    private final FinancialProviderRepository financialProviderRepository;
    private final FinancialProviderCatalogRepository financialProviderCatalogRepository;
    private final FinancialProviderMapper financialProviderMapper;
    private final FinancialProviderCatalogMapper financialProviderCatalogMapper;
    private final FindAllFinancialProviderUseCase findAllFinancialProviderUseCase;
    private final FindAllFinancialProviderCatalogsUseCase findAllFinancialProviderCatalogsUseCase;
    private final ActivityLogHelper activityLogHelper;

    public FinancialProviderViewController(
            FinancialProviderRepository financialProviderRepository,
            FinancialProviderCatalogRepository financialProviderCatalogRepository,
            FinancialProviderMapper financialProviderMapper,
            FinancialProviderCatalogMapper financialProviderCatalogMapper,
            FindAllFinancialProviderUseCase findAllFinancialProviderUseCase,
            FindAllFinancialProviderCatalogsUseCase findAllFinancialProviderCatalogsUseCase,
            ActivityLogHelper activityLogHelper) {
        this.financialProviderRepository = financialProviderRepository;
        this.financialProviderCatalogRepository = financialProviderCatalogRepository;
        this.financialProviderMapper = financialProviderMapper;
        this.financialProviderCatalogMapper = financialProviderCatalogMapper;
        this.findAllFinancialProviderUseCase = findAllFinancialProviderUseCase;
        this.findAllFinancialProviderCatalogsUseCase = findAllFinancialProviderCatalogsUseCase;
        this.activityLogHelper = activityLogHelper;
    }

    @GetMapping
    public String list(HttpSession session, Model model) {
        String email = (String) session.getAttribute("userEmail");
        if (email == null) return "redirect:/ui";
        model.addAttribute("providers", findAllFinancialProviderUseCase.getActiveByEmail(email));
        model.addAttribute("catalogs", findAllFinancialProviderCatalogsUseCase.getCatalogList());
        model.addAttribute("newProvider", new CreateFinancialProviderReq());
        model.addAttribute("newCatalog", new CreateFinancialProviderCatalogReq());
        model.addAttribute("email", email);
        return "providers/list";
    }

    @PostMapping
    public String createProvider(@ModelAttribute CreateFinancialProviderReq req, HttpSession session) {
        String email = (String) session.getAttribute("userEmail");
        if (email == null) return "redirect:/ui";
        req.setEmail(email);
        if (req.getCode() == null || req.getCode().isBlank()) req.setCode(java.util.UUID.randomUUID().toString());
        var saved = financialProviderRepository.save(financialProviderMapper.toModel(req), email, req.getCatalogId());
        activityLogHelper.log(session, "Create Provider", saved);
        return "redirect:/ui/providers";
    }

    @DeleteMapping("/{code}")
    public String deleteProvider(@PathVariable String code, HttpSession session) {
        financialProviderRepository.delete(code);
        activityLogHelper.log(session, "Delete Provider", Map.of("deleted", true, "code", code));
        return "redirect:/ui/providers";
    }

    @PostMapping("/catalog")
    public String createCatalog(@ModelAttribute CreateFinancialProviderCatalogReq req, HttpSession session) {
        var saved = financialProviderCatalogRepository.save(financialProviderCatalogMapper.toModel(req));
        activityLogHelper.log(session, "Create Provider Catalog", saved);
        return "redirect:/ui/providers";
    }

    @DeleteMapping("/catalog/{id}")
    public String deleteCatalog(@PathVariable Integer id, HttpSession session) {
        financialProviderCatalogRepository.delete(id);
        activityLogHelper.log(session, "Delete Provider Catalog", Map.of("deleted", true, "id", id));
        return "redirect:/ui/providers";
    }
}
