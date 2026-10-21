package com.jimm0063.magi.debt.management.debtmanagementltesystem.infrastructure.web.view;

import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.application.port.in.FindAllUpcomingDebtsUseCase;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.application.port.in.FindAllDebtAccountUseCase;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.application.port.in.FindAllFinancialProviderUseCase;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.application.port.in.MarkDebtAsIgnorableUseCase;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.application.port.in.PreviewBulkStatementUseCase;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.application.port.in.SyncBulkStatementUseCase;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.enums.AccountStatementType;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.enums.BulkItemAction;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.enums.DebtDiffStatus;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.model.BulkStatementBatch;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.model.BulkStatementItem;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.model.BulkStatementUpload;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.model.BulkSyncResult;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.model.Debt;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.model.DebtAccount;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.model.DebtDiff;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.model.FinancialProvider;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.model.StatementDiffResult;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.infrastructure.configuration.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static org.hamcrest.Matchers.containsString;

@WebMvcTest(BulkStatementViewController.class)
@Import(SecurityConfig.class)
class BulkStatementViewControllerTest {

    @Autowired MockMvc mockMvc;

    @MockBean PreviewBulkStatementUseCase previewUseCase;
    @MockBean SyncBulkStatementUseCase syncUseCase;
    @MockBean MarkDebtAsIgnorableUseCase markIgnorableUseCase;
    @MockBean FindAllFinancialProviderUseCase findAllFinancialProviderUseCase;
    @MockBean FindAllDebtAccountUseCase findAllDebtAccountUseCase;
    @MockBean ActivityLogHelper activityLogHelper;
    @MockBean FindAllUpcomingDebtsUseCase findAllUpcomingDebtsUseCase;

    private static final String EMAIL = "user@test.com";

    // ── helpers ────────────────────────────────────────────────────────────────

    private static Debt debt(String desc, int current, int max) {
        Debt d = new Debt();
        d.setDescription(desc);
        d.setHashSum("h-" + desc);
        d.setMonthlyPayment(BigDecimal.TEN);
        d.setCurrentInstallment(current);
        d.setMaxFinancingTerm(max);
        return d;
    }

    private static BulkStatementBatch sampleBatch() {
        BulkStatementItem ok = new BulkStatementItem("visa.pdf", "VISA", BulkItemAction.UPDATE);
        ok.setAccountName("Visa Oro");
        ok.setProviderName("Bank");
        ok.setDiff(new StatementDiffResult("VISA", AccountStatementType.UNIVERSAL,
                List.of(new DebtDiff(debt("New TV", 1, 12), null, DebtDiffStatus.NEW)),
                List.of(new DebtDiff(debt("Laptop", 4, 12), debt("Laptop", 3, 12), DebtDiffStatus.UPDATED)),
                List.of(), List.of(), List.of()));
        ok.setMissingFromStatement(new ArrayList<>(List.of(debt("Phantom charge", 2, 6))));
        ok.setExistingActiveCount(2);

        BulkStatementItem failed = new BulkStatementItem("amex.pdf", "AMEX", BulkItemAction.UPDATE);
        failed.setAccountName("Amex");
        failed.setError("PDF is password protected");
        failed.setNeedsPdfPassword(true);
        failed.setRetryable(true);
        return new BulkStatementBatch(new ArrayList<>(List.of(ok, failed)));
    }

    private static MockHttpSession sessionWith(BulkStatementBatch batch) {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("userEmail", EMAIL);
        if (batch != null) session.setAttribute(BulkStatementViewController.BATCH_KEY, batch);
        return session;
    }

    // ── session guard ─────────────────────────────────────────────────────────

    @Test
    void every_page_without_login_redirects_to_login() throws Exception {
        mockMvc.perform(get("/ui/bulk-statements")).andExpect(redirectedUrl("/ui"));
        mockMvc.perform(get("/ui/bulk-statements/preview")).andExpect(redirectedUrl("/ui"));
        mockMvc.perform(post("/ui/bulk-statements/execute")).andExpect(redirectedUrl("/ui"));
    }

    @Test
    void preview_without_pending_batch_redirects_to_upload() throws Exception {
        mockMvc.perform(get("/ui/bulk-statements/preview").session(sessionWith(null)))
                .andExpect(redirectedUrl("/ui/bulk-statements"));
    }

    // ── upload ────────────────────────────────────────────────────────────────

