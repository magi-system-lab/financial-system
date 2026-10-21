package com.jimm0063.magi.debt.management.debtmanagementltesystem.infrastructure.web.view;

import com.jimm0063.magi.debt.management.debtmanagementltesystem.application.service.ReceivableService;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.application.port.in.GetReceivableHistoryUseCase;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.application.port.in.GetReceivableStatusUseCase;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.application.port.in.RecordReceivablePaymentUseCase;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.application.port.out.ReceivablePaymentRepository;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.application.port.out.ReceivableRepository;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.dto.ReceivableHistoryDto;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.model.DebtSysUser;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.model.Receivable;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.model.ReceivablePayment;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.infrastructure.configuration.SecurityConfig;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.infrastructure.mapper.ReceivableMapper;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.infrastructure.model.ReceivablePaymentReq;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(ReceivableViewController.class)
@Import(SecurityConfig.class)
class ReceivableViewControllerTest {

    @Autowired MockMvc mockMvc;

    @MockBean ReceivableRepository receivableRepository;
    @MockBean ReceivableMapper receivableMapper;
    @MockBean GetReceivableStatusUseCase getReceivableStatusUseCase;
    @MockBean RecordReceivablePaymentUseCase recordReceivablePaymentUseCase;
    @MockBean GetReceivableHistoryUseCase getReceivableHistoryUseCase;
    @MockBean ActivityLogHelper activityLogHelper;

    private static final String EMAIL = "user@test.com";

    // ── helpers ────────────────────────────────────────────────────────────────

    private MockHttpSession session() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("userEmail", EMAIL);
        return session;
    }

    private static ReceivablePayment payment(int id, String amount, LocalDate date, String note) {
        ReceivablePayment p = new ReceivablePayment();
        p.setId(id);
        p.setAmount(new BigDecimal(amount));
        p.setPaymentDate(date);
        p.setNote(note);
        p.setCreatedAt(Instant.now());
        return p;
    }

    // Built by the real service so the template sees exactly what production produces
    private static ReceivableHistoryDto sampleHistory(String ownerEmail) {
        DebtSysUser owner = new DebtSysUser();
        owner.setEmail(ownerEmail);
        Receivable receivable = new Receivable();
        receivable.setId(3);
        receivable.setDebtorName("Prima Ana");
        receivable.setDescription("Card purchase");
        receivable.setPrincipalAmount(new BigDecimal("1200"));
        receivable.setLentDate(LocalDate.of(2026, 5, 1));
        receivable.setActive(true);
        receivable.setDebtSysUser(owner);

        ReceivableRepository repo = Mockito.mock(ReceivableRepository.class);
        ReceivablePaymentRepository paymentRepo = Mockito.mock(ReceivablePaymentRepository.class);
        when(repo.findById(3)).thenReturn(Optional.of(receivable));
        when(paymentRepo.findAllByReceivableId(3)).thenReturn(List.of(
                payment(2, "250.50", LocalDate.of(2026, 7, 14), "transfer"),
                payment(1, "300", LocalDate.of(2026, 6, 2), "cash")));
        return new ReceivableService(repo, paymentRepo).getHistory(3);
    }

    // ── detail ─────────────────────────────────────────────────────────────────

    @Test
    void detail_renders_history_and_chart_data() throws Exception {
        ReceivableHistoryDto history = sampleHistory(EMAIL);
        when(getReceivableHistoryUseCase.getHistory(3)).thenReturn(history);

        mockMvc.perform(get("/ui/receivables/3").session(session()))
                .andExpect(status().isOk())
                .andExpect(view().name("receivables/detail"))
                .andExpect(content().string(containsString("Prima Ana")))
                .andExpect(content().string(containsString("14 Jul 2026")))
                .andExpect(content().string(containsString("$649.50")))  // remaining after both payments
                .andExpect(content().string(containsString("balanceChart")))
                .andExpect(content().string(containsString("\"2026-06\"")));
    }

    @Test
    void detail_of_another_users_receivable_redirects_to_list() throws Exception {
        ReceivableHistoryDto history = sampleHistory("someone@else.com");
        when(getReceivableHistoryUseCase.getHistory(3)).thenReturn(history);

        mockMvc.perform(get("/ui/receivables/3").session(session()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/ui/receivables"));
    }

    @Test
    void detail_without_session_redirects_home() throws Exception {
        mockMvc.perform(get("/ui/receivables/3"))
                .andExpect(redirectedUrl("/ui"));
    }

    // ── registerPayment ────────────────────────────────────────────────────────

    @Test
    void payment_from_detail_binds_date_and_returns_to_detail() throws Exception {
        mockMvc.perform(post("/ui/receivables/3/payment").session(session())
                        .param("amount", "150")
                        .param("paymentDate", "2026-08-03")
                        .param("note", "covers Aug")
                        .param("returnTo", "detail"))
                .andExpect(redirectedUrl("/ui/receivables/3"));

        ArgumentCaptor<ReceivablePaymentReq> captor = ArgumentCaptor.forClass(ReceivablePaymentReq.class);
        verify(recordReceivablePaymentUseCase).registerPayment(eq(3), captor.capture());
        assertThat(captor.getValue().getPaymentDate()).isEqualTo(LocalDate.of(2026, 8, 3));
        assertThat(captor.getValue().getAmount()).isEqualByComparingTo("150");
    }

    @Test
    void payment_from_list_returns_to_list() throws Exception {
        mockMvc.perform(post("/ui/receivables/3/payment").session(session()).param("amount", "150"))
                .andExpect(redirectedUrl("/ui/receivables"));
        verify(recordReceivablePaymentUseCase).registerPayment(eq(3), any());
    }
}
