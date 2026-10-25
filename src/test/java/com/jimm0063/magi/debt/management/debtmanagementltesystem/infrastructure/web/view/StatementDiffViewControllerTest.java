package com.jimm0063.magi.debt.management.debtmanagementltesystem.infrastructure.web.view;

import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.application.port.in.FindAllUpcomingDebtsUseCase;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.application.port.in.MarkDebtAsIgnorableUseCase;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.application.port.in.PreviewStatementDiffUseCase;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.application.port.in.SyncStatementDiffUseCase;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.application.port.out.DebtAccountRepository;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.enums.AccountStatementType;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.enums.DebtDiffStatus;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.model.Debt;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.model.DebtDiff;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.model.StatementDiffResult;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.infrastructure.configuration.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(StatementDiffViewController.class)
@Import(SecurityConfig.class)
class StatementDiffViewControllerTest {

    @Autowired MockMvc mockMvc;

    @MockBean PreviewStatementDiffUseCase previewUseCase;
    @MockBean SyncStatementDiffUseCase syncUseCase;
    @MockBean MarkDebtAsIgnorableUseCase markIgnorableUseCase;
    @MockBean DebtAccountRepository debtAccountRepository;
    @MockBean ActivityLogHelper activityLogHelper;
    @MockBean FindAllUpcomingDebtsUseCase findAllUpcomingDebtsUseCase;

    private static Debt debt(String desc, int current, int max) {
        Debt d = new Debt();
        d.setDescription(desc);
        d.setHashSum("h-" + desc);
        d.setMonthlyPayment(BigDecimal.TEN);
        d.setCurrentInstallment(current);
        d.setMaxFinancingTerm(max);
        return d;
    }

    @Test
    void preview_renders_diff_rows_from_shared_fragment() throws Exception {
        StatementDiffResult diff = new StatementDiffResult("VISA", AccountStatementType.UNIVERSAL,
                List.of(new DebtDiff(debt("New TV", 1, 12), null, DebtDiffStatus.NEW)),
                List.of(new DebtDiff(debt("Laptop", 4, 12), debt("Laptop", 3, 12), DebtDiffStatus.UPDATED)),
                List.of(new DebtDiff(debt("Phone", 6, 6), debt("Phone", 5, 6), DebtDiffStatus.COMPLETED)),
                List.of(), List.of());

        mockMvc.perform(get("/ui/v2/statements/VISA/preview")
                        .sessionAttr("userEmail", "user@test.com")
                        .sessionAttr("v2StatementDiff", diff))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("New TV")))
                .andExpect(content().string(containsString("Laptop")))
                .andExpect(content().string(containsString("Phone")))
                .andExpect(content().string(containsString("3 → 4 of 12")))
                .andExpect(content().string(containsString("data-open-sheet=\"ignore-sheet\"")))
                .andExpect(content().string(containsString("Mark as ignorable")))
                .andExpect(content().string(containsString("action=\"/ui/v2/statements/VISA/mark-ignorable\"")));
    }

    @Test
    void preview_reminds_about_card_upcoming_debts() throws Exception {
        StatementDiffResult diff = new StatementDiffResult("VISA", AccountStatementType.UNIVERSAL,
                List.of(), List.of(), List.of(), List.of(), List.of());
        com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.model.UpcomingDebt tv =
                new com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.model.UpcomingDebt();
        tv.setId(9);
        tv.setDescription("TV Liverpool");
        tv.setMonthlyPayment(new BigDecimal("1000.00"));
        tv.setInstallments(12);
        tv.setFirstPaymentDate(java.time.LocalDate.of(2027, 1, 1));
        org.mockito.Mockito.when(findAllUpcomingDebtsUseCase.getPendingByDebtAccount("VISA")).thenReturn(List.of(tv));

        mockMvc.perform(get("/ui/v2/statements/VISA/preview")
                        .sessionAttr("userEmail", "user@test.com")
                        .sessionAttr("v2StatementDiff", diff))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("on your upcoming pile")))
                .andExpect(content().string(containsString("TV Liverpool")))
                .andExpect(content().string(containsString("/ui/upcoming-debts/9/start")))
                .andExpect(content().string(containsString("value=\"/ui/v2/statements/VISA/preview\"")));
    }
}
