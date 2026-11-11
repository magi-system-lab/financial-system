package com.jimm0063.magi.debt.management.debtmanagementltesystem.application.service;

import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.application.port.in.GetPayoffRecommendationUseCase;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.application.port.in.PayOffWithSavingsUseCase;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.application.port.out.DebtRepository;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.application.port.out.FixedExpenseRepository;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.application.port.out.UserRepository;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.dto.PayoffRecommendationDto;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.model.Debt;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.model.DebtSysUser;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.model.FixedExpense;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.utils.PayoffAdvisor;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.utils.PayoffSimulator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

@Service
public class PayoffRecommendationService implements GetPayoffRecommendationUseCase, PayOffWithSavingsUseCase {
    private final DebtRepository debtRepository;
    private final UserRepository userRepository;
    private final FixedExpenseRepository fixedExpenseRepository;

    public PayoffRecommendationService(DebtRepository debtRepository, UserRepository userRepository,
                                       FixedExpenseRepository fixedExpenseRepository) {
        this.debtRepository = debtRepository;
        this.userRepository = userRepository;
        this.fixedExpenseRepository = fixedExpenseRepository;
    }

    @Override
    public PayoffRecommendationDto getRecommendation(String email) {
        Double savings = userRepository.findUserByEmail(email).map(DebtSysUser::getSavings).orElse(null);
        BigDecimal fixed = FixedExpense.monthlyTotal(
                fixedExpenseRepository.findAllFixedExpenseByEmailAndActiveTrue(email), java.time.YearMonth.now());
        return PayoffAdvisor.recommend(debtRepository.findAllDebtsByUser(email),
                savings == null ? BigDecimal.ZERO : BigDecimal.valueOf(savings), fixed);
    }

    @Override
    @Transactional
    public List<Debt> payOffWithSavings(String email, List<Integer> debtIds) {
        if (debtIds == null || debtIds.isEmpty()) throw new IllegalArgumentException("Pick the debts you paid off");
        List<Debt> paid = debtRepository.findAllDebtsByUser(email).stream()
                .filter(d -> d.getId() != null && debtIds.contains(d.getId()) && PayoffSimulator.monthsLeft(d) > 0
                        && d.getMonthlyPayment() != null)
                .toList();
        if (paid.isEmpty()) throw new IllegalArgumentException("Those debts are no longer active");
        BigDecimal cost = paid.stream().map(PayoffAdvisor::cost).reduce(BigDecimal.ZERO, BigDecimal::add);
        for (Debt d : paid) {
            d.setCurrentInstallment(d.getMaxFinancingTerm());
            d.setActive(false);
            debtRepository.update(d);
        }
        double savings = userRepository.findUserByEmail(email).map(DebtSysUser::getSavings).orElse(0.0);
        userRepository.updateFinancials(email, null, Math.max(0, savings - cost.doubleValue()));
        return paid;
    }
}
