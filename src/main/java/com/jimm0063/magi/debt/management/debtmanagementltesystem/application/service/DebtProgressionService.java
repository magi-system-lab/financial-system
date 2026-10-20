package com.jimm0063.magi.debt.management.debtmanagementltesystem.application.service;

import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.application.port.in.GetDebtProgressionUseCase;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.application.port.out.DebtRepository;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.application.port.out.UpcomingDebtRepository;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.dto.CardProgressionDto;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.dto.MonthProgressionDto;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.model.Debt;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.model.DebtAccount;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.model.UpcomingDebt;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class DebtProgressionService implements GetDebtProgressionUseCase {

    private static final DateTimeFormatter DISPLAY_FORMAT =
            DateTimeFormatter.ofPattern("MMMM yyyy", new Locale("es", "MX"));

    /** Grouping key for upcoming debts that were registered without a card. */
    static final String NO_CARD_CODE = "__NO_CARD__";
    private static final String NO_CARD_NAME = "Sin tarjeta asignada";

    private final DebtRepository debtRepository;
    private final UpcomingDebtRepository upcomingDebtRepository;

    public DebtProgressionService(DebtRepository debtRepository, UpcomingDebtRepository upcomingDebtRepository) {
        this.debtRepository = debtRepository;
        this.upcomingDebtRepository = upcomingDebtRepository;
    }

    @Override
    public List<MonthProgressionDto> getProgression(String email, YearMonth targetMonth) {
        return buildProgression(YearMonth.now(), targetMonth,
                debtRepository.findAllDebtsByUser(email),
                upcomingDebtRepository.findAllPendingByUser(email));
    }

    List<MonthProgressionDto> buildProgression(YearMonth current, YearMonth targetMonth,
                                               List<Debt> allDebts, List<UpcomingDebt> upcomingDebts) {
        Map<String, List<Debt>> byAccountCode = new LinkedHashMap<>();
        Map<String, List<UpcomingDebt>> upcomingByAccountCode = new LinkedHashMap<>();
        Map<String, String> accountNameByCode = new LinkedHashMap<>();
        for (Debt debt : allDebts) {
            String code = debt.getDebtAccount().getCode();
            byAccountCode.computeIfAbsent(code, k -> new ArrayList<>()).add(debt);
            accountNameByCode.putIfAbsent(code, debt.getDebtAccount().getName());
        }
        // Pending upcoming purchases: not in the debt table yet, but they will be charged
        for (UpcomingDebt upcoming : upcomingDebts) {
            if (!upcoming.isPending() || upcoming.getMonthlyPayment() == null || upcoming.getFirstPaymentDate() == null)
                continue;
            DebtAccount account = upcoming.getDebtAccount();
            String code = account != null ? account.getCode() : NO_CARD_CODE;
            upcomingByAccountCode.computeIfAbsent(code, k -> new ArrayList<>()).add(upcoming);
            accountNameByCode.putIfAbsent(code, account != null ? account.getName() : NO_CARD_NAME);
        }

        List<MonthProgressionDto> result = new ArrayList<>();

        for (YearMonth month = current.plusMonths(1); !month.isAfter(targetMonth); month = month.plusMonths(1)) {
            long offset = current.until(month, java.time.temporal.ChronoUnit.MONTHS);
            final YearMonth thisMonth = month;

            List<CardProgressionDto> cards = new ArrayList<>();
            BigDecimal totalPayment = BigDecimal.ZERO;

            for (Map.Entry<String, String> account : accountNameByCode.entrySet()) {
                List<Debt> debts = byAccountCode.getOrDefault(account.getKey(), List.of());

                List<Debt> closingDebts = debts.stream()
                        .filter(d -> (d.getMaxFinancingTerm() - d.getCurrentInstallment()) == offset)
                        .toList();

                long activeCount = debts.stream()
                        .filter(d -> (d.getMaxFinancingTerm() - d.getCurrentInstallment()) > offset)
                        .count();

                List<Debt> duePayments = debts.stream()
                        .filter(d -> (d.getMaxFinancingTerm() - d.getCurrentInstallment()) >= offset)
                        .toList();

                BigDecimal debtPayment = duePayments.stream()
                        .map(Debt::getMonthlyPayment)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                BigDecimal remainingAmount = duePayments.stream()
                        .map(d -> d.getMonthlyPayment()
                                .multiply(BigDecimal.valueOf(d.getMaxFinancingTerm() - d.getCurrentInstallment() - offset + 1)))
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                List<UpcomingDebt> chargedUpcoming = upcomingByAccountCode.getOrDefault(account.getKey(), List.of()).stream()
                        .filter(u -> u.chargedIn(thisMonth))
                        .toList();

                BigDecimal upcomingPayment = chargedUpcoming.stream()
                        .map(UpcomingDebt::getMonthlyPayment)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                BigDecimal upcomingRemaining = chargedUpcoming.stream()
                        .map(u -> u.getMonthlyPayment()
                                .multiply(BigDecimal.valueOf(thisMonth.until(u.lastPaymentMonth(), java.time.temporal.ChronoUnit.MONTHS) + 1)))
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                BigDecimal cardPayment = debtPayment.add(upcomingPayment);
                if (cardPayment.compareTo(BigDecimal.ZERO) > 0) {
                    cards.add(new CardProgressionDto(account.getKey(), account.getValue(), closingDebts, (int) activeCount,
                            cardPayment, remainingAmount.add(upcomingRemaining), chargedUpcoming, upcomingPayment));
                    totalPayment = totalPayment.add(cardPayment);
                }
            }

            String displayName = month.format(DISPLAY_FORMAT);
            result.add(new MonthProgressionDto(month, displayName, totalPayment, cards));
        }

        return result;
    }
}
