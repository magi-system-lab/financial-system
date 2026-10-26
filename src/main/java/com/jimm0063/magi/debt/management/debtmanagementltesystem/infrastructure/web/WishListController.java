package com.jimm0063.magi.debt.management.debtmanagementltesystem.infrastructure.web;

import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.application.port.in.ManageWishListUseCase;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.model.WishItem;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

/** Cool-down list: park a purchase for a few days before deciding. */
@RestController
@RequestMapping("/wish")
public class WishListController {
    private final ManageWishListUseCase manageWishListUseCase;

    public WishListController(ManageWishListUseCase manageWishListUseCase) {
        this.manageWishListUseCase = manageWishListUseCase;
    }

    @GetMapping("/all/{email}")
    public ResponseEntity<List<WishItem>> getAll(@PathVariable String email) {
        return ResponseEntity.ok(manageWishListUseCase.getByEmail(email));
    }

    @PostMapping("/{email}")
    public ResponseEntity<WishItem> add(@PathVariable String email, @RequestParam String description,
                                        @RequestParam BigDecimal price,
                                        @RequestParam(required = false) Integer installments) {
        return ResponseEntity.ok(manageWishListUseCase.add(email, description, price, installments));
    }

    @PatchMapping("/{id}/planned")
    public ResponseEntity<WishItem> planned(@PathVariable Integer id) {
        return ResponseEntity.ok(manageWishListUseCase.markPlanned(id));
    }

    @PatchMapping("/{id}/drop")
    public ResponseEntity<WishItem> drop(@PathVariable Integer id) {
        return ResponseEntity.ok(manageWishListUseCase.drop(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        manageWishListUseCase.delete(id);
        return ResponseEntity.noContent().build();
    }
}
