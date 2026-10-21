package com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.application.port.in;

import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.dto.PaymentProjectionDto;

public interface GetPaymentProjectionUseCase {
    PaymentProjectionDto getProjection(String email, int months);
}
