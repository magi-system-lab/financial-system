package com.jimm0063.magi.debt.management.debtmanagementltesystem.infrastructure.web;

import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.application.port.in.FindAllReceivablesUseCase;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.application.port.in.GetReceivableHistoryUseCase;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.application.port.in.GetReceivableStatusUseCase;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.application.port.in.RecordReceivablePaymentUseCase;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.application.port.out.ReceivableRepository;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.dto.ReceivableHistoryDto;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.dto.ReceivableStatusDto;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.model.Receivable;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.infrastructure.mapper.ReceivableMapper;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.infrastructure.model.CreateReceivableReq;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.infrastructure.model.ReceivablePaymentReq;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/receivable")
public class ReceivableController {
    private final ReceivableRepository receivableRepository;
    private final ReceivableMapper receivableMapper;
    private final FindAllReceivablesUseCase findAllReceivablesUseCase;
    private final RecordReceivablePaymentUseCase recordReceivablePaymentUseCase;
    private final GetReceivableStatusUseCase getReceivableStatusUseCase;
    private final GetReceivableHistoryUseCase getReceivableHistoryUseCase;

    public ReceivableController(ReceivableRepository receivableRepository, ReceivableMapper receivableMapper,
                                FindAllReceivablesUseCase findAllReceivablesUseCase,
                                RecordReceivablePaymentUseCase recordReceivablePaymentUseCase,
                                GetReceivableStatusUseCase getReceivableStatusUseCase,
                                GetReceivableHistoryUseCase getReceivableHistoryUseCase) {
        this.receivableRepository = receivableRepository;
        this.receivableMapper = receivableMapper;
        this.findAllReceivablesUseCase = findAllReceivablesUseCase;
        this.recordReceivablePaymentUseCase = recordReceivablePaymentUseCase;
        this.getReceivableStatusUseCase = getReceivableStatusUseCase;
        this.getReceivableHistoryUseCase = getReceivableHistoryUseCase;
    }

    @PostMapping("/{userEmail}")
    public ResponseEntity<Receivable> createReceivable(@RequestBody CreateReceivableReq createReceivableReq, @PathVariable String userEmail) {
        return ResponseEntity.ok(receivableRepository.save(receivableMapper.toModel(createReceivableReq), userEmail));
    }

    @PutMapping
    public ResponseEntity<Receivable> updateReceivable(@RequestBody Receivable receivable) {
        return ResponseEntity.ok(receivableRepository.update(receivable));
    }

    @DeleteMapping("/{receivableId}")
    public ResponseEntity<Void> deleteReceivable(@PathVariable Integer receivableId) {
        receivableRepository.delete(receivableId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/all/{userEmail}")
    public ResponseEntity<List<Receivable>> getAllReceivables(@PathVariable String userEmail) {
        return ResponseEntity.ok(findAllReceivablesUseCase.getByEmail(userEmail));
    }

    @PostMapping("/{receivableId}/payment")
    public ResponseEntity<ReceivableStatusDto> registerPayment(@PathVariable Integer receivableId,
                                                                @RequestBody ReceivablePaymentReq receivablePaymentReq) {
        return ResponseEntity.ok(recordReceivablePaymentUseCase.registerPayment(receivableId, receivablePaymentReq));
    }

    @GetMapping("/status/{receivableId}")
    public ResponseEntity<ReceivableStatusDto> getStatus(@PathVariable Integer receivableId) {
        return ResponseEntity.ok(getReceivableStatusUseCase.getStatus(receivableId));
    }

    @GetMapping("/history/{receivableId}")
    public ResponseEntity<ReceivableHistoryDto> getHistory(@PathVariable Integer receivableId) {
        return ResponseEntity.ok(getReceivableHistoryUseCase.getHistory(receivableId));
    }

    @GetMapping("/status/all/{userEmail}")
    public ResponseEntity<List<ReceivableStatusDto>> getAllStatuses(@PathVariable String userEmail) {
        return ResponseEntity.ok(getReceivableStatusUseCase.getAllStatusesByEmail(userEmail));
    }
}
