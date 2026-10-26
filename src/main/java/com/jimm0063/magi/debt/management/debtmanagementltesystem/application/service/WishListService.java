package com.jimm0063.magi.debt.management.debtmanagementltesystem.application.service;

import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.application.port.in.ManageWishListUseCase;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.application.port.out.WishItemRepository;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.enums.WishStatus;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.model.WishItem;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class WishListService implements ManageWishListUseCase {
    private final WishItemRepository wishItemRepository;

    public WishListService(WishItemRepository wishItemRepository) {
        this.wishItemRepository = wishItemRepository;
    }

    @Override
    public List<WishItem> getByEmail(String email) {
        return wishItemRepository.findAllActiveByUser(email);
    }

    @Override
    public WishItem add(String email, String description, BigDecimal price, Integer installments) {
        if (description == null || description.isBlank()) throw new IllegalArgumentException("Describe what you want to buy");
        if (price == null || price.signum() <= 0) throw new IllegalArgumentException("Enter the price");
        if (installments != null && installments < 1) throw new IllegalArgumentException("Installments must be at least 1");
        WishItem item = new WishItem();
        item.setDescription(description.trim());
        item.setPrice(price);
        item.setInstallments(installments);
        item.setStatus(WishStatus.WAITING);
        return wishItemRepository.save(item, email);
    }

    @Override
    public WishItem markPlanned(Integer id) {
        return wishItemRepository.updateStatus(id, WishStatus.PLANNED);
    }

    @Override
    public WishItem drop(Integer id) {
        return wishItemRepository.updateStatus(id, WishStatus.DROPPED);
    }

    @Override
    public void delete(Integer id) {
        wishItemRepository.delete(id);
    }
}
