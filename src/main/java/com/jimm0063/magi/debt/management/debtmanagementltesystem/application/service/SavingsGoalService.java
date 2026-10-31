package com.jimm0063.magi.debt.management.debtmanagementltesystem.application.service;

import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.application.port.in.ManageSavingsGoalUseCase;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.application.port.out.DebtRepository;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.application.port.out.SavingsGoalContributionRepository;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.application.port.out.SavingsGoalRepository;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.dto.SavingsGoalStatusDto;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.enums.SavingsGoalStatus;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.exceptions.EntityNotFoundException;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.model.Debt;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.model.SavingsGoal;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.model.SavingsGoalContribution;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.utils.PayoffSimulator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class SavingsGoalService implements ManageSavingsGoalUseCase {
    private final SavingsGoalRepository savingsGoalRepository;
    private final SavingsGoalContributionRepository contributionRepository;
    private final DebtRepository debtRepository;

    public SavingsGoalService(SavingsGoalRepository savingsGoalRepository,
                              SavingsGoalContributionRepository contributionRepository,
                              DebtRepository debtRepository) {
        this.savingsGoalRepository = savingsGoalRepository;
        this.contributionRepository = contributionRepository;
        this.debtRepository = debtRepository;
    }

    @Override
    public List<SavingsGoalStatusDto> getByEmail(String email) {
        List<Debt> debts = debtRepository.findAllDebtsByUser(email);
        Map<Integer, List<SavingsGoalContribution>> contributions = contributionRepository.findAllByUser(email);
        LocalDate today = LocalDate.now();
        return savingsGoalRepository.findAllActiveByUser(email).stream()
                .map(g -> status(g, contributions.getOrDefault(g.getId(), List.of()), debts, today))
                .sorted(Comparator.comparing((SavingsGoalStatusDto s) -> s.goal().getStatus()))
                .toList();
    }

    @Override
    public SavingsGoalStatusDto getStatus(String email, Integer goalId) {
        SavingsGoal goal = find(goalId);
        return status(goal, contributionRepository.findAllByGoal(goalId), debtRepository.findAllDebtsByUser(email),
                LocalDate.now());
    }

    @Override
    public SavingsGoal create(String email, String name, BigDecimal targetAmount, LocalDate targetDate, Integer debtId) {
        SavingsGoal goal = new SavingsGoal();
        apply(goal, email, name, targetAmount, targetDate, debtId);
        goal.setStatus(SavingsGoalStatus.ACTIVE);
        return savingsGoalRepository.save(goal, email);
    }

    @Override
    public SavingsGoal update(String email, Integer goalId, String name, BigDecimal targetAmount, LocalDate targetDate,
                              Integer debtId) {
        SavingsGoal goal = find(goalId);
        apply(goal, email, name, targetAmount, targetDate, debtId);
        return savingsGoalRepository.update(goal);
    }

    private void apply(SavingsGoal goal, String email, String name, BigDecimal targetAmount, LocalDate targetDate,
                       Integer debtId) {
        if (debtId != null) {
            Debt debt = debtRepository.findAllDebtsByUser(email).stream()
                    .filter(d -> debtId.equals(d.getId()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("That debt is no longer active"));
            goal.setDebtId(debt.getId());
            goal.setDebtHash(debt.getHashSum());
            goal.setTargetAmount(PayoffSimulator.payoffBalance(debt));
            goal.setName(name == null || name.isBlank() ? debt.getDescription() : name.trim());
        } else {
            if (name == null || name.isBlank()) throw new IllegalArgumentException("Give the goal a name");
            if (targetAmount == null || targetAmount.signum() <= 0) throw new IllegalArgumentException("Enter the amount to save");
            goal.setDebtId(null);
            goal.setDebtHash(null);
            goal.setTargetAmount(targetAmount);
            goal.setName(name.trim());
        }
        goal.setTargetDate(targetDate);
    }

    @Override
    public SavingsGoalContribution contribute(Integer goalId, BigDecimal amount, LocalDate date, String note) {
        if (amount == null || amount.signum() == 0) throw new IllegalArgumentException("Enter an amount");
        find(goalId);
        if (amount.signum() < 0) {
            BigDecimal saved = saved(contributionRepository.findAllByGoal(goalId));
            if (saved.add(amount).signum() < 0) throw new IllegalArgumentException("You can't take out more than what is saved");
        }
        SavingsGoalContribution c = new SavingsGoalContribution();
        c.setAmount(amount);
        c.setContributionDate(date != null ? date : LocalDate.now());
        c.setNote(note == null || note.isBlank() ? null : note.trim());
        return contributionRepository.save(c, goalId);
    }

    @Override
    public void deleteContribution(Integer contributionId) {
        contributionRepository.delete(contributionId);
    }

    @Override
    @Transactional
    public SavingsGoalStatusDto payOffDebt(String email, Integer goalId) {
        SavingsGoalStatusDto status = getStatus(email, goalId);
        if (!status.goal().isOpen()) throw new IllegalArgumentException("This goal is already closed");
        Debt debt = status.debt();
        if (debt == null) throw new IllegalArgumentException("The linked debt is no longer active");
        BigDecimal balance = PayoffSimulator.payoffBalance(debt);
        if (status.saved().compareTo(balance) < 0) throw new IllegalArgumentException("Not enough saved yet");

        debt.setCurrentInstallment(debt.getMaxFinancingTerm());
        debt.setActive(false);
        debtRepository.update(debt);

        SavingsGoalContribution used = new SavingsGoalContribution();
        used.setAmount(balance.negate());
        used.setContributionDate(LocalDate.now());
        used.setNote(debt.getDescription());
        contributionRepository.save(used, goalId);

        setStatus(status.goal(), SavingsGoalStatus.COMPLETED);
        return getStatus(email, goalId);
    }

    @Override
    public SavingsGoal complete(Integer goalId) {
        return setStatus(find(goalId), SavingsGoalStatus.COMPLETED);
    }

    @Override
    public SavingsGoal cancel(Integer goalId) {
        return setStatus(find(goalId), SavingsGoalStatus.CANCELLED);
    }

    @Override
    public SavingsGoal reopen(Integer goalId) {
        return setStatus(find(goalId), SavingsGoalStatus.ACTIVE);
    }

    @Override
    public void delete(Integer goalId) {
        savingsGoalRepository.delete(goalId);
    }

    private SavingsGoal setStatus(SavingsGoal goal, SavingsGoalStatus status) {
        goal.setStatus(status);
        goal.setCompletedAt(status == SavingsGoalStatus.ACTIVE ? null : Instant.now());
        return savingsGoalRepository.update(goal);
    }

    private SavingsGoal find(Integer goalId) {
        return savingsGoalRepository.findById(goalId)
                .orElseThrow(() -> new EntityNotFoundException("Savings goal " + goalId + " not found"));
    }

    static SavingsGoalStatusDto status(SavingsGoal goal, List<SavingsGoalContribution> contributions,
                                       List<Debt> activeDebts, LocalDate today) {
        BigDecimal saved = saved(contributions);
        Debt debt = goal.isLinkedToDebt() ? linkedDebt(goal, activeDebts) : null;
        boolean debtGone = goal.isLinkedToDebt() && debt == null;
        BigDecimal target = debt != null ? PayoffSimulator.payoffBalance(debt)
                : goal.getTargetAmount() != null ? goal.getTargetAmount() : BigDecimal.ZERO;
        BigDecimal remaining = target.subtract(saved).max(BigDecimal.ZERO);
        int percent = target.signum() <= 0 ? 0
                : saved.multiply(BigDecimal.valueOf(100)).divide(target, 0, RoundingMode.DOWN).min(BigDecimal.valueOf(100)).max(BigDecimal.ZERO).intValue();

        Integer monthsLeft = null;
        BigDecimal suggested = null;
        BigDecimal behindBy = BigDecimal.ZERO;
        if (goal.getTargetDate() != null) {
            monthsLeft = (int) Math.max(0, YearMonth.from(today).until(YearMonth.from(goal.getTargetDate()), ChronoUnit.MONTHS));
            if (remaining.signum() > 0)
                suggested = remaining.divide(BigDecimal.valueOf(Math.max(1, monthsLeft)), 0, RoundingMode.UP);
            if (goal.isOpen() && remaining.signum() > 0 && goal.getCreatedAt() != null) {
                LocalDate start = goal.getCreatedAt().atZone(ZoneId.systemDefault()).toLocalDate();
                long total = ChronoUnit.DAYS.between(start, goal.getTargetDate());
                long elapsed = ChronoUnit.DAYS.between(start, today);
                if (total > 0 && elapsed > 0) {
                    BigDecimal expected = target.multiply(BigDecimal.valueOf(Math.min(elapsed, total)))
                            .divide(BigDecimal.valueOf(total), 0, RoundingMode.HALF_UP);
                    BigDecimal gap = expected.subtract(saved);
                    // ignore small gaps (under 5% of the target)
                    if (gap.compareTo(target.multiply(new BigDecimal("0.05"))) > 0) behindBy = gap;
                }
            }
        }
        return new SavingsGoalStatusDto(goal, saved, target, remaining, percent, debt, debtGone, monthsLeft,
                suggested, behindBy, contributions);
    }

    /** By id, or by hash after a "rebuild from 0" re-inserted the debt with a new id. */
    private static Debt linkedDebt(SavingsGoal goal, List<Debt> activeDebts) {
        return activeDebts.stream().filter(d -> Objects.equals(goal.getDebtId(), d.getId())).findFirst()
                .or(() -> goal.getDebtHash() == null ? java.util.Optional.empty()
                        : activeDebts.stream().filter(d -> goal.getDebtHash().equals(d.getHashSum())).findFirst())
                .filter(d -> PayoffSimulator.monthsLeft(d) > 0)
                .orElse(null);
    }

    private static BigDecimal saved(List<SavingsGoalContribution> contributions) {
        return contributions.stream().map(SavingsGoalContribution::getAmount).filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
