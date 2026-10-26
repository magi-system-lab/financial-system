package com.jimm0063.magi.debt.management.debtmanagementltesystem.infrastructure.web;

import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.application.port.in.GetDebtActivityUseCase;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.dto.DebtActivityDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/debt/activity")
public class DebtActivityController {
    private final GetDebtActivityUseCase getDebtActivityUseCase;

    public DebtActivityController(GetDebtActivityUseCase getDebtActivityUseCase) {
        this.getDebtActivityUseCase = getDebtActivityUseCase;
    }

    /** New purchases and finished debts per month, oldest first, ending with the current month. */
    @GetMapping("/{email}")
    public ResponseEntity<List<DebtActivityDto>> getActivity(@PathVariable String email,
                                                             @RequestParam(defaultValue = "6") int months) {
        return ResponseEntity.ok(getDebtActivityUseCase.getActivity(email, Math.min(Math.max(months, 1), 36)));
    }
}
