package com.jimm0063.magi.debt.management.debtmanagementltesystem.infrastructure.web.view;

import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.application.port.in.FindAllFixedExpenseCatalogsUseCase;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.application.port.in.FindAllFixedExpenseUseCase;
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
}
