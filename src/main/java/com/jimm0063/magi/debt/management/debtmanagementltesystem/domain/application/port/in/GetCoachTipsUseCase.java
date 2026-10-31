package com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.application.port.in;

import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.dto.CoachTipDto;

import java.util.List;

public interface GetCoachTipsUseCase {
    /** Advice for the user right now, most urgent first. */
    List<CoachTipDto> getTips(String email);
}
