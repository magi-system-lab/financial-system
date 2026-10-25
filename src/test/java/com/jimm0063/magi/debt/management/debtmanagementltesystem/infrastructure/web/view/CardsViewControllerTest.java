package com.jimm0063.magi.debt.management.debtmanagementltesystem.infrastructure.web.view;

import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.application.port.in.FindAllFinancialProviderUseCase;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.application.port.in.GetFinancialStatusUseCase;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.dto.UserStatusDashboard;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.infrastructure.configuration.SecurityConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CardsViewController.class)
@Import(SecurityConfig.class)
class CardsViewControllerTest {

    private static final String EMAIL = "user@test.com";

    @Autowired MockMvc mockMvc;

    @MockBean GetFinancialStatusUseCase getFinancialStatusUseCase;
    @MockBean FindAllFinancialProviderUseCase findAllFinancialProviderUseCase;
    @MockBean ActivityLogHelper activityLogHelper;

    @BeforeEach
    void setUp() {
        UserStatusDashboard dashboard = new UserStatusDashboard();
        dashboard.setUserDebtAccounts(List.of(CardsFixtures.spare(), CardsFixtures.azul()));
        dashboard.setAccountSummaries(List.of(CardsFixtures.azulSummary()));
        when(getFinancialStatusUseCase.getUserStatus(EMAIL)).thenReturn(dashboard);
        when(findAllFinancialProviderUseCase.getActiveByEmail(EMAIL)).thenReturn(List.of(CardsFixtures.bbva()));
    }

    @Test
    void without_user_redirects_to_login() throws Exception {
        mockMvc.perform(get("/ui/cards")).andExpect(redirectedUrl("/ui"));
    }

    @Test
    void wallet_lists_cards_by_monthly_amount_with_their_colours() throws Exception {
        String html = mockMvc.perform(get("/ui/cards").sessionAttr("userEmail", EMAIL))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("2 cards · $4,774 this month")))
                .andExpect(content().string(containsString("--card-color:#0B3D91")))
                .andExpect(content().string(containsString("--card-color:#0F766E")))
                .andExpect(content().string(containsString("4 debts · $31,250 left")))
                .andExpect(content().string(containsString("href=\"/ui/debt-accounts/acc-azul\"")))
                .andExpect(content().string(containsString("href=\"/ui/cards\" aria-current=\"page\"")))
                .andReturn().getResponse().getContentAsString();
        // highest monthly payment first
        org.assertj.core.api.Assertions.assertThat(html.indexOf("BBVA Azul")).isLessThan(html.indexOf("Spare card"));
    }

    @Test
    void provider_filter_shows_only_its_cards() throws Exception {
        mockMvc.perform(get("/ui/cards").param("provider", "prov-bbva").sessionAttr("userEmail", EMAIL))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Showing BBVA")))
                .andExpect(content().string(not(containsString("Spare card"))));
    }

    @Test
    void add_card_sheet_posts_to_the_first_provider_and_offers_colours() throws Exception {
        mockMvc.perform(get("/ui/cards").sessionAttr("userEmail", EMAIL))
                .andExpect(content().string(containsString("action=\"/ui/debt-accounts/prov-bbva\"")))
                .andExpect(content().string(containsString("name=\"color\" value=\"auto\"")));
    }
}
