package com.jimm0063.magi.debt.management.debtmanagementltesystem.infrastructure.web.view;

import com.jimm0063.magi.debt.management.debtmanagementltesystem.infrastructure.configuration.SecurityConfig;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.infrastructure.model.ActivityEntry;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MoreViewController.class)
@Import(SecurityConfig.class)
class MoreViewControllerTest {

    @Autowired MockMvc mockMvc;

    @MockBean ActivityLogHelper activityLogHelper;

    @Test
    void more_without_user_redirects_to_login() throws Exception {
        mockMvc.perform(get("/ui/more"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/ui"));
    }

    @Test
    void more_renders_shell_with_more_tab_active() throws Exception {
        mockMvc.perform(get("/ui/more").sessionAttr("userEmail", "user@test.com"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("class=\"fm-tabbar glass\"")))
                .andExpect(content().string(containsString("href=\"/ui/more\" aria-current=\"page\"")))
                .andExpect(content().string(containsString("user@test.com")))
                .andExpect(content().string(containsString("data-from-cache=\"false\"")))
                .andExpect(content().string(containsString("Switch user")));
    }

    @Test
    void remembered_cookie_restores_user_without_session() throws Exception {
        mockMvc.perform(get("/ui/more").cookie(new Cookie(RememberedUser.COOKIE, "me%40test.com")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("me@test.com")));
    }

    @Test
    void lang_param_switches_to_spanish_and_is_remembered() throws Exception {
        mockMvc.perform(get("/ui/more").param("lang", "es_MX").sessionAttr("userEmail", "user@test.com"))
                .andExpect(status().isOk())
                .andExpect(cookie().value("fm_lang", "es-MX"))
                .andExpect(content().string(containsString("Cambiar de usuario")))
                .andExpect(content().string(containsString("Resumen")))
                .andExpect(content().string(not(containsString("Switch user"))));
    }

    @Test
    void activity_lists_session_entries() throws Exception {
        mockMvc.perform(get("/ui/activity")
                        .sessionAttr("userEmail", "user@test.com")
                        .sessionAttr("activityLog", List.of(new ActivityEntry("2026-10-25 10:00:00", "Bulk Sync", "{\"ok\":true}"))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Bulk Sync")))
                .andExpect(content().string(containsString("{&quot;ok&quot;:true}")));
    }

    @Test
    void activity_shows_empty_state() throws Exception {
        mockMvc.perform(get("/ui/activity").sessionAttr("userEmail", "user@test.com"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("No activity yet")));
    }
}
