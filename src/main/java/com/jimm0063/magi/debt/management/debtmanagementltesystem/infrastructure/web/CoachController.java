package com.jimm0063.magi.debt.management.debtmanagementltesystem.infrastructure.web;

import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.application.port.in.GetCoachTipsUseCase;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.application.port.in.GetPayoffPlanUseCase;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.dto.CoachTipDto;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.dto.PayoffPlanDto;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.enums.PayoffStrategy;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

/** Financial coach: attack plan to pay debts off sooner + rule-based advice. */
@RestController
@RequestMapping("/coach")
public class CoachController {
    private final GetPayoffPlanUseCase getPayoffPlanUseCase;
    private final GetCoachTipsUseCase getCoachTipsUseCase;

    public CoachController(GetPayoffPlanUseCase getPayoffPlanUseCase, GetCoachTipsUseCase getCoachTipsUseCase) {
        this.getPayoffPlanUseCase = getPayoffPlanUseCase;
        this.getCoachTipsUseCase = getCoachTipsUseCase;
    }

    @GetMapping("/plan/{email}")
    public ResponseEntity<PayoffPlanDto> plan(@PathVariable String email,
                                              @RequestParam(defaultValue = "0") BigDecimal extra,
                                              @RequestParam(defaultValue = "0") BigDecimal lump,
                                              @RequestParam(defaultValue = "FLOW") PayoffStrategy strategy,
                                              @RequestParam(defaultValue = "true") boolean snowball) {
        return ResponseEntity.ok(getPayoffPlanUseCase.getPlan(email, extra, lump, strategy, snowball));
    }

    @GetMapping("/tips/{email}")
    public ResponseEntity<List<CoachTipDto>> tips(@PathVariable String email) {
        return ResponseEntity.ok(getCoachTipsUseCase.getTips(email));
    }
}
