package com.jimm0063.magi.debt.management.debtmanagementltesystem.infrastructure.web.view;

import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.application.port.in.FindAllFinancialProviderUseCase;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.application.port.in.GetFinancialStatusUseCase;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.dto.AccountSummaryDto;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.dto.UserStatusDashboard;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.enums.AccountStatementType;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.model.DebtAccount;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Cards tab: every active card of the user as a Wallet-style stack (optionally one provider only). */
@Controller
@RequestMapping("/ui/cards")
public class CardsViewController {

    /** One card in the wallet: the account plus what it costs this month and what is left to pay. */
    public record CardSummary(DebtAccount account, String color, String providerName, BigDecimal monthly,
                              BigDecimal remaining, int activeDebts, int monthsLeft) {}

    private final GetFinancialStatusUseCase getFinancialStatusUseCase;
    private final FindAllFinancialProviderUseCase findAllFinancialProviderUseCase;
    private final CardColors cardColors;

    public CardsViewController(GetFinancialStatusUseCase getFinancialStatusUseCase,
                               FindAllFinancialProviderUseCase findAllFinancialProviderUseCase,
                               CardColors cardColors) {
        this.getFinancialStatusUseCase = getFinancialStatusUseCase;
        this.findAllFinancialProviderUseCase = findAllFinancialProviderUseCase;
        this.cardColors = cardColors;
    }

    @GetMapping
    public String cards(@RequestParam(required = false) String provider, HttpSession session, Model model) {
        String email = (String) session.getAttribute("userEmail");
        if (email == null) return "redirect:/ui";

        UserStatusDashboard dashboard = getFinancialStatusUseCase.getUserStatus(email);
        Map<String, AccountSummaryDto> summaries = dashboard.getAccountSummaries() == null ? Map.of()
                : dashboard.getAccountSummaries().stream()
                    .collect(Collectors.toMap(AccountSummaryDto::getCode, Function.identity(), (a, b) -> a));
        List<DebtAccount> accounts = dashboard.getUserDebtAccounts() == null ? List.of() : dashboard.getUserDebtAccounts();

        List<CardSummary> cards = accounts.stream()
                .filter(a -> !Boolean.FALSE.equals(a.getActive()))
                .filter(a -> provider == null || (a.getFinancialProvider() != null
                        && provider.equals(a.getFinancialProvider().getCode())))
                .map(a -> summary(a, summaries.get(a.getCode())))
                .sorted(Comparator.comparing(CardSummary::monthly).reversed()
                        .thenComparing(c -> c.account().getName(), Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();

        model.addAttribute("cards", cards);
        model.addAttribute("monthlyTotal", cards.stream().map(CardSummary::monthly).reduce(BigDecimal.ZERO, BigDecimal::add));
        model.addAttribute("providers", findAllFinancialProviderUseCase.getActiveByEmail(email));
        model.addAttribute("provider", provider);
        model.addAttribute("providerName", provider == null ? null : cards.stream().findFirst()
                .map(CardSummary::providerName).orElse(provider));
        model.addAttribute("statementTypes", AccountStatementType.values());
        return "cards/index";
    }

    private CardSummary summary(DebtAccount account, AccountSummaryDto s) {
        String providerName = account.getFinancialProvider() != null ? account.getFinancialProvider().getName() : null;
        if (s == null) {
            return new CardSummary(account, cardColors.of(account), providerName, BigDecimal.ZERO, BigDecimal.ZERO, 0, 0);
        }
        return new CardSummary(account, cardColors.of(account), providerName,
                BigDecimal.valueOf(s.getMonthlyTotal() == null ? 0 : s.getMonthlyTotal()),
                BigDecimal.valueOf(s.getEstimatedRemainingBalance() == null ? 0 : s.getEstimatedRemainingBalance()),
                s.getActiveDebtCount(), s.getMaxMonthsRemaining());
    }
}
