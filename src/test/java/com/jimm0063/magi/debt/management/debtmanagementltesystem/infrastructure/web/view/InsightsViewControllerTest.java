package com.jimm0063.magi.debt.management.debtmanagementltesystem.infrastructure.web.view;

import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.application.port.in.GetDebtProgressionUseCase;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.application.port.in.GetFinancialStatusUseCase;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.application.port.out.UserRepository;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.dto.AccountSummaryDto;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.dto.UserStatusDashboard;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.exceptions.EntityNotFoundException;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.infrastructure.configuration.SecurityConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(InsightsViewController.class)
@Import({SecurityConfig.class, UiFormat.class})
class InsightsViewControllerTest {

    private static final String EMAIL = "user@test.com";

    @Autowired MockMvc mockMvc;

    @MockBean GetFinancialStatusUseCase getFinancialStatusUseCase;
    @MockBean GetDebtProgressionUseCase getDebtProgressionUseCase;
    @MockBean UserRepository userRepository;
    @MockBean ActivityLogHelper activityLogHelper;

    @BeforeEach
    void setUp() {
        UserStatusDashboard dashboard = new UserStatusDashboard();
        dashboard.setSalary(5000.0);
        dashboard.setSavings(20000.0);
        dashboard.setMonthlyDebtPaymentAmount(1000.0);
        dashboard.setDebtLoanAmount(0.0);
        dashboard.setDebtForLifePlanAmount(0.0);
        dashboard.setMonthlyFixedExpensesAmount(1500.0);
        AccountSummaryDto visa = new AccountSummaryDto();
        visa.setCode("VISA");
        visa.setName("Visa Oro");
        visa.setActiveDebtCount(2);
        visa.setMonthlyTotal(1000.0);
        visa.setEstimatedRemainingBalance(3000.0);
        visa.setMaxMonthsRemaining(4);
        visa.setPayoffFeasible(true);
        visa.setSavingsCoveragePercent(666.7);
        dashboard.setAccountSummaries(List.of(visa));
        when(getFinancialStatusUseCase.getUserStatus(EMAIL)).thenReturn(dashboard);
        when(getDebtProgressionUseCase.getProgression(eq(EMAIL), any())).thenReturn(InsightsFixtures.progression());
    }

    @Test
    void without_user_redirects_to_login() throws Exception {
        mockMvc.perform(get("/ui/insights")).andExpect(redirectedUrl("/ui"));
    }

    @Test
    void renders_month_panels_cards_finishing_and_advisor() throws Exception {
        mockMvc.perform(get("/ui/insights").sessionAttr("userEmail", EMAIL))
                .andExpect(status().isOk())
                .andExpect(model().attribute("range", 12))
                .andExpect(content().string(containsString("November 2026")))
                .andExpect(content().string(containsString(">$1,000<")))
                .andExpect(content().string(containsString("↓ $200 in Dec")))
                .andExpect(content().string(containsString("Leaves $2,500 of your salary after fixed expenses")))
                .andExpect(content().string(containsString("Phone")))
                .andExpect(content().string(containsString("Visa Oro · last payment Dec")))
                .andExpect(content().string(containsString("11/12 paid")))
                .andExpect(content().string(containsString("href=\"/ui/debt-accounts/VISA\"")))
                .andExpect(content().string(containsString("Mar 2027")))          // debt-free
                .andExpect(content().string(containsString("Can pay off")))
                .andExpect(content().string(containsString("Cards &amp; people")))
                .andExpect(content().string(containsString("data-select=\"month\"")))
                .andExpect(content().string(containsString("href=\"/ui/insights\" aria-current=\"page\"")));
    }

    @Test
    void range_param_accepts_6_and_24_and_falls_back_to_12() throws Exception {
        mockMvc.perform(get("/ui/insights").param("months", "24").sessionAttr("userEmail", EMAIL))
                .andExpect(model().attribute("range", 24));
        mockMvc.perform(get("/ui/insights").param("months", "7").sessionAttr("userEmail", EMAIL))
                .andExpect(model().attribute("range", 12));
    }

    @Test
    void spanish_month_names_and_labels() throws Exception {
        mockMvc.perform(get("/ui/insights").param("lang", "es_MX").sessionAttr("userEmail", EMAIL))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Noviembre 2026")))
                .andExpect(content().string(containsString("Por terminar")))
                .andExpect(content().string(containsString("Visa Oro · último pago Dic")))
                .andExpect(content().string(not(containsString("last payment"))));
    }

    @Test
    void unknown_email_forgets_cookie_to_avoid_redirect_loop() throws Exception {
        when(getFinancialStatusUseCase.getUserStatus("ghost@test.com")).thenThrow(new EntityNotFoundException("no user"));

        mockMvc.perform(get("/ui/insights").sessionAttr("userEmail", "ghost@test.com"))
                .andExpect(redirectedUrl("/ui?error=email_not_found"))
                .andExpect(cookie().maxAge(RememberedUser.COOKIE, 0));
    }

    @Test
    void saving_salary_and_savings_returns_to_insights() throws Exception {
        mockMvc.perform(post("/ui/financials").param("_method", "PUT")
                        .param("salary", "6000").param("savings", "100")
                        .sessionAttr("userEmail", EMAIL))
                .andExpect(redirectedUrl("/ui/insights"));
        verify(userRepository).updateFinancials(EMAIL, 6000.0, 100.0);
    }

    @Test
    void empty_state_when_nothing_is_owed() throws Exception {
        when(getDebtProgressionUseCase.getProgression(eq(EMAIL), any()))
                .thenReturn(InsightsFixtures.progression().subList(4, 6));
        mockMvc.perform(get("/ui/insights").sessionAttr("userEmail", EMAIL))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("No payments ahead")));
    }
}
