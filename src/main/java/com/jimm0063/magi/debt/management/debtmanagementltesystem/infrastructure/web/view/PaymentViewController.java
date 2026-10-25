package com.jimm0063.magi.debt.management.debtmanagementltesystem.infrastructure.web.view;

import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.application.port.in.DoPayment;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.application.port.out.DebtAccountRepository;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.application.port.out.PaymentRepository;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.model.Payment;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

@Controller
@RequestMapping("/ui/payments")
public class PaymentViewController {

    private final DoPayment doPayment;
    private final PaymentRepository paymentRepository;
    private final ActivityLogHelper activityLogHelper;
    private final DebtAccountRepository debtAccountRepository;

    public PaymentViewController(DoPayment doPayment,
                                 PaymentRepository paymentRepository,
                                 ActivityLogHelper activityLogHelper,
                                 DebtAccountRepository debtAccountRepository) {
        this.doPayment = doPayment;
        this.paymentRepository = paymentRepository;
        this.activityLogHelper = activityLogHelper;
        this.debtAccountRepository = debtAccountRepository;
    }

    @GetMapping("/{debtAccountCode}")
    public String list(@PathVariable String debtAccountCode, HttpSession session, Model model) {
        String email = (String) session.getAttribute("userEmail");
        if (email == null) return "redirect:/ui";
        model.addAttribute("payments", paymentRepository.findByDebtAccountCode(debtAccountCode).stream()
                .sorted(java.util.Comparator.comparing(Payment::getCreatedAt,
                        java.util.Comparator.nullsLast(java.util.Comparator.reverseOrder())))
                .toList());
        model.addAttribute("account", debtAccountRepository.findDebtAccountByCodeAndActiveTrue(debtAccountCode).orElse(null));
        model.addAttribute("debtAccountCode", debtAccountCode);
        model.addAttribute("email", email);
        return "payments/list";
    }

    @PostMapping("/{debtAccountCode}")
    public String doPaymentAction(@PathVariable String debtAccountCode,
                                  HttpSession session,
                                  RedirectAttributes redirectAttributes) {
        if (session.getAttribute("userEmail") == null) return "redirect:/ui";
        try {
            var payment = doPayment.cardPayment(debtAccountCode);
            activityLogHelper.log(session, "Process Payment — " + debtAccountCode, payment);
            return "redirect:/ui/payments/" + enc(debtAccountCode);
        } catch (Exception e) {
            Map<String, String> info = new LinkedHashMap<>();
            info.put("exception", e.getClass().getName());
            info.put("message", e.getMessage());
            if (e.getCause() != null) {
                info.put("cause", e.getCause().getClass().getName() + ": " + e.getCause().getMessage());
            }
            activityLogHelper.log(session, "Payment ERROR — " + debtAccountCode, info);
            String msg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            if (e.getCause() != null && e.getCause().getMessage() != null) msg += " → " + e.getCause().getMessage();
            redirectAttributes.addFlashAttribute("paymentError", msg);
            return "redirect:/ui/payments/" + enc(debtAccountCode);
        }
    }

    private static String enc(String segment) {
        return UriUtils.encodePathSegment(segment, StandardCharsets.UTF_8);
    }
}
