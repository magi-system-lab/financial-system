package com.jimm0063.magi.debt.management.debtmanagementltesystem.infrastructure.web.view;

import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.application.port.in.DebtAccountStatusUseCase;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.application.port.in.FindAllDebtAccountUseCase;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.application.port.in.FindAllDebtsUseCase;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.application.port.out.DebtAccountRepository;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.application.port.out.PaymentRepository;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.application.port.out.UpcomingDebtRepository;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.dto.DebtAccountStatusDto;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.model.DebtAccount;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.infrastructure.configuration.SecurityConfig;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.infrastructure.mapper.DebtAccountMapper;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.infrastructure.model.CreateDebtAccountReq;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DebtAccountViewController.class)
@Import({SecurityConfig.class, UiFormat.class, CardColors.class})
class DebtAccountViewControllerTest {

    private static final String EMAIL = "user@test.com";

    @Autowired MockMvc mockMvc;

    @MockBean DebtAccountRepository debtAccountRepository;
    @MockBean DebtAccountMapper debtAccountMapper;
    @MockBean FindAllDebtAccountUseCase findAllDebtAccountUseCase;
    @MockBean FindAllDebtsUseCase findAllDebtsUseCase;
    @MockBean DebtAccountStatusUseCase debtAccountStatusUseCase;
    @MockBean PaymentRepository paymentRepository;
    @MockBean UpcomingDebtRepository upcomingDebtRepository;
    @MockBean ActivityLogHelper activityLogHelper;

    @BeforeEach
    void setUp() {
        when(debtAccountRepository.findDebtAccountByCodeAndActiveTrue("acc-azul")).thenReturn(Optional.of(CardsFixtures.azul()));
        when(findAllDebtsUseCase.getActiveByDebtAccount("acc-azul")).thenReturn(List.of(CardsFixtures.macbook()));
        when(debtAccountStatusUseCase.getStatus("acc-azul")).thenReturn(new DebtAccountStatusDto());
        when(paymentRepository.findByDebtAccountCode("acc-azul")).thenReturn(List.of());
        when(upcomingDebtRepository.findAllPendingByDebtAccount("acc-azul")).thenReturn(List.of());
    }

    @Test
    void detail_shows_card_debts_and_edit_data() throws Exception {
        mockMvc.perform(get("/ui/debt-accounts/acc-azul").sessionAttr("userEmail", EMAIL))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("--card-color:#0B3D91")))
                .andExpect(content().string(containsString("MacBook Air")))
                .andExpect(content().string(containsString("9 of 12")))
                .andExpect(content().string(containsString("$6,250 left")))
                .andExpect(content().string(containsString("data-action=\"/ui/debts/7/update\"")))
                .andExpect(content().string(containsString("data-f-monthly-payment=\"2083.25\"")))
                .andExpect(content().string(containsString("Record payment")))
                .andExpect(content().string(containsString("href=\"/ui/cards\" aria-current=\"page\"")));
    }

    @Test
    void unknown_card_shows_not_found_message() throws Exception {
        when(debtAccountRepository.findDebtAccountByCodeAndActiveTrue("nope")).thenReturn(Optional.empty());
        when(findAllDebtsUseCase.getActiveByDebtAccount("nope")).thenReturn(List.of());
        when(debtAccountStatusUseCase.getStatus("nope")).thenReturn(new DebtAccountStatusDto());
        mockMvc.perform(get("/ui/debt-accounts/nope").sessionAttr("userEmail", EMAIL))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("doesn’t exist")));
    }

    @Test
    void colour_is_saved_and_auto_resets_it() throws Exception {
        mockMvc.perform(post("/ui/debt-accounts/acc-azul/color").param("color", "#0F766E").sessionAttr("userEmail", EMAIL))
                .andExpect(redirectedUrl("/ui/debt-accounts/acc-azul"));
        verify(debtAccountRepository).updateColor("acc-azul", "#0F766E");

        mockMvc.perform(post("/ui/debt-accounts/acc-azul/color").param("color", "auto").sessionAttr("userEmail", EMAIL))
                .andExpect(redirectedUrl("/ui/debt-accounts/acc-azul"));
        verify(debtAccountRepository).updateColor("acc-azul", null);
    }

    @Test
    void new_card_gets_a_generated_code_and_opens_its_detail() throws Exception {
        DebtAccount saved = new DebtAccount();
        saved.setCode("generated");
        when(debtAccountMapper.toModel(any(CreateDebtAccountReq.class))).thenReturn(new DebtAccount());
        when(debtAccountRepository.save(any(), eq("prov-bbva"))).thenReturn(saved);

        mockMvc.perform(post("/ui/debt-accounts/prov-bbva").param("name", "Oro").param("color", "auto")
                        .sessionAttr("userEmail", EMAIL))
                .andExpect(redirectedUrl("/ui/debt-accounts/generated"));

        ArgumentCaptor<CreateDebtAccountReq> req = ArgumentCaptor.forClass(CreateDebtAccountReq.class);
        verify(debtAccountMapper).toModel(req.capture());
        assertThat(req.getValue().getCode()).isNotBlank();
        assertThat(req.getValue().getColor()).isNull();
    }

    @Test
    void old_provider_list_opens_the_filtered_wallet() throws Exception {
        mockMvc.perform(get("/ui/debt-accounts").param("providerCode", "prov bbva").sessionAttr("userEmail", EMAIL))
                .andExpect(redirectedUrl("/ui/cards?provider=prov%20bbva"));
    }

    @Test
    void deleting_a_card_returns_to_the_wallet() throws Exception {
        mockMvc.perform(post("/ui/debt-accounts/acc-azul").param("_method", "DELETE").sessionAttr("userEmail", EMAIL))
                .andExpect(redirectedUrl("/ui/cards"));
        verify(debtAccountRepository).delete("acc-azul");
    }
}
