package com.jimm0063.magi.debt.management.debtmanagementltesystem.infrastructure.web.view;

import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.application.port.in.FindAllFixedExpenseCatalogsUseCase;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.application.port.in.FindAllFixedExpenseUseCase;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.application.port.in.MoveBetweenSectionsUseCase;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.enums.FixedExpenseKind;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.application.port.out.FixedExpenseCatalogRepository;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.application.port.out.FixedExpenseRepository;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.model.FixedExpense;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.model.FixedExpenseCatalog;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.infrastructure.configuration.SecurityConfig;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.infrastructure.mapper.FixedExpenseCatalogMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(FixedExpenseViewController.class)
@Import(SecurityConfig.class)
class FixedExpenseViewControllerTest {

    @Autowired MockMvc mockMvc;

    @MockBean FixedExpenseRepository fixedExpenseRepository;
    @MockBean FixedExpenseCatalogRepository fixedExpenseCatalogRepository;
    @MockBean FixedExpenseCatalogMapper fixedExpenseCatalogMapper;
    @MockBean FindAllFixedExpenseUseCase findAllFixedExpenseUseCase;
    @MockBean FindAllFixedExpenseCatalogsUseCase findAllFixedExpenseCatalogsUseCase;
    @MockBean ActivityLogHelper activityLogHelper;
    @MockBean MoveBetweenSectionsUseCase moveBetweenSectionsUseCase;

    private static FixedExpenseCatalog catalog(int id, String name) {
        FixedExpenseCatalog c = new FixedExpenseCatalog();
        c.setId(id);
        c.setName(name);
        return c;
    }

    private static FixedExpense expense(int id, String name, String cost, FixedExpenseCatalog catalog) {
        FixedExpense e = new FixedExpense();
        e.setId(id);
        e.setName(name);
        e.setMonthlyCost(new BigDecimal(cost));
        e.setPaymentDay(5);
        e.setFixedExpenseCatalog(catalog);
        return e;
    }

    private static final FixedExpenseCatalog HOME = catalog(1, "Home");
    private static final FixedExpenseCatalog FUN = catalog(2, "Subscriptions");
    private static final List<FixedExpense> EXPENSES = List.of(
            expense(1, "Netflix", "300", FUN),
            expense(2, "Rent", "6000", HOME),
            expense(3, "Internet", "700", HOME),
            expense(4, "Spotify", "200", FUN),
            expense(5, "Gym", "800", null));

    @Test
    void by_category_groups_and_sorts_by_total() {
        var groups = FixedExpenseViewController.byCategory(EXPENSES);

        assertThat(groups).extracting(FixedExpenseViewController.CategoryGroup::name)
                .containsExactly("Home", "", "Subscriptions");
        assertThat(groups.get(0).total()).isEqualByComparingTo("6700");
        assertThat(groups.get(0).percent()).isEqualTo(84);
        assertThat(groups.get(0).expenses()).extracting(FixedExpense::getName).containsExactly("Rent", "Internet");
    }

    @Test
    void list_renders_total_breakdown_and_groups() throws Exception {
        when(findAllFixedExpenseUseCase.getByEmail("user@test.com")).thenReturn(EXPENSES);
        when(findAllFixedExpenseCatalogsUseCase.getCatalogList()).thenReturn(List.of(HOME, FUN));

        mockMvc.perform(get("/ui/fixed-expenses").sessionAttr("userEmail", "user@test.com"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("$8,000")))
                .andExpect(content().string(containsString("Home")))
                .andExpect(content().string(containsString("No category")))
                .andExpect(content().string(containsString("Day 5")))
                .andExpect(content().string(containsString("data-delete-action=\"/ui/fixed-expenses/2\"")));
    }

    @Test
    void list_without_session_redirects_to_login() throws Exception {
        mockMvc.perform(get("/ui/fixed-expenses"))
                .andExpect(redirectedUrl("/ui"));
    }

    @Test
    void bulk_delete_removes_the_selected_ids() throws Exception {
        mockMvc.perform(post("/ui/fixed-expenses/bulk-delete").sessionAttr("userEmail", "user@test.com")
                        .param("_method", "DELETE").param("ids", "1", "3"))
                .andExpect(redirectedUrl("/ui/fixed-expenses"));
        verify(fixedExpenseRepository).deleteMultiple(List.of(1, 3));
    }

    private static FixedExpense credit(int id, String name, String cost) {
        FixedExpense e = expense(id, name, cost, HOME);
        e.setKind(FixedExpenseKind.CREDIT);
        return e;
    }

