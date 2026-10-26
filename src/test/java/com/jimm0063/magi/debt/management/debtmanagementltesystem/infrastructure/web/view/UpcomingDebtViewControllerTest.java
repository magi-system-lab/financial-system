package com.jimm0063.magi.debt.management.debtmanagementltesystem.infrastructure.web.view;

import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.application.port.in.FindAllDebtAccountUseCase;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.application.port.in.FindAllFinancialProviderUseCase;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.application.port.in.FindAllUpcomingDebtsUseCase;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.application.port.in.GetPaymentProjectionUseCase;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.application.port.in.ManageUpcomingDebtUseCase;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.application.port.in.ManageWishListUseCase;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.application.port.out.UserRepository;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.dto.MonthProjectionDto;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.dto.PaymentProjectionDto;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.enums.DebtTypeEnum;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.enums.UpcomingDebtStatus;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.model.DebtAccount;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.model.UpcomingDebt;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.model.WishItem;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.enums.WishStatus;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.infrastructure.configuration.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UpcomingDebtViewController.class)
@Import(SecurityConfig.class)
class UpcomingDebtViewControllerTest {

    @Autowired MockMvc mockMvc;

    @MockBean FindAllUpcomingDebtsUseCase findAllUpcomingDebtsUseCase;
    @MockBean ManageUpcomingDebtUseCase manageUpcomingDebtUseCase;
    @MockBean GetPaymentProjectionUseCase getPaymentProjectionUseCase;
    @MockBean FindAllFinancialProviderUseCase findAllFinancialProviderUseCase;
    @MockBean FindAllDebtAccountUseCase findAllDebtAccountUseCase;
    @MockBean UserRepository userRepository;
    @MockBean ManageWishListUseCase manageWishListUseCase;
    @MockBean ActivityLogHelper activityLogHelper;

    private static UpcomingDebt upcoming(int id, String desc, YearMonth first, UpcomingDebtStatus status) {
        DebtAccount visa = new DebtAccount();
        visa.setCode("VISA");
        visa.setName("Visa Oro");
        UpcomingDebt u = new UpcomingDebt();
        u.setId(id);
        u.setDescription(desc);
        u.setDebtAccount(visa);
        u.setTotalAmount(new BigDecimal("12000.00"));
        u.setMonthlyPayment(new BigDecimal("1000.00"));
        u.setInstallments(12);
        u.setFirstPaymentDate(first.atDay(1));
        u.setDebtType(DebtTypeEnum.CARD);
        u.setStatus(status);
        u.setActive(true);
        return u;
    }

    @Test
    void list_renders_pile_projection_and_history() throws Exception {
        YearMonth now = YearMonth.now();
        when(findAllUpcomingDebtsUseCase.getByEmail("user@test.com")).thenReturn(List.of(
                upcoming(1, "TV Liverpool", now.plusMonths(3), UpcomingDebtStatus.PENDING),
                upcoming(2, "Old Phone", now.minusMonths(1), UpcomingDebtStatus.PENDING),
                upcoming(3, "Fridge", now.minusMonths(2), UpcomingDebtStatus.STARTED)));
        MonthProjectionDto month = new MonthProjectionDto();
        month.setMonth(now.plusMonths(3));
        month.setCurrentDebts(500);
        month.setUpcomingDebts(1000);
        month.setFixedExpenses(2000);
        month.setTotalCommitments(3500);
        month.setAvailableIncome(-100.0);
        month.setStartingThisMonth(List.of("TV Liverpool"));
        PaymentProjectionDto projection = new PaymentProjectionDto();
        projection.setSalary(3400.0);
        projection.setMonths(List.of(month));
        projection.setPeakMonth(month);
        projection.setPeakCommitments(3500);
        when(getPaymentProjectionUseCase.getProjection("user@test.com", 12)).thenReturn(projection);

        mockMvc.perform(get("/ui/upcoming-debts").sessionAttr("userEmail", "user@test.com"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("TV Liverpool")))
                .andExpect(content().string(containsString("Visa Oro")))
                .andExpect(content().string(containsString("in 3 months")))
                .andExpect(content().string(containsString("1 purchase reached its first payment month")))
                .andExpect(content().string(containsString("Starts: TV Liverpool")))
                .andExpect(content().string(containsString("Fridge")))
                .andExpect(content().string(containsString("Back to pile")))
                .andExpect(content().string(containsString("-$100.00")))
                .andExpect(content().string(containsString("Visa Oro · $1,000.00 × 12")))
                .andExpect(content().string(containsString("data-cancel-action=\"/ui/upcoming-debts/1/cancel\"")));
    }

