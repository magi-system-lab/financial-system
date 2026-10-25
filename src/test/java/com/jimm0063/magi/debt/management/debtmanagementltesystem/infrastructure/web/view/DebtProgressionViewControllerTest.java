package com.jimm0063.magi.debt.management.debtmanagementltesystem.infrastructure.web.view;

import com.jimm0063.magi.debt.management.debtmanagementltesystem.infrastructure.configuration.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;

@WebMvcTest(DebtProgressionViewController.class)
@Import(SecurityConfig.class)
class DebtProgressionViewControllerTest {

    @Autowired MockMvc mockMvc;

    @MockBean ActivityLogHelper activityLogHelper;

    @Test
    void month_by_month_page_now_opens_insights_with_24_months() throws Exception {
        mockMvc.perform(get("/ui/progression").param("targetMonth", "2027-01").sessionAttr("userEmail", "user@test.com"))
                .andExpect(redirectedUrl("/ui/insights?months=24"));
    }
}
