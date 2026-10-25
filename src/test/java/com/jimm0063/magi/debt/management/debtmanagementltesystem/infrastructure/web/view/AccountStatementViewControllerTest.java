package com.jimm0063.magi.debt.management.debtmanagementltesystem.infrastructure.web.view;

import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.application.port.in.DebtDuplicationPreventUseCase;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.application.port.in.ExtractFromFileUseCase;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.application.port.in.FilterDebtsUseCase;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.application.port.in.FindAllDebtsUseCase;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.application.port.in.LoadDebtList;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.application.port.in.MarkDebtAsIgnorableUseCase;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.application.port.in.SourceOfTruthImportUseCase;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.application.port.out.DebtAccountRepository;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.dto.AccountStatementPreviewDto;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.enums.AccountStatementType;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.enums.DebtTypeEnum;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.model.Debt;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.model.DebtAccount;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.infrastructure.configuration.SecurityConfig;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.infrastructure.mapper.DebtMapper;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.infrastructure.model.CreateDebtReq;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.dto.DebtInstallmentUpdateDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AccountStatementViewController.class)
@Import(SecurityConfig.class)
class AccountStatementViewControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean ExtractFromFileUseCase extractFromFileUseCase;
    @MockBean FilterDebtsUseCase filterDebtsUseCase;
    @MockBean DebtDuplicationPreventUseCase debtDuplicationPreventUseCase;
    @MockBean LoadDebtList loadDebtList;
    @MockBean SourceOfTruthImportUseCase sourceOfTruthImportUseCase;
    @MockBean DebtMapper debtMapper;
    @MockBean DebtAccountRepository debtAccountRepository;
    @MockBean FindAllDebtsUseCase findAllDebtsUseCase;
    @MockBean ActivityLogHelper activityLogHelper;
    @MockBean MarkDebtAsIgnorableUseCase markDebtAsIgnorableUseCase;

    private static final String ACC = "VISA_TEST";

    // ── session guard ─────────────────────────────────────────────────────────

    @Test
    void get_upload_form_without_session_redirects_to_login() throws Exception {
        mockMvc.perform(get("/ui/statements/" + ACC))
                .andExpect(redirectedUrl("/ui"));
    }

    @Test
    void post_add_without_session_redirects_to_login() throws Exception {
        mockMvc.perform(post("/ui/statements/" + ACC + "/add"))
                .andExpect(redirectedUrl("/ui"));
    }

    @Test
    void post_sync_without_session_redirects_to_login() throws Exception {
        mockMvc.perform(post("/ui/statements/" + ACC + "/sync"))
                .andExpect(redirectedUrl("/ui"));
    }

    @Test
    void post_replace_without_session_redirects_to_login() throws Exception {
        mockMvc.perform(post("/ui/statements/" + ACC + "/replace"))
                .andExpect(redirectedUrl("/ui"));
    }

    // ── extract ───────────────────────────────────────────────────────────────

    @Test
    void extract_redirects_to_preview_on_success() throws Exception {
        DebtAccount account = new DebtAccount();
        account.setCode(ACC);
        when(debtAccountRepository.findDebtAccountByCodeAndActiveTrue(ACC)).thenReturn(Optional.of(account));

        Debt debt = validDebt("Test Purchase", "hash-1");
        when(extractFromFileUseCase.extractDebts(any(), eq(ACC), eq(AccountStatementType.CSV_UPLOAD)))
                .thenReturn(List.of(debt));
        when(debtDuplicationPreventUseCase.getHashSum(any(), any())).thenReturn("hash-1");
        when(filterDebtsUseCase.previewAccountStatement(any(), eq(ACC)))
                .thenReturn(new AccountStatementPreviewDto(List.of(debt), List.of(), List.of(), List.of(), List.of()));

        MockMultipartFile file = new MockMultipartFile("file", "debts.csv", "text/csv",
                "description,operationDate,originalAmount,monthlyPayment,currentInstallment,maxFinancingTerm,debtType\n"
                        .getBytes());

        mockMvc.perform(multipart("/ui/statements/" + ACC + "/extract")
                        .file(file)
                        .param("accountStatementType", "CSV_UPLOAD")
                        .sessionAttr("userEmail", "user@test.com"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/ui/statements/" + ACC + "/preview"));
    }

    @Test
    void extract_redirects_to_error_on_parse_failure() throws Exception {
        when(extractFromFileUseCase.extractDebts(any(), any(), any()))
                .thenThrow(new IOException("cannot parse"));

        MockMultipartFile file = new MockMultipartFile("file", "bad.csv", "text/csv", "bad".getBytes());

        mockMvc.perform(multipart("/ui/statements/" + ACC + "/extract")
                        .file(file)
                        .param("accountStatementType", "CSV_UPLOAD")
                        .sessionAttr("userEmail", "user@test.com"))
                .andExpect(redirectedUrl("/ui/statements/" + ACC))
                .andExpect(flash().attributeExists("extractionError"));
    }

    @Test
    void extract_redirects_to_error_on_missing_fields() throws Exception {
        when(extractFromFileUseCase.extractDebts(any(), any(), any()))
                .thenThrow(new IllegalArgumentException("monthlyPayment required"));

        MockMultipartFile file = new MockMultipartFile("file", "bad.csv", "text/csv", "bad".getBytes());

        mockMvc.perform(multipart("/ui/statements/" + ACC + "/extract")
                        .file(file)
                        .param("accountStatementType", "CSV_UPLOAD")
                        .sessionAttr("userEmail", "user@test.com"))
                .andExpect(redirectedUrl("/ui/statements/" + ACC))
                .andExpect(flash().attributeExists("extractionError"));
    }

    @Test
    void preview_renders_editable_new_debts_updates_and_mode_actions() throws Exception {
        Debt tv = new Debt();
        tv.setDescription("New TV");
        tv.setMonthlyPayment(new BigDecimal("500.00"));
        tv.setCurrentInstallment(1);
        tv.setMaxFinancingTerm(12);
        tv.setHashSum("hash-tv");
        tv.setDebtType(DebtTypeEnum.CARD);
        Debt laptop = new Debt();
        laptop.setDescription("Laptop");
        laptop.setMonthlyPayment(new BigDecimal("900.00"));
        laptop.setMaxFinancingTerm(12);
        laptop.setHashSum("hash-laptop");
        DebtInstallmentUpdateDto update = new DebtInstallmentUpdateDto();
        update.setDebt(laptop);
        update.setPreviousInstallment(3);
        update.setNewInstallment(4);
        AccountStatementPreviewDto preview = new AccountStatementPreviewDto(
                List.of(tv), List.of(update), List.of(), List.of(), List.of());
        when(findAllDebtsUseCase.getActiveByDebtAccount(ACC)).thenReturn(List.of());

        mockMvc.perform(get("/ui/statements/" + ACC + "/preview")
                        .sessionAttr("userEmail", "user@test.com")
                        .sessionAttr("statementPreview", preview))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("name=\"debts[0].description\"")))
                .andExpect(content().string(containsString("value=\"New TV\"")))
                .andExpect(content().string(containsString("3 → 4 / 12")))
                .andExpect(content().string(containsString("name=\"typeOverrides[0].hashSum\"")))
                .andExpect(content().string(containsString("formaction=\"/ui/statements/" + ACC + "/sync\"")))
                .andExpect(content().string(containsString("formaction=\"/ui/statements/" + ACC + "/replace\"")))
                .andExpect(content().string(containsString("action=\"/ui/statements/" + ACC + "/mark-ignorable\"")));
    }

    // ── add ───────────────────────────────────────────────────────────────────

    @Test
    void add_saves_new_debts_and_redirects_to_account() throws Exception {
        Debt saved = validDebt("Test Purchase", "hash-1");
        when(debtMapper.toModel(any(CreateDebtReq.class))).thenReturn(saved);
        when(debtDuplicationPreventUseCase.getHashSum(any(), any())).thenReturn("hash-1");
        when(loadDebtList.saveUnrepeated(anyList(), eq(ACC))).thenReturn(List.of(saved));

        mockMvc.perform(post("/ui/statements/" + ACC + "/add")
                        .param("debts[0].description", "Test Purchase")
                        .param("debts[0].monthlyPayment", "500.00")
                        .param("debts[0].maxFinancingTerm", "6")
                        .param("debts[0].debtType", "CARD")
                        .sessionAttr("userEmail", "user@test.com"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/ui/debt-accounts/" + ACC));

        verify(loadDebtList).saveUnrepeated(anyList(), eq(ACC));
    }

    @Test
    void add_ignores_rows_with_blank_description() throws Exception {
        when(loadDebtList.saveUnrepeated(anyList(), any())).thenReturn(List.of());

        // First row has blank description — should be filtered out before save
        mockMvc.perform(post("/ui/statements/" + ACC + "/add")
                        .param("debts[0].description", "  ")   // blank
                        .param("debts[0].monthlyPayment", "100.00")
                        .sessionAttr("userEmail", "user@test.com"))
                .andExpect(redirectedUrl("/ui/debt-accounts/" + ACC));

        // saveUnrepeated still called but with empty list
        verify(loadDebtList).saveUnrepeated(eq(List.of()), eq(ACC));
    }

    // ── sync ──────────────────────────────────────────────────────────────────

    @Test
    void sync_applies_type_override_and_redirects() throws Exception {
        Debt sessionDebt = validDebt("Laptop", "hash-lap");
        sessionDebt.setDebtType(DebtTypeEnum.CARD);

        when(loadDebtList.saveUnrepeated(anyList(), eq(ACC))).thenReturn(List.of());

        mockMvc.perform(post("/ui/statements/" + ACC + "/sync")
                        .param("typeOverrides[0].hashSum", "hash-lap")
                        .param("typeOverrides[0].debtType", "LOAN")
                        .sessionAttr("userEmail", "user@test.com")
                        .sessionAttr("extractedDebts", new ArrayList<>(List.of(sessionDebt))))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/ui/debt-accounts/" + ACC));

        // Type override must have been applied to the session debt
        assertThat(sessionDebt.getDebtType()).isEqualTo(DebtTypeEnum.LOAN);
    }

    @Test
    void sync_redirects_back_to_upload_when_no_session_debts() throws Exception {
        // No extractedDebts in session → guard kicks in
        mockMvc.perform(post("/ui/statements/" + ACC + "/sync")
                        .sessionAttr("userEmail", "user@test.com"))
                .andExpect(redirectedUrl("/ui/statements/" + ACC));
    }

    // ── replace ───────────────────────────────────────────────────────────────

    @Test
    void replace_applies_type_override_and_redirects() throws Exception {
        Debt sessionDebt = validDebt("Laptop", "hash-lap");
        sessionDebt.setDebtType(DebtTypeEnum.CARD);

        when(sourceOfTruthImportUseCase.replaceAllWithStatement(anyList(), eq(ACC))).thenReturn(List.of());

        mockMvc.perform(post("/ui/statements/" + ACC + "/replace")
                        .param("typeOverrides[0].hashSum", "hash-lap")
                        .param("typeOverrides[0].debtType", "PEOPLE")
                        .sessionAttr("userEmail", "user@test.com")
                        .sessionAttr("extractedDebts", new ArrayList<>(List.of(sessionDebt))))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/ui/debt-accounts/" + ACC));

        assertThat(sessionDebt.getDebtType()).isEqualTo(DebtTypeEnum.PEOPLE);
        verify(sourceOfTruthImportUseCase).replaceAllWithStatement(anyList(), eq(ACC));
    }

    @Test
    void replace_redirects_back_to_upload_when_no_session_debts() throws Exception {
        mockMvc.perform(post("/ui/statements/" + ACC + "/replace")
                        .sessionAttr("userEmail", "user@test.com"))
                .andExpect(redirectedUrl("/ui/statements/" + ACC));
    }

    // ── type override — does not affect debt with different hash ──────────────

    @Test
    void sync_override_does_not_affect_debt_with_different_hash() throws Exception {
        Debt sessionDebt = validDebt("Laptop", "hash-laptop");
        sessionDebt.setDebtType(DebtTypeEnum.CARD);

        when(loadDebtList.saveUnrepeated(anyList(), eq(ACC))).thenReturn(List.of());

        mockMvc.perform(post("/ui/statements/" + ACC + "/sync")
                        .param("typeOverrides[0].hashSum", "hash-DIFFERENT")
                        .param("typeOverrides[0].debtType", "LOAN")
                        .sessionAttr("userEmail", "user@test.com")
                        .sessionAttr("extractedDebts", new ArrayList<>(List.of(sessionDebt))))
                .andExpect(redirectedUrl("/ui/debt-accounts/" + ACC));

        // Type must remain CARD — override targeted a different hash
        assertThat(sessionDebt.getDebtType()).isEqualTo(DebtTypeEnum.CARD);
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private Debt validDebt(String description, String hash) {
        Debt d = new Debt();
        d.setDescription(description);
        d.setMonthlyPayment(new BigDecimal("500.00"));
        d.setMaxFinancingTerm(6);
        d.setCurrentInstallment(1);
        d.setDebtType(DebtTypeEnum.CARD);
        d.setActive(true);
        d.setHashSum(hash);
        return d;
    }
}