    @Test
    void upload_form_lists_cards_grouped_by_provider() throws Exception {
        FinancialProvider provider = new FinancialProvider();
        provider.setCode("P1");
        provider.setName("BBVA Bank");
        DebtAccount account = new DebtAccount();
        account.setCode("VISA");
        account.setName("Visa Oro");
        account.setAccountStatementType(AccountStatementType.BBVA);
        when(findAllFinancialProviderUseCase.getActiveByEmail(EMAIL)).thenReturn(List.of(provider));
        when(findAllDebtAccountUseCase.getActiveByFinancialProvider("P1")).thenReturn(List.of(account));

        mockMvc.perform(get("/ui/bulk-statements").session(sessionWith(null)))
                .andExpect(status().isOk())
                .andExpect(view().name("bulk-statements/upload"))
                .andExpect(content().string(containsString("label=\"BBVA Bank\"")))
                .andExpect(content().string(containsString("Visa Oro · BBVA")));
    }

    @Test
    void extract_binds_indexed_rows_and_stores_batch_in_session() throws Exception {
        BulkStatementBatch batch = sampleBatch();
        when(previewUseCase.preview(anyList(), eq(BulkItemAction.SOURCE_OF_TRUTH))).thenReturn(batch);
        MockHttpSession session = sessionWith(null);

        mockMvc.perform(multipart("/ui/bulk-statements/extract")
                        .file(new MockMultipartFile("rows[0].file", "visa.pdf", "application/pdf", new byte[]{1}))
                        .file(new MockMultipartFile("rows[1].file", "amex.pdf", "application/pdf", new byte[]{2}))
                        .param("rows[0].debtAccountCode", "VISA")
                        .param("rows[0].pdfPassword", "a,b")
                        .param("rows[1].debtAccountCode", "AMEX")
                        .param("rows[1].pdfPassword", "")
                        .param("defaultAction", "SOURCE_OF_TRUTH")
                        .session(session))
                .andExpect(redirectedUrl("/ui/bulk-statements/preview"));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<BulkStatementUpload>> uploads = ArgumentCaptor.forClass(List.class);
        verify(previewUseCase).preview(uploads.capture(), eq(BulkItemAction.SOURCE_OF_TRUTH));
        assertThat(uploads.getValue()).hasSize(2);
        assertThat(uploads.getValue().get(0).debtAccountCode()).isEqualTo("VISA");
        assertThat(uploads.getValue().get(0).pdfPassword()).isEqualTo("a,b");
        assertThat(uploads.getValue().get(0).file().getOriginalFilename()).isEqualTo("visa.pdf");
        assertThat(uploads.getValue().get(1).debtAccountCode()).isEqualTo("AMEX");
        assertThat(session.getAttribute(BulkStatementViewController.BATCH_KEY)).isSameAs(batch);
    }

    @Test
    void extract_without_files_goes_back_with_error() throws Exception {
        mockMvc.perform(multipart("/ui/bulk-statements/extract").session(sessionWith(null)))
                .andExpect(redirectedUrl("/ui/bulk-statements"))
                .andExpect(flash().attributeExists("uploadError"));
        verify(previewUseCase, never()).preview(anyList(), any());
    }

    // ── review ────────────────────────────────────────────────────────────────

    @Test
    void preview_renders_every_card_with_its_diff_and_errors() throws Exception {
        mockMvc.perform(get("/ui/bulk-statements/preview").session(sessionWith(sampleBatch())))
                .andExpect(status().isOk())
                .andExpect(view().name("bulk-statements/preview"))
                .andExpect(content().string(containsString("Visa Oro")))
                .andExpect(content().string(containsString("New TV")))
                .andExpect(content().string(containsString("Laptop")))
                .andExpect(content().string(containsString("Phantom charge")))
                .andExpect(content().string(containsString("NOT IN STATEMENT")))
                .andExpect(content().string(containsString("PDF is password protected")))
                .andExpect(content().string(containsString("Retry this file")))
                .andExpect(content().string(containsString("Rebuild from 0")));
    }

    @Test
    void set_action_changes_only_that_card() throws Exception {
        BulkStatementBatch batch = sampleBatch();

        mockMvc.perform(post("/ui/bulk-statements/items/0/action")
                        .param("action", "SOURCE_OF_TRUTH")
                        .session(sessionWith(batch)))
                .andExpect(redirectedUrl("/ui/bulk-statements/preview#item-0"));

        assertThat(batch.items().get(0).getAction()).isEqualTo(BulkItemAction.SOURCE_OF_TRUTH);
        assertThat(batch.items().get(1).getAction()).isEqualTo(BulkItemAction.UPDATE);
    }

