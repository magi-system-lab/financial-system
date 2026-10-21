package com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.application.port.in;

import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.dto.ReceivableHistoryDto;

public interface GetReceivableHistoryUseCase {
    ReceivableHistoryDto getHistory(Integer receivableId);
}
