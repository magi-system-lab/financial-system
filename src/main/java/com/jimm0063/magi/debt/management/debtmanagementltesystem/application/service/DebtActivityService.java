package com.jimm0063.magi.debt.management.debtmanagementltesystem.application.service;

import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.application.port.in.GetDebtActivityUseCase;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.application.port.out.DebtRepository;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.dto.DebtActivityDto;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.model.Debt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Month-by-month debt activity.
 * <ul>
 *   <li><b>Added</b>: the first row ever saved for a purchase (same hash). "Rebuild from 0" imports
 *       re-insert existing debts as new rows; those are not counted again.</li>
 *   <li><b>Finished</b>: deactivated with every installment paid. Deleted debts and debts
 *       deactivated by a rebuild still have installments left, so they don't count.</li>
 * </ul>
 */
@Service
public class DebtActivityService implements GetDebtActivityUseCase {

    private final DebtRepository debtRepository;
    private final Clock clock;

    @Autowired
    public DebtActivityService(DebtRepository debtRepository) {
        this(debtRepository, Clock.systemDefaultZone());
    }

    DebtActivityService(DebtRepository debtRepository, Clock clock) {
        this.debtRepository = debtRepository;
        this.clock = clock;
    }

    @Override
    public List<DebtActivityDto> getActivity(String email, int months) {
        List<Debt> history = debtRepository.findDebtHistoryByUser(email);
        YearMonth current = YearMonth.now(clock);
        YearMonth first = current.minusMonths(Math.max(1, months) - 1L);

        // Earliest row per purchase
        Map<String, Debt> firstRow = new HashMap<>();
        for (Debt d : history) {
            if (d.getCreatedAt() == null) continue;
            firstRow.merge(key(d), d, (a, b) -> b.getCreatedAt().isBefore(a.getCreatedAt()) ? b : a);
        }

        List<DebtActivityDto> result = new ArrayList<>();
        for (YearMonth m = first; !m.isAfter(current); m = m.plusMonths(1)) {
            YearMonth month = m;
            List<Debt> added = firstRow.values().stream()
                    .filter(d -> month.equals(monthOf(d.getCreatedAt())))
                    .sorted(Comparator.comparing(Debt::getCreatedAt))
                    .toList();
            Map<String, Debt> finishedByKey = new HashMap<>();
            history.stream()
                    .filter(DebtActivityService::isFinished)
                    .filter(d -> month.equals(monthOf(d.getUpdatedAt())))
                    .forEach(d -> finishedByKey.putIfAbsent(key(d), d));
            List<Debt> finished = finishedByKey.values().stream()
                    .sorted(Comparator.comparing(Debt::getUpdatedAt))
                    .toList();
            result.add(new DebtActivityDto(month, added, sum(added), finished, sum(finished)));
        }
        return result;
    }

    static boolean isFinished(Debt d) {
        return Boolean.FALSE.equals(d.getActive()) && d.getUpdatedAt() != null
                && d.getCurrentInstallment() != null && d.getMaxFinancingTerm() != null
                && d.getCurrentInstallment() >= d.getMaxFinancingTerm();
    }

    private YearMonth monthOf(Instant instant) {
        return instant == null ? null : YearMonth.from(instant.atZone(clock.getZone()));
    }

    private static String key(Debt d) {
        return d.getHashSum() != null ? d.getHashSum() : "id:" + d.getId();
    }

    private static BigDecimal sum(List<Debt> debts) {
        return debts.stream().map(Debt::getMonthlyPayment).filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