    @Test
    void list_renders_in_spanish() throws Exception {
        YearMonth now = YearMonth.now();
        when(findAllUpcomingDebtsUseCase.getByEmail("user@test.com")).thenReturn(List.of(
                upcoming(1, "TV Liverpool", now.plusMonths(3), UpcomingDebtStatus.PENDING)));
        PaymentProjectionDto projection = new PaymentProjectionDto();
        projection.setMonths(List.of());
        when(getPaymentProjectionUseCase.getProjection("user@test.com", 12)).thenReturn(projection);

        mockMvc.perform(get("/ui/upcoming-debts").param("lang", "es_MX").sessionAttr("userEmail", "user@test.com"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("¿Y si compro…?")))
                .andExpect(content().string(containsString("en 3 meses")));
    }

    @Test
    void cool_down_list_shows_days_left_and_plans_into_the_pile() throws Exception {
        WishItem tv = new WishItem();
        tv.setId(7);
        tv.setDescription("OLED TV");
        tv.setPrice(new BigDecimal("24000"));
        tv.setInstallments(12);
        tv.setStatus(WishStatus.WAITING);
        tv.setActive(true);
        tv.setCreatedAt(java.time.Instant.now().minus(java.time.Duration.ofDays(3)));
        WishItem dropped = new WishItem();
        dropped.setDescription("Sneakers");
        dropped.setPrice(new BigDecimal("2500"));
        dropped.setStatus(WishStatus.DROPPED);
        dropped.setActive(true);
        when(manageWishListUseCase.getByEmail("user@test.com")).thenReturn(List.of(tv, dropped));
        when(findAllUpcomingDebtsUseCase.getByEmail("user@test.com")).thenReturn(List.of());
        PaymentProjectionDto projection = new PaymentProjectionDto();
        projection.setMonths(List.of());
        when(getPaymentProjectionUseCase.getProjection("user@test.com", 12)).thenReturn(projection);

        mockMvc.perform(get("/ui/upcoming-debts").sessionAttr("userEmail", "user@test.com"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("OLED TV")))
                .andExpect(content().string(containsString("4 days left")))
                .andExpect(content().string(containsString("data-f-wish-id=\"7\"")))
                .andExpect(content().string(containsString("skipped $2,500")));

        mockMvc.perform(post("/ui/upcoming-debts").sessionAttr("userEmail", "user@test.com")
                        .param("description", "OLED TV").param("totalAmount", "24000").param("installments", "12")
                        .param("firstPaymentMonth", "2027-01").param("wishId", "7"))
                .andExpect(redirectedUrl("/ui/upcoming-debts"));
        verify(manageWishListUseCase).markPlanned(7);
    }

    @Test
    void list_without_session_redirects_to_login() throws Exception {
        mockMvc.perform(get("/ui/upcoming-debts"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/ui"));
    }

    @Test
    void create_with_invalid_input_flashes_error() throws Exception {
        when(manageUpcomingDebtUseCase.create(eq("user@test.com"), any()))
                .thenThrow(new IllegalArgumentException("Enter the total amount or the monthly payment"));

        mockMvc.perform(post("/ui/upcoming-debts").sessionAttr("userEmail", "user@test.com")
                        .param("description", "TV").param("installments", "12").param("firstPaymentMonth", "2027-01"))
                .andExpect(redirectedUrl("/ui/upcoming-debts"))
                .andExpect(flash().attribute("formError", "Enter the total amount or the monthly payment"));
    }

    @Test
    void create_binds_month_input() throws Exception {
        mockMvc.perform(post("/ui/upcoming-debts").sessionAttr("userEmail", "user@test.com")
                        .param("description", "TV").param("totalAmount", "12000").param("installments", "12")
                        .param("firstPaymentMonth", "2027-01").param("purchaseDate", "2026-10-12"))
                .andExpect(redirectedUrl("/ui/upcoming-debts"));

        verify(manageUpcomingDebtUseCase).create(eq("user@test.com"),
                org.mockito.ArgumentMatchers.argThat(c -> YearMonth.of(2027, 1).equals(c.firstPaymentMonth())
                        && c.purchaseDate() != null && c.installments() == 12));
    }

    @Test
    void start_returns_to_statement_preview() throws Exception {
        mockMvc.perform(post("/ui/upcoming-debts/5/start").sessionAttr("userEmail", "user@test.com")
                        .param("returnTo", "/ui/v2/statements/VISA/preview"))
                .andExpect(redirectedUrl("/ui/v2/statements/VISA/preview"));
        verify(manageUpcomingDebtUseCase).markAsStarted(5);
    }

    @Test
    void start_ignores_external_return_url() throws Exception {
        mockMvc.perform(post("/ui/upcoming-debts/5/start").sessionAttr("userEmail", "user@test.com")
                        .param("returnTo", "https://evil.example.com"))
                .andExpect(redirectedUrl("/ui/upcoming-debts"));
    }
}