    @Test
    void set_action_for_all_skips_failed_cards() throws Exception {
        BulkStatementBatch batch = sampleBatch();

        mockMvc.perform(post("/ui/bulk-statements/action-all")
                        .param("action", "SKIP")
                        .session(sessionWith(batch)))
                .andExpect(redirectedUrl("/ui/bulk-statements/preview"));

        assertThat(batch.items().get(0).getAction()).isEqualTo(BulkItemAction.SKIP);
        assertThat(batch.items().get(1).getAction()).isEqualTo(BulkItemAction.UPDATE);
    }

    @Test
    void mark_ignorable_recomputes_only_that_card() throws Exception {
        BulkStatementBatch batch = sampleBatch();

        mockMvc.perform(post("/ui/bulk-statements/items/0/mark-ignorable")
                        .param("hashSum", "h-New TV")
                        .param("reason", "not mine")
                        .session(sessionWith(batch)))
                .andExpect(redirectedUrl("/ui/bulk-statements/preview#item-0"));

        verify(markIgnorableUseCase).markAsIgnorable("h-New TV", "not mine");
        verify(previewUseCase).recompute(batch.items().get(0));
    }

    @Test
    void retry_reextracts_failed_card() throws Exception {
        BulkStatementBatch batch = sampleBatch();

        mockMvc.perform(multipart("/ui/bulk-statements/items/1/retry")
                        .file(new MockMultipartFile("file", "amex.pdf", "application/pdf", new byte[]{1}))
                        .param("pdfPassword", "secret")
                        .session(sessionWith(batch)))
                .andExpect(redirectedUrl("/ui/bulk-statements/preview#item-1"));

        verify(previewUseCase).retry(eq(batch.items().get(1)), any(), eq("secret"));
    }

    // ── execute ───────────────────────────────────────────────────────────────

    @Test
    void execute_persists_clears_session_and_shows_result() throws Exception {
        BulkStatementBatch batch = sampleBatch();
        BulkSyncResult result = new BulkSyncResult(List.of(new BulkSyncResult.Entry(
                "visa.pdf", "VISA", "Visa Oro", "UPDATE", BulkSyncResult.Outcome.APPLIED, 1, 1, 0, null)));
        when(syncUseCase.sync(batch)).thenReturn(result);
        MockHttpSession session = sessionWith(batch);

        mockMvc.perform(post("/ui/bulk-statements/execute").session(session))
                .andExpect(redirectedUrl("/ui/bulk-statements/result"))
                .andExpect(flash().attribute("result", result));

        assertThat(session.getAttribute(BulkStatementViewController.BATCH_KEY)).isNull();
    }

    @Test
    void execute_failure_keeps_batch_and_shows_error() throws Exception {
        BulkStatementBatch batch = sampleBatch();
        when(syncUseCase.sync(batch)).thenThrow(new RuntimeException("constraint violated"));
        MockHttpSession session = sessionWith(batch);

        mockMvc.perform(post("/ui/bulk-statements/execute").session(session))
                .andExpect(redirectedUrl("/ui/bulk-statements/preview"))
                .andExpect(flash().attribute("syncError", "constraint violated"));

        assertThat(session.getAttribute(BulkStatementViewController.BATCH_KEY)).isSameAs(batch);
    }

    @Test
    void result_page_renders_entries() throws Exception {
        BulkSyncResult result = new BulkSyncResult(List.of(new BulkSyncResult.Entry(
                "visa.pdf", "VISA", "Visa Oro", "SOURCE_OF_TRUTH", BulkSyncResult.Outcome.APPLIED, 3, 0, 5, "Rebuilt from statement")));

        mockMvc.perform(get("/ui/bulk-statements/result").session(sessionWith(null)).flashAttr("result", result))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Rebuilt from statement")))
                .andExpect(content().string(containsString("Visa Oro")));
    }

    @Test
    void result_page_without_result_redirects_to_upload() throws Exception {
        mockMvc.perform(get("/ui/bulk-statements/result").session(sessionWith(null)))
                .andExpect(redirectedUrl("/ui/bulk-statements"));
    }
}
