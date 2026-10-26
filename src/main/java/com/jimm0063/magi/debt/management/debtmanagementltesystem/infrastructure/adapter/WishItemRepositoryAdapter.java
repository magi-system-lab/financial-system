package com.jimm0063.magi.debt.management.debtmanagementltesystem.infrastructure.adapter;

import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.application.port.out.WishItemRepository;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.enums.WishStatus;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.exceptions.EntityNotFoundException;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.model.WishItem;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.infrastructure.entity.DebtSysUserEntity;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.infrastructure.entity.WishItemEntity;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.infrastructure.mapper.WishItemMapper;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.infrastructure.persistence.DebtSysUserJpaRepository;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.infrastructure.persistence.WishItemJpaRepository;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Component
public class WishItemRepositoryAdapter implements WishItemRepository {
    private final WishItemJpaRepository wishItemJpaRepository;
    private final WishItemMapper wishItemMapper;
    private final DebtSysUserJpaRepository debtSysUserJpaRepository;

    public WishItemRepositoryAdapter(WishItemJpaRepository wishItemJpaRepository, WishItemMapper wishItemMapper,
                                     DebtSysUserJpaRepository debtSysUserJpaRepository) {
        this.wishItemJpaRepository = wishItemJpaRepository;
        this.wishItemMapper = wishItemMapper;
        this.debtSysUserJpaRepository = debtSysUserJpaRepository;
    }

    @Override
    public List<WishItem> findAllActiveByUser(String email) {
        return wishItemJpaRepository.findAllByDebtSysUser_EmailAndActiveTrueOrderByCreatedAtDesc(email)
                .stream()
                .map(wishItemMapper::toModel)
                .toList();
    }

    @Override
    public Optional<WishItem> findById(Integer id) {
        return wishItemJpaRepository.findById(id)
                .filter(e -> Boolean.TRUE.equals(e.getActive()))
                .map(wishItemMapper::toModel);
    }

    @Override
    public WishItem save(WishItem item, String userEmail) {
        DebtSysUserEntity user = debtSysUserJpaRepository.findByEmailAndActiveTrue(userEmail)
                .orElseThrow(() -> new EntityNotFoundException("User " + userEmail + " not found"));
        WishItemEntity entity = wishItemMapper.toEntity(item);
        entity.setDebtSysUser(user);
        entity.setActive(true);
        return wishItemMapper.toModel(wishItemJpaRepository.save(entity));
    }

    @Override
    public WishItem updateStatus(Integer id, WishStatus status) {
        WishItemEntity entity = find(id);
        entity.setStatus(status);
        entity.setDecidedAt(status == WishStatus.WAITING ? null : Instant.now());
        return wishItemMapper.toModel(wishItemJpaRepository.save(entity));
    }

    @Override
    public void delete(Integer id) {
        WishItemEntity entity = find(id);
        entity.setActive(false);
        wishItemJpaRepository.save(entity);
    }

    private WishItemEntity find(Integer id) {
        return wishItemJpaRepository.findById(id)
                .filter(e -> Boolean.TRUE.equals(e.getActive()))
                .orElseThrow(() -> new EntityNotFoundException("Wish item " + id + " not found"));
    }
}
