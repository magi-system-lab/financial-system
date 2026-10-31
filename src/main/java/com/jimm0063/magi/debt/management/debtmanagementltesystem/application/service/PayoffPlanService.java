package com.jimm0063.magi.debt.management.debtmanagementltesystem.application.service;

import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.application.port.in.GetPayoffPlanUseCase;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.application.port.out.DebtRepository;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.dto.PayoffPlanDto;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.enums.PayoffStrategy;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.utils.PayoffSimulator;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.YearMonth;

@Service
public class PayoffPlanService implements GetPayoffPlanUseCase {
    private final DebtRepository debtRepository;

    public PayoffPlanService(DebtRepository debtRepository) {
        this.debtRepository = debtRepository;
    }

    @Override
    public PayoffPlanDto getPlan(String email, BigDecimal extraMonthly, BigDecimal lumpSum,
                                 PayoffStrategy strategy, boolean snowball) {
        return PayoffSimulator.simulate(debtRepository.findAllDebtsByUser(email), YearMonth.now(),
                extraMonthly, lumpSum, strategy, snowball);
    }
}
