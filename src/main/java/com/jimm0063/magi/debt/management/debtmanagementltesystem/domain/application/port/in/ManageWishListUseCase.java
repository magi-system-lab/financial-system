package com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.application.port.in;

import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.model.WishItem;

import java.math.BigDecimal;
import java.util.List;

public interface ManageWishListUseCase {
    List<WishItem> getByEmail(String email);
    WishItem add(String email, String description, BigDecimal price, Integer installments);
    /** Still wanted after the cool-down: it moves to the upcoming pile. */
    WishItem markPlanned(Integer id);
    WishItem drop(Integer id);
    void delete(Integer id);
}
