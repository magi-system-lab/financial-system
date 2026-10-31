package com.jimm0063.magi.debt.management.debtmanagementltesystem.infrastructure.web;

import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.application.port.in.ManageSavingsGoalUseCase;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.dto.SavingsGoalStatusDto;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.model.SavingsGoal;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.model.SavingsGoalContribution;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Savings goals: put money aside to pay a debt off (or for anything else, e.g. an emergency fund). */
@RestController
@RequestMapping("/savings/goal")
public class SavingsGoalController {
    private final ManageSavingsGoalUseCase manageSavingsGoalUseCase;

    public SavingsGoalController(ManageSavingsGoalUseCase manageSavingsGoalUseCase) {
        this.manageSavingsGoalUseCase = manageSavingsGoalUseCase;
    }

    @GetMapping("/all/{email}")
    public ResponseEntity<List<SavingsGoalStatusDto>> getAll(@PathVariable String email) {
        return ResponseEntity.ok(manageSavingsGoalUseCase.getByEmail(email));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SavingsGoalStatusDto> get(@PathVariable Integer id, @RequestHeader String email) {
        return ResponseEntity.ok(manageSavingsGoalUseCase.getStatus(email, id));
    }

    @PostMapping("/{email}")
    public ResponseEntity<SavingsGoal> create(@PathVariable String email,
                                              @RequestParam(required = false) String name,
                                              @RequestParam(required = false) BigDecimal targetAmount,
                                              @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate targetDate,
                                              @RequestParam(required = false) Integer debtId) {
        return ResponseEntity.ok(manageSavingsGoalUseCase.create(email, name, targetAmount, targetDate, debtId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<SavingsGoal> update(@PathVariable Integer id, @RequestHeader String email,
                                              @RequestParam(required = false) String name,
                                              @RequestParam(required = false) BigDecimal targetAmount,
                                              @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate targetDate,
                                              @RequestParam(required = false) Integer debtId) {
        return ResponseEntity.ok(manageSavingsGoalUseCase.update(email, id, name, targetAmount, targetDate, debtId));
    }

    @PostMapping("/{id}/contribution")
    public ResponseEntity<SavingsGoalContribution> contribute(@PathVariable Integer id, @RequestParam BigDecimal amount,
                                                              @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                                              @RequestParam(required = false) String note) {
        return ResponseEntity.ok(manageSavingsGoalUseCase.contribute(id, amount, date, note));
    }

    @DeleteMapping("/contribution/{contributionId}")
    public ResponseEntity<Void> deleteContribution(@PathVariable Integer contributionId) {
        manageSavingsGoalUseCase.deleteContribution(contributionId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/pay-off")
    public ResponseEntity<SavingsGoalStatusDto> payOff(@PathVariable Integer id, @RequestHeader String email) {
        return ResponseEntity.ok(manageSavingsGoalUseCase.payOffDebt(email, id));
    }

    @PatchMapping("/{id}/complete")
    public ResponseEntity<SavingsGoal> complete(@PathVariable Integer id) {
        return ResponseEntity.ok(manageSavingsGoalUseCase.complete(id));
    }

    @PatchMapping("/{id}/cancel")
    public ResponseEntity<SavingsGoal> cancel(@PathVariable Integer id) {
        return ResponseEntity.ok(manageSavingsGoalUseCase.cancel(id));
    }

    @PatchMapping("/{id}/reopen")
    public ResponseEntity<SavingsGoal> reopen(@PathVariable Integer id) {
        return ResponseEntity.ok(manageSavingsGoalUseCase.reopen(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        manageSavingsGoalUseCase.delete(id);
        return ResponseEntity.noContent().build();
    }
}
