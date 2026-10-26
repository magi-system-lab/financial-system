package com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.application.port.out;

import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.enums.WishStatus;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.model.WishItem;

import java.util.List;
import java.util.Optional;

public interface WishItemRepository {
    List<WishItem> findAllActiveByUser(String email);
    Optional<WishItem> findById(Integer id);
    WishItem save(WishItem item, String userEmail);
    WishItem updateStatus(Integer id, WishStatus status);
    void delete(Integer id);
}
