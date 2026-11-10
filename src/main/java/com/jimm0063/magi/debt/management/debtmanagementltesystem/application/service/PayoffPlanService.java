package com.jimm0063.magi.debt.management.debtmanagementltesystem.application.service;

import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.application.port.in.GetDebtFreeOptionsUseCase;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.application.port.in.GetPayoffPlanUseCase;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.application.port.out.DebtRepository;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.application.port.out.FixedExpenseRepository;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.application.port.out.UserRepository;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.dto.DebtFreeOptionsDto;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.dto.PayoffPlanDto;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.enums.PayoffStrategy;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.model.Debt;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.model.DebtSysUser;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.model.FixedExpense;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.utils.DebtFreeAdvisor;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.utils.PayoffSimulator;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import java.util.Objects;

@Service
public class PayoffPlanService implements GetPayoffPlanUseCase, GetDebtFreeOptionsUseCase {
    private final DebtRepository debtRepository;
    private final UserRepository userRepository;
    private final FixedExpenseRepository fixedExpenseRepository;

    public PayoffPlanService(DebtRepository debtRepository, UserRepository userRepository,
                             FixedExpenseRepository fixedExpenseRepository) {
        this.debtRepository = debtRepository;
        this.userRepository = userRepository;
        this.fixedExpenseRepository = fixedExpenseRepository;
    }

    @Override
    public PayoffPlanDto getPlan(String email, BigDecimal extraMonthly, BigDecimal lumpSum,
                                 PayoffStrategy strategy, boolean snowball) {
        return PayoffSimulator.simulate(debtRepository.findAllDebtsByUser(email), YearMonth.now(),
                extraMonthly, lumpSum, strategy, snowball);
    }

    @Override
    public DebtFreeOptionsDto getOptions(String email) {
        List<Debt> debts = debtRepository.findAllDebtsByUser(email);
        Double salary = userRepository.findUserByEmail(email).map(DebtSysUser::getSalary).orElse(null);
        BigDecimal fixed = fixedExpenseRepository.findAllFixedExpenseByEmailAndActiveTrue(email).stream()
                .map(FixedExpense::getMonthlyCost).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
        return DebtFreeAdvisor.options(debts, YearMonth.now(), freeMoney(salary, fixed, debts));
    }

    /** Salary − fixed expenses − next month's debt payments; null without salary. */
    static BigDecimal freeMoney(Double salary, BigDecimal fixed, List<Debt> debts) {
        if (salary == null || salary <= 0) return null;
        BigDecimal payments = debts.stream()
                .filter(d -> d.getMonthlyPayment() != null && PayoffSimulator.monthsLeft(d) >= 1)
                .map(Debt::getMonthlyPayment).reduce(BigDecimal.ZERO, BigDecimal::add);
        return BigDecimal.valueOf(salary).subtract(fixed).subtract(payments);
    }
}
