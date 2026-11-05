package com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.application.port.in;

import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.dto.PayoffRecommendationDto;

public interface GetPayoffRecommendationUseCase {
    /** Which debts to pay off with the savings above the safety net to lower next month's payment the most. */
    PayoffRecommendationDto getRecommendation(String email);
}
