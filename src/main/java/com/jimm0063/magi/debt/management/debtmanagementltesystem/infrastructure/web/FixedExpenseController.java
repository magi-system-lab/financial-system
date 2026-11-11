package com.jimm0063.magi.debt.management.debtmanagementltesystem.infrastructure.web;

import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.application.port.in.FindAllFixedExpenseCatalogsUseCase;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.application.port.in.FindAllFixedExpenseUseCase;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.application.port.in.MoveBetweenSectionsUseCase;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.application.port.out.FixedExpenseCatalogRepository;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.application.port.out.FixedExpenseRepository;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.enums.FixedExpenseKind;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.model.FixedExpense;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.model.FixedExpenseCatalog;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.infrastructure.mapper.FixedExpenseCatalogMapper;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.infrastructure.model.CreateFixedExpenseCatalogReq;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.infrastructure.model.FixedExpenseReq;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/fixed/expense")
public class FixedExpenseController {
    private final FixedExpenseCatalogRepository fixedExpenseCatalogRepository;
    private final FixedExpenseRepository fixedExpenseRepository;
    private final FixedExpenseCatalogMapper fixedExpenseCatalogMapper;
    private final FindAllFixedExpenseUseCase findAllFixedExpenseUseCase;
    private final FindAllFixedExpenseCatalogsUseCase findAllFixedExpenseCatalogsUseCase;
    private final MoveBetweenSectionsUseCase moveBetweenSectionsUseCase;

    public FixedExpenseController(FixedExpenseCatalogRepository fixedExpenseCatalogRepository,
                                  FixedExpenseRepository fixedExpenseRepository,
                                  FixedExpenseCatalogMapper fixedExpenseCatalogMapper,
                                  FindAllFixedExpenseUseCase findAllFixedExpenseUseCase,
                                  FindAllFixedExpenseCatalogsUseCase findAllFixedExpenseCatalogsUseCase,
                                  MoveBetweenSectionsUseCase moveBetweenSectionsUseCase) {
        this.fixedExpenseCatalogRepository = fixedExpenseCatalogRepository;
        this.fixedExpenseRepository = fixedExpenseRepository;
        this.fixedExpenseCatalogMapper = fixedExpenseCatalogMapper;
        this.findAllFixedExpenseUseCase = findAllFixedExpenseUseCase;
        this.findAllFixedExpenseCatalogsUseCase = findAllFixedExpenseCatalogsUseCase;
        this.moveBetweenSectionsUseCase = moveBetweenSectionsUseCase;
    }

    @GetMapping("/catalog/all")
    public ResponseEntity<List<FixedExpenseCatalog>> getFixedExpenseCatalogs() {
        return ResponseEntity.ok(findAllFixedExpenseCatalogsUseCase.getCatalogList());
    }

    @PostMapping("/catalog")
    public ResponseEntity<FixedExpenseCatalog> createFixedExpenseCatalog(@RequestBody CreateFixedExpenseCatalogReq createFixedExpenseCatalogReq) {
        return ResponseEntity.ok(fixedExpenseCatalogRepository.save(fixedExpenseCatalogMapper.toModel(createFixedExpenseCatalogReq)
        ));
    }

    @PutMapping("/catalog")
    public ResponseEntity<FixedExpenseCatalog> updateFixedExpenseCatalog(@RequestBody FixedExpenseCatalog fixedExpenseCatalog) {
        return ResponseEntity.ok(fixedExpenseCatalogRepository.save(fixedExpenseCatalog));
    }

    @DeleteMapping("/catalog/{fixedExpenseCatalogId}")
    public ResponseEntity deleteFixedExpenseCatalog(@PathVariable Integer fixedExpenseCatalogId) {
        fixedExpenseCatalogRepository.delete(fixedExpenseCatalogId);
        return ResponseEntity.ok().build();
    }

    @PostMapping
    public ResponseEntity<FixedExpense> createFixedExpense(@RequestBody FixedExpenseReq createFixedExpenseReq) {
        return ResponseEntity.ok(fixedExpenseRepository.save(createFixedExpenseReq, createFixedExpenseReq.getCatalogId()));
    }

    @PutMapping
    public ResponseEntity<FixedExpense> updateFixedExpense(@RequestBody FixedExpenseReq fixedExpenseReq) {
        return ResponseEntity.ok(fixedExpenseRepository.updateFromReq(fixedExpenseReq));
    }

    @PutMapping("/bulk")
    public ResponseEntity<List<FixedExpense>> updateFixedExpenses(@RequestBody List<FixedExpenseReq> fixedExpenseReqs) {
        return ResponseEntity.ok(fixedExpenseRepository.updateMultiple(fixedExpenseReqs));
    }

    @DeleteMapping("/{fixedExpenseId}")
    public ResponseEntity deleteFixedExpense(@PathVariable Integer fixedExpenseId) {
        fixedExpenseRepository.delete(fixedExpenseId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/bulk")
    public ResponseEntity deleteFixedExpenses(@RequestBody List<Integer> fixedExpenseIds) {
        fixedExpenseRepository.deleteMultiple(fixedExpenseIds);
        return ResponseEntity.ok().build();
    }

    /** Every fixed expense and credit of the user; {@code ?kind=EXPENSE|CREDIT} keeps one section only. */
    @GetMapping("/all/{userEmail}")
    public ResponseEntity<List<FixedExpense>> getFixedExpenses(@PathVariable String userEmail,
                                                               @RequestParam(required = false) FixedExpenseKind kind) {
        List<FixedExpense> all = findAllFixedExpenseUseCase.getByEmail(userEmail);
        if (kind == null) return ResponseEntity.ok(all);
        return ResponseEntity.ok(all.stream().filter(e -> FixedExpenseKind.orDefault(e.getKind()) == kind).toList());
    }

    /** Fixed expense → credit or back. */
    @PatchMapping("/{fixedExpenseId}/move")
    public ResponseEntity<List<FixedExpense>> move(@PathVariable Integer fixedExpenseId, @RequestParam FixedExpenseKind to) {
        return ResponseEntity.ok(moveBetweenSectionsUseCase.moveFixedExpenses(List.of(fixedExpenseId), to));
    }

    @PatchMapping("/bulk/move")
    public ResponseEntity<List<FixedExpense>> moveMany(@RequestBody List<Integer> fixedExpenseIds, @RequestParam FixedExpenseKind to) {
        return ResponseEntity.ok(moveBetweenSectionsUseCase.moveFixedExpenses(fixedExpenseIds, to));
    }

    /** A debt → credit / fixed expense (the debt is soft-deleted and ignored on future statements). */
    @PostMapping("/from-debt/{debtId}")
    public ResponseEntity<FixedExpense> fromDebt(@PathVariable Integer debtId, @RequestHeader String email,
                                                 @RequestParam FixedExpenseKind to, @RequestParam Integer catalogId) {
        return ResponseEntity.ok(moveBetweenSectionsUseCase.moveDebt(email, debtId, to, catalogId));
    }
}
