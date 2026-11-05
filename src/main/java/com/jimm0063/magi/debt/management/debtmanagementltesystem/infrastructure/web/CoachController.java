package com.jimm0063.magi.debt.management.debtmanagementltesystem.infrastructure.web;

import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.application.port.in.GetCoachTipsUseCase;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.application.port.in.GetPayoffPlanUseCase;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.dto.CoachTipDto;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.application.port.in.GetPayoffRecommendationUseCase;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.application.port.in.PayOffWithSavingsUseCase;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.dto.PayoffPlanDto;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.dto.PayoffRecommendationDto;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.model.Debt;
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
    private final GetPayoffRecommendationUseCase getPayoffRecommendationUseCase;
    private final PayOffWithSavingsUseCase payOffWithSavingsUseCase;

    public CoachController(GetPayoffPlanUseCase getPayoffPlanUseCase, GetCoachTipsUseCase getCoachTipsUseCase,
                           GetPayoffRecommendationUseCase getPayoffRecommendationUseCase,
                           PayOffWithSavingsUseCase payOffWithSavingsUseCase) {
        this.getPayoffPlanUseCase = getPayoffPlanUseCase;
        this.getCoachTipsUseCase = getCoachTipsUseCase;
        this.getPayoffRecommendationUseCase = getPayoffRecommendationUseCase;
        this.payOffWithSavingsUseCase = payOffWithSavingsUseCase;
    }

    @GetMapping("/plan/{email}")
    public ResponseEntity<PayoffPlanDto> plan(@PathVariable String email,
                                              @RequestParam(defaultValue = "0") BigDecimal extra,
                                              @RequestParam(defaultValue = "0") BigDecimal lump,
                                              @RequestParam(defaultValue = "FLOW") PayoffStrategy strategy,
                                              @RequestParam(defaultValue = "true") boolean snowball) {
        return ResponseEntity.ok(getPayoffPlanUseCase.getPlan(email, extra, lump, strategy, snowball));
    }

    @GetMapping("/recommendation/{email}")
    public ResponseEntity<PayoffRecommendationDto> recommendation(@PathVariable String email) {
        return ResponseEntity.ok(getPayoffRecommendationUseCase.getRecommendation(email));
    }

    /** The user paid these debts off with their savings: closes them and takes their cost from the savings. */
    @PostMapping("/recommendation/{email}/paid")
    public ResponseEntity<List<Debt>> paidWithSavings(@PathVariable String email, @RequestParam List<Integer> debtIds) {
        return ResponseEntity.ok(payOffWithSavingsUseCase.payOffWithSavings(email, debtIds));
    }

    @GetMapping("/tips/{email}")
    public ResponseEntity<List<CoachTipDto>> tips(@PathVariable String email) {
        return ResponseEntity.ok(getCoachTipsUseCase.getTips(email));
    }
}