    @Test
    void fixed_expenses_page_leaves_credits_out() throws Exception {
        when(findAllFixedExpenseUseCase.getByEmail("user@test.com"))
                .thenReturn(List.of(expense(1, "Rent", "6000", HOME), credit(9, "Car loan", "4200")));
        when(findAllFixedExpenseCatalogsUseCase.getCatalogList()).thenReturn(List.of(HOME));

        mockMvc.perform(get("/ui/fixed-expenses").sessionAttr("userEmail", "user@test.com"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Rent")))
                .andExpect(content().string(not(containsString("Car loan"))))
                .andExpect(content().string(containsString("Move to credits")))
                .andExpect(content().string(containsString("data-move-action=\"/ui/fixed-expenses/1/move\"")));
    }

    @Test
    void credits_page_shows_only_credits() throws Exception {
        when(findAllFixedExpenseUseCase.getByEmail("user@test.com"))
                .thenReturn(List.of(expense(1, "Rent", "6000", HOME), credit(9, "Car loan", "4200")));
        when(findAllFixedExpenseCatalogsUseCase.getCatalogList()).thenReturn(List.of(HOME));

        mockMvc.perform(get("/ui/credits").sessionAttr("userEmail", "user@test.com"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Car loan")))
                .andExpect(content().string(containsString("$4,200")))
                .andExpect(content().string(not(containsString("Rent"))))
                .andExpect(content().string(containsString("Move to fixed expenses")))
                .andExpect(content().string(containsString("data-delete-action=\"/ui/credits/9\"")));
    }

    @Test
    void adding_on_the_credits_page_saves_a_credit() throws Exception {
        FixedExpense saved = credit(10, "Mortgage", "9000");
        when(fixedExpenseRepository.save(any(), eq(1))).thenReturn(saved);

        mockMvc.perform(post("/ui/credits").sessionAttr("userEmail", "user@test.com")
                        .param("name", "Mortgage").param("monthlyCost", "9000").param("catalogId", "1"))
                .andExpect(redirectedUrl("/ui/credits"));
        verify(fixedExpenseRepository).save(argThat(r -> r.getKind() == FixedExpenseKind.CREDIT
                && "user@test.com".equals(r.getUserEmail())), eq(1));
    }

    @Test
    void move_sends_a_fixed_expense_to_credits_and_back() throws Exception {
        mockMvc.perform(post("/ui/fixed-expenses/4/move").sessionAttr("userEmail", "user@test.com"))
                .andExpect(redirectedUrl("/ui/fixed-expenses"));
        verify(moveBetweenSectionsUseCase).moveFixedExpenses(List.of(4), FixedExpenseKind.CREDIT);

        mockMvc.perform(post("/ui/credits/bulk-move").sessionAttr("userEmail", "user@test.com").param("ids", "7", "8"))
                .andExpect(redirectedUrl("/ui/credits"));
        verify(moveBetweenSectionsUseCase).moveFixedExpenses(List.of(7, 8), FixedExpenseKind.EXPENSE);
    }

    @Test
    void unknown_section_is_not_mapped() throws Exception {
        mockMvc.perform(get("/ui/loans").sessionAttr("userEmail", "user@test.com"))
                .andExpect(status().isNotFound());
    }

    @Test
    void credits_show_their_last_payment_and_finished_ones_apart() throws Exception {
        java.time.YearMonth now = java.time.YearMonth.now();
        FixedExpense car = credit(9, "Car loan", "4200");
        car.setEndDate(now.plusMonths(4).atDay(1));
        FixedExpense old = credit(10, "Old phone plan", "999");
        old.setEndDate(now.minusMonths(1).atDay(1));
        when(findAllFixedExpenseUseCase.getByEmail("user@test.com")).thenReturn(List.of(car, old));
        when(findAllFixedExpenseCatalogsUseCase.getCatalogList()).thenReturn(List.of(HOME));

        mockMvc.perform(get("/ui/credits").sessionAttr("userEmail", "user@test.com"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("5 left")))
                .andExpect(content().string(containsString("Finished")))
                .andExpect(content().string(containsString("Old phone plan")))
                .andExpect(content().string(containsString("data-f-end-month=\"" + now.plusMonths(4) + "\"")))
                .andExpect(content().string(containsString("$4,200.00")))
                .andExpect(content().string(containsString("name=\"endMonth\"")));
    }
}
