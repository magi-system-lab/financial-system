package com.jimm0063.magi.debt.management.debtmanagementltesystem.infrastructure.web.view;

import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.application.port.in.GetDebtProgressionUseCase;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.dto.CardProgressionDto;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.dto.MonthProgressionDto;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.enums.UpcomingDebtStatus;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.model.UpcomingDebt;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DebtProgressionViewController.class)
@Import(SecurityConfig.class)
class DebtProgressionViewControllerTest {

    @Autowired MockMvc mockMvc;

    @MockBean GetDebtProgressionUseCase getDebtProgressionUseCase;
    @MockBean ActivityLogHelper activityLogHelper;

    @Test
    void progression_shows_upcoming_purchases() throws Exception {
        YearMonth jan = YearMonth.of(2027, 1);
        UpcomingDebt tv = new UpcomingDebt();
        tv.setDescription("TV Liverpool");
        tv.setMonthlyPayment(new BigDecimal("1000.00"));
        tv.setInstallments(12);
        tv.setFirstPaymentDate(jan.atDay(1));
        tv.setStatus(UpcomingDebtStatus.PENDING);
        tv.setActive(true);
        CardProgressionDto card = new CardProgressionDto("VISA", "Visa Oro", List.of(), 1,
                new BigDecimal("1500.00"), new BigDecimal("16500.00"), List.of(tv), new BigDecimal("1000.00"));
        when(getDebtProgressionUseCase.getProgression(eq("user@test.com"), any()))
                .thenReturn(List.of(new MonthProgressionDto(jan, "enero 2027", new BigDecimal("1500.00"), List.of(card))));

        mockMvc.perform(get("/ui/progression").param("targetMonth", "2027-01").sessionAttr("userEmail", "user@test.com"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Inicia diferida")))
                .andExpect(content().string(containsString("incl. $1,000.00 de compras diferidas")))
                .andExpect(content().string(containsString("TV Liverpool · $1,000.00 x 12")));
    }
}
