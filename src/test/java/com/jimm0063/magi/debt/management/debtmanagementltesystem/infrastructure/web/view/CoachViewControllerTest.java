package com.jimm0063.magi.debt.management.debtmanagementltesystem.infrastructure.web.view;

import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.application.port.in.FindAllDebtsUseCase;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.application.port.in.GetCoachTipsUseCase;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.application.port.in.GetFinancialStatusUseCase;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.application.port.in.GetPayoffPlanUseCase;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.application.port.in.ManageSavingsGoalUseCase;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.dto.CoachTipDto;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.dto.SavingsGoalStatusDto;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.dto.UserStatusDashboard;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.enums.CoachTipSeverity;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.enums.CoachTipType;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.enums.PayoffStrategy;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.enums.SavingsGoalStatus;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.model.Debt;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.model.DebtAccount;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.model.SavingsGoal;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.utils.PayoffSimulator;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.infrastructure.configuration.SecurityConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CoachViewController.class)
@Import(SecurityConfig.class)
class CoachViewControllerTest {

    private static final String EMAIL = "user@test.com";

    @Autowired MockMvc mockMvc;

    @MockBean GetCoachTipsUseCase getCoachTipsUseCase;
    @MockBean GetPayoffPlanUseCase getPayoffPlanUseCase;
    @MockBean ManageSavingsGoalUseCase manageSavingsGoalUseCase;
    @MockBean FindAllDebtsUseCase findAllDebtsUseCase;
    @MockBean GetFinancialStatusUseCase getFinancialStatusUseCase;
    @MockBean ActivityLogHelper activityLogHelper;

    private final List<Debt> debts = List.of(debt(1, "Phone", "100", 10, 12), debt(2, "Laptop", "500", 8, 12));

    private static Debt debt(int id, String desc, String monthly, int paid, int term) {
        DebtAccount visa = new DebtAccount();
        visa.setCode("VISA");
        visa.setName("Visa Oro");
        Debt d = new Debt();
        d.setId(id);
        d.setDescription(desc);
        d.setMonthlyPayment(new BigDecimal(monthly));
        d.setCurrentInstallment(paid);
        d.setMaxFinancingTerm(term);
        d.setDebtAccount(visa);
        d.setActive(true);
        return d;
    }

    @BeforeEach
    void setUp() {
        UserStatusDashboard dashboard = new UserStatusDashboard();
        dashboard.setSalary(10000.0);
        dashboard.setSavings(2000.0);
        dashboard.setMonthlyFixedExpensesAmount(3000.0);
        when(getFinancialStatusUseCase.getUserStatus(EMAIL)).thenReturn(dashboard);
        when(findAllDebtsUseCase.getActiveByUser(EMAIL)).thenReturn(debts);
        // real simulation over the fixture debts
        when(getPayoffPlanUseCase.getPlan(eq(EMAIL), any(), any(), any(), anyBoolean())).thenAnswer(i ->
                PayoffSimulator.simulate(debts, YearMonth.now(), i.getArgument(1), i.getArgument(2), i.getArgument(3), i.getArgument(4)));
        when(getCoachTipsUseCase.getTips(EMAIL)).thenReturn(List.of(
                CoachTipDto.of(CoachTipType.SAFETY_LOW, CoachTipSeverity.WARN, 0.5, new BigDecimal("8600"))));
    }

    @Test
    void redirects_without_user() throws Exception {
        mockMvc.perform(get("/ui/coach")).andExpect(redirectedUrl("/ui"));
    }

    @Test
    void renders_tips_plan_with_the_suggested_extra_and_goals() throws Exception {
        SavingsGoal g = new SavingsGoal();
        g.setId(4);
        g.setName("Laptop payoff");
        g.setStatus(SavingsGoalStatus.ACTIVE);
        g.setDebtId(2);
        g.setTargetDate(LocalDate.now().plusMonths(2));
        SavingsGoalStatusDto status = new SavingsGoalStatusDto(g, new BigDecimal("2000"), new BigDecimal("2000"),
                BigDecimal.ZERO, 100, debts.get(1), false, 2, null, BigDecimal.ZERO, List.of());
        when(manageSavingsGoalUseCase.getByEmail(EMAIL)).thenReturn(List.of(status));

        // free = 10,000 − 3,000 − 600 = 6,400 → suggested 600
        mockMvc.perform(get("/ui/coach").sessionAttr("userEmail", EMAIL))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Build your safety net first")))
                .andExpect(content().string(containsString("Your savings cover 0.5 months of payments and fixed expenses. Put $8,600 aside")))
                .andExpect(content().string(containsString("Suggested: $600 a month")))
                .andExpect(content().string(containsString("value=\"600\"")))
                .andExpect(content().string(containsString("Laptop payoff")))
                .andExpect(content().string(containsString("Ready to pay off")))
                .andExpect(content().string(containsString("/ui/coach/goals/4/pay-off")))
                .andExpect(content().string(containsString("Emergency fund ($8,800)")))
                .andExpect(content().string(containsString("Goal set")));
        verify(getPayoffPlanUseCase).getPlan(EMAIL, new BigDecimal("600"), BigDecimal.ZERO, PayoffStrategy.FLOW, true);
    }

    @Test
    void plan_form_values_and_unchecked_snowball() throws Exception {
        when(manageSavingsGoalUseCase.getByEmail(EMAIL)).thenReturn(List.of());

        mockMvc.perform(get("/ui/coach").param("f", "1").param("extra", "250").param("lump", "200")
                        .param("strategy", "SMALLEST").param("lang", "es_MX").sessionAttr("userEmail", EMAIL))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Plan de ataque")))
                .andExpect(content().string(containsString("Bola de nieve: primero los saldos más chicos")))
                .andExpect(content().string(containsString("Ahorrar para esto")));
        verify(getPayoffPlanUseCase).getPlan(EMAIL, new BigDecimal("250"), new BigDecimal("200"), PayoffStrategy.SMALLEST, false);
    }

    @Test
    void create_goal_with_month_and_errors_come_back_as_flash() throws Exception {
        mockMvc.perform(post("/ui/coach/goals").param("name", "Trip").param("targetAmount", "5000")
                        .param("targetMonth", "2027-03").param("debtId", "").sessionAttr("userEmail", EMAIL))
                .andExpect(redirectedUrl("/ui/coach#goals"));
        verify(manageSavingsGoalUseCase).create(EMAIL, "Trip", new BigDecimal("5000"), LocalDate.of(2027, 3, 31), null);

        when(manageSavingsGoalUseCase.contribute(eq(4), any(), any(), any()))
                .thenThrow(new IllegalArgumentException("You can't take out more than what is saved"));
        mockMvc.perform(post("/ui/coach/goals/4/contributions").param("amount", "300").param("withdraw", "1")
                        .sessionAttr("userEmail", EMAIL))
                .andExpect(redirectedUrl("/ui/coach#goals"))
                .andExpect(flash().attribute("formError", "You can't take out more than what is saved"));
        verify(manageSavingsGoalUseCase).contribute(4, new BigDecimal("-300"), null, null);
    }
}
