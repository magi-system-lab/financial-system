package com.jimm0063.magi.debt.management.debtmanagementltesystem.infrastructure.web;

import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.application.port.in.FindAllUpcomingDebtsUseCase;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.application.port.in.GetPaymentProjectionUseCase;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.application.port.in.ManageUpcomingDebtUseCase;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.dto.PaymentProjectionDto;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.model.UpcomingDebt;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.infrastructure.model.CreateUpcomingDebtReq;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/upcoming/debt")
public class UpcomingDebtController {
    private final FindAllUpcomingDebtsUseCase findAllUpcomingDebtsUseCase;
    private final ManageUpcomingDebtUseCase manageUpcomingDebtUseCase;
    private final GetPaymentProjectionUseCase getPaymentProjectionUseCase;

    public UpcomingDebtController(FindAllUpcomingDebtsUseCase findAllUpcomingDebtsUseCase,
                                  ManageUpcomingDebtUseCase manageUpcomingDebtUseCase,
                                  GetPaymentProjectionUseCase getPaymentProjectionUseCase) {
        this.findAllUpcomingDebtsUseCase = findAllUpcomingDebtsUseCase;
        this.manageUpcomingDebtUseCase = manageUpcomingDebtUseCase;
        this.getPaymentProjectionUseCase = getPaymentProjectionUseCase;
    }

    @PostMapping("/{userEmail}")
    public ResponseEntity<UpcomingDebt> create(@PathVariable String userEmail, @RequestBody CreateUpcomingDebtReq req) {
        return ResponseEntity.ok(manageUpcomingDebtUseCase.create(userEmail, req.toCommand()));
    }

    @PutMapping("/{upcomingDebtId}")
    public ResponseEntity<UpcomingDebt> update(@PathVariable Integer upcomingDebtId, @RequestBody CreateUpcomingDebtReq req) {
        return ResponseEntity.ok(manageUpcomingDebtUseCase.update(upcomingDebtId, req.toCommand()));
    }

    @PatchMapping("/{upcomingDebtId}/start")
    public ResponseEntity<UpcomingDebt> markAsStarted(@PathVariable Integer upcomingDebtId) {
        return ResponseEntity.ok(manageUpcomingDebtUseCase.markAsStarted(upcomingDebtId));
    }

    @PatchMapping("/{upcomingDebtId}/cancel")
    public ResponseEntity<UpcomingDebt> cancel(@PathVariable Integer upcomingDebtId) {
        return ResponseEntity.ok(manageUpcomingDebtUseCase.cancel(upcomingDebtId));
    }

    @PatchMapping("/{upcomingDebtId}/reopen")
    public ResponseEntity<UpcomingDebt> reopen(@PathVariable Integer upcomingDebtId) {
        return ResponseEntity.ok(manageUpcomingDebtUseCase.reopen(upcomingDebtId));
    }

    @DeleteMapping("/{upcomingDebtId}")
    public ResponseEntity<Void> delete(@PathVariable Integer upcomingDebtId) {
        manageUpcomingDebtUseCase.delete(upcomingDebtId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/all/{userEmail}")
    public ResponseEntity<List<UpcomingDebt>> getAll(@PathVariable String userEmail) {
        return ResponseEntity.ok(findAllUpcomingDebtsUseCase.getByEmail(userEmail));
    }

    @GetMapping("/pending/{userEmail}")
    public ResponseEntity<List<UpcomingDebt>> getPending(@PathVariable String userEmail) {
        return ResponseEntity.ok(findAllUpcomingDebtsUseCase.getPendingByEmail(userEmail));
    }

    @GetMapping("/projection/{userEmail}")
    public ResponseEntity<PaymentProjectionDto> getProjection(@PathVariable String userEmail,
                                                              @RequestParam(defaultValue = "12") int months) {
        return ResponseEntity.ok(getPaymentProjectionUseCase.getProjection(userEmail, months));
    }
}
