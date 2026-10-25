package com.jimm0063.magi.debt.management.debtmanagementltesystem.infrastructure.web.view;

import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.application.port.in.GetFinancialStatusUseCase;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.application.port.out.UserRepository;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.exceptions.EntityNotFoundException;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.infrastructure.configuration.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(DashboardViewController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = "app.default-user-email=default@test.com")
class DashboardViewControllerTest {

    @Autowired MockMvc mockMvc;

    @MockBean GetFinancialStatusUseCase getFinancialStatusUseCase;
    @MockBean UserRepository userRepository;
    @MockBean ActivityLogHelper activityLogHelper;

    @Test
    void login_remembers_email_in_long_lived_cookie() throws Exception {
        mockMvc.perform(post("/ui/session").param("email", " me@test.com "))
                .andExpect(redirectedUrl("/ui/dashboard"))
                .andExpect(cookie().value(RememberedUser.COOKIE, "me%40test.com"))
                .andExpect(cookie().maxAge(RememberedUser.COOKIE, 365 * 24 * 3600))
                .andExpect(cookie().httpOnly(RememberedUser.COOKIE, true));
    }

    @Test
    void switch_user_clears_cookie_and_shows_login_without_auto_login() throws Exception {
        mockMvc.perform(post("/ui/session/end").sessionAttr("userEmail", "me@test.com"))
                .andExpect(redirectedUrl("/ui?switch"))
                .andExpect(cookie().maxAge(RememberedUser.COOKIE, 0));

        mockMvc.perform(get("/ui").param("switch", ""))
                .andExpect(status().isOk())
                .andExpect(view().name("home"))
                .andExpect(content().string(containsString("name=\"email\"")))
                .andExpect(content().string(containsString("rel=\"manifest\"")))
                .andExpect(content().string(containsString("apple-mobile-web-app-capable")));
    }

    @Test
    void unknown_email_forgets_cookie_to_avoid_redirect_loop() throws Exception {
        when(getFinancialStatusUseCase.getUserStatus("ghost@test.com")).thenThrow(new EntityNotFoundException("no user"));

        mockMvc.perform(get("/ui/dashboard").sessionAttr("userEmail", "ghost@test.com"))
                .andExpect(redirectedUrl("/ui?error=email_not_found"))
                .andExpect(cookie().maxAge(RememberedUser.COOKIE, 0));
    }

    @Test
    void default_email_still_auto_logs_in() throws Exception {
        mockMvc.perform(get("/ui"))
                .andExpect(redirectedUrl("/ui/dashboard"));
    }
}
