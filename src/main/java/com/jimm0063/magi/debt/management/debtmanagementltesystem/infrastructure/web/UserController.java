package com.jimm0063.magi.debt.management.debtmanagementltesystem.infrastructure.web;

import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.application.port.out.UserRepository;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.model.DebtSysUser;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.infrastructure.model.UpdateUserReq;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/user")
public class UserController {
    private final UserRepository userRepository;

    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @PutMapping("/financials")
    public ResponseEntity<DebtSysUser> updateFinancials(@RequestBody UpdateUserReq updateUserReq) {
        return ResponseEntity.ok(this.userRepository.updateFinancials(
                updateUserReq.getEmail(), updateUserReq.getSalary(), updateUserReq.getSavings()));
    }

    /** Max share of the salary for debt payments (e.g. 30); 0 removes it. */
    @PutMapping("/debt-limit/{email}")
    public ResponseEntity<DebtSysUser> updateDebtLimit(@PathVariable String email, @RequestParam Integer percent) {
        return ResponseEntity.ok(this.userRepository.updateDebtLimit(email, percent));
    }
}
