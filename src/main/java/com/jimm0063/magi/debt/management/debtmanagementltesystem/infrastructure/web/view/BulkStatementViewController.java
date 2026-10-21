package com.jimm0063.magi.debt.management.debtmanagementltesystem.infrastructure.web.view;

import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.application.port.in.FindAllDebtAccountUseCase;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.application.port.in.FindAllFinancialProviderUseCase;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.application.port.in.FindAllUpcomingDebtsUseCase;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.application.port.in.MarkDebtAsIgnorableUseCase;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.application.port.in.PreviewBulkStatementUseCase;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.application.port.in.SyncBulkStatementUseCase;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.enums.BulkItemAction;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.model.BulkStatementBatch;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.model.BulkStatementItem;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.model.BulkStatementUpload;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.model.BulkSyncResult;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.model.DebtAccount;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.model.FinancialProvider;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.model.UpcomingDebt;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.infrastructure.model.BulkStatementUploadForm;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Bulk statement import: upload many statements, associate each with a card, review the
 * per-card diff (choosing update / rebuild-from-0 / skip per card) and persist everything at once.
 */
@Controller
@RequestMapping("/ui/bulk-statements")
public class BulkStatementViewController {

    static final String BATCH_KEY = "bulkStatementBatch";
    private static final String BASE = "/ui/bulk-statements";

    private final PreviewBulkStatementUseCase previewUseCase;
    private final SyncBulkStatementUseCase syncUseCase;
    private final MarkDebtAsIgnorableUseCase markIgnorableUseCase;
    private final FindAllFinancialProviderUseCase findAllFinancialProviderUseCase;
    private final FindAllDebtAccountUseCase findAllDebtAccountUseCase;
    private final ActivityLogHelper activityLogHelper;
    private final FindAllUpcomingDebtsUseCase findAllUpcomingDebtsUseCase;

    public BulkStatementViewController(
            PreviewBulkStatementUseCase previewUseCase,
            SyncBulkStatementUseCase syncUseCase,
            MarkDebtAsIgnorableUseCase markIgnorableUseCase,
            FindAllFinancialProviderUseCase findAllFinancialProviderUseCase,
            FindAllDebtAccountUseCase findAllDebtAccountUseCase,
            ActivityLogHelper activityLogHelper,
            FindAllUpcomingDebtsUseCase findAllUpcomingDebtsUseCase) {
        this.previewUseCase = previewUseCase;
        this.syncUseCase = syncUseCase;
        this.markIgnorableUseCase = markIgnorableUseCase;
        this.findAllFinancialProviderUseCase = findAllFinancialProviderUseCase;
        this.findAllDebtAccountUseCase = findAllDebtAccountUseCase;
        this.activityLogHelper = activityLogHelper;
        this.findAllUpcomingDebtsUseCase = findAllUpcomingDebtsUseCase;
    }

    /** Step 1 — pick the files and associate each one with a card. */
    @GetMapping
    public String uploadForm(HttpSession session, Model model) {
        String email = (String) session.getAttribute("userEmail");
        if (email == null) return "redirect:/ui";

        Map<String, List<DebtAccount>> accountsByProvider = new LinkedHashMap<>();
        for (FinancialProvider provider : findAllFinancialProviderUseCase.getActiveByEmail(email)) {
            List<DebtAccount> accounts = findAllDebtAccountUseCase.getActiveByFinancialProvider(provider.getCode());
            if (!accounts.isEmpty()) accountsByProvider.put(provider.getName(), accounts);
        }
        model.addAttribute("accountsByProvider", accountsByProvider);
        model.addAttribute("hasPendingBatch", session.getAttribute(BATCH_KEY) instanceof BulkStatementBatch);
        model.addAttribute("email", email);
        return "bulk-statements/upload";
    }

    /** Step 2 — extract every file and diff it against its card. Nothing is persisted. */
    @PostMapping("/extract")
    public String extract(@ModelAttribute BulkStatementUploadForm form,
                          HttpSession session,
                          RedirectAttributes redirectAttributes) {
        if (session.getAttribute("userEmail") == null) return "redirect:/ui";

        List<BulkStatementUpload> uploads = form.getRows().stream()
                .filter(r -> r != null && r.getFile() != null && !r.getFile().isEmpty())
                .map(r -> new BulkStatementUpload(r.getFile(), r.getDebtAccountCode(), r.getPdfPassword()))
                .toList();
        if (uploads.isEmpty()) {
            redirectAttributes.addFlashAttribute("uploadError", "Select at least one statement file.");
            return "redirect:" + BASE;
        }

        BulkItemAction defaultAction = form.getDefaultAction() == null ? BulkItemAction.UPDATE : form.getDefaultAction();
        BulkStatementBatch batch = previewUseCase.preview(uploads, defaultAction);
        session.setAttribute(BATCH_KEY, batch);
        activityLogHelper.log(session, "Bulk statement preview — " + uploads.size() + " file(s)", summary(batch));
        return "redirect:" + BASE + "/preview";
    }

    /** Middle view — per-card diff with the action that will be applied to each card. */
    @GetMapping("/preview")
    public String preview(HttpSession session, Model model) {
        String email = (String) session.getAttribute("userEmail");
        if (email == null) return "redirect:/ui";
        BulkStatementBatch batch = batch(session);
        if (batch == null) return "redirect:" + BASE;
        model.addAttribute("batch", batch);
        model.addAttribute("email", email);
        // Purchases still on the upcoming pile, per card, so the user can recognise them in each statement
        Map<String, List<UpcomingDebt>> upcomingByAccount = new LinkedHashMap<>();
        for (BulkStatementItem item : batch.items()) {
            String code = item.getDebtAccountCode();
            if (code != null && !code.isBlank() && !upcomingByAccount.containsKey(code))
                upcomingByAccount.put(code, findAllUpcomingDebtsUseCase.getPendingByDebtAccount(code));
        }
        model.addAttribute("upcomingByAccount", upcomingByAccount);
        return "bulk-statements/preview";
    }

    /** Change what will be done with one card (update / rebuild from 0 / skip). */
    @PostMapping("/items/{index}/action")
    public String setAction(@PathVariable int index, @RequestParam BulkItemAction action, HttpSession session) {
        if (session.getAttribute("userEmail") == null) return "redirect:/ui";
        BulkStatementBatch batch = batch(session);
        if (batch == null) return "redirect:" + BASE;
        BulkStatementItem item = item(batch, index);
        if (item != null && !item.hasError()) {
            item.setAction(action);
            session.setAttribute(BATCH_KEY, batch);
        }
        return "redirect:" + BASE + "/preview#item-" + index;
    }

    /** Apply the same action to every card that was extracted successfully. */
    @PostMapping("/action-all")
    public String setActionForAll(@RequestParam BulkItemAction action, HttpSession session) {
        if (session.getAttribute("userEmail") == null) return "redirect:/ui";
        BulkStatementBatch batch = batch(session);
        if (batch == null) return "redirect:" + BASE;
        batch.items().stream().filter(i -> !i.hasError()).forEach(i -> i.setAction(action));
        session.setAttribute(BATCH_KEY, batch);
        return "redirect:" + BASE + "/preview";
    }

    /** Mark a debt as ignorable and recompute only the affected card's diff. */
    @PostMapping("/items/{index}/mark-ignorable")
    public String markIgnorable(@PathVariable int index,
                                @RequestParam String hashSum,
                                @RequestParam String reason,
                                HttpSession session) {
        if (session.getAttribute("userEmail") == null) return "redirect:/ui";
        BulkStatementBatch batch = batch(session);
        if (batch == null) return "redirect:" + BASE;
        BulkStatementItem item = item(batch, index);
        if (item != null && item.getDiff() != null) {
            markIgnorableUseCase.markAsIgnorable(hashSum, reason);
            previewUseCase.recompute(item);
            session.setAttribute(BATCH_KEY, batch);
            activityLogHelper.log(session, "Mark ignorable — " + item.getDebtAccountCode(),
                    "hash=" + hashSum + ", reason=" + reason);
        }
        return "redirect:" + BASE + "/preview#item-" + index;
    }

    /** Re-upload the file of a card that failed (e.g. with the PDF password this time). */
    @PostMapping("/items/{index}/retry")
    public String retry(@PathVariable int index,
                        @RequestParam("file") MultipartFile file,
                        @RequestParam(required = false) String pdfPassword,
                        HttpSession session) {
        if (session.getAttribute("userEmail") == null) return "redirect:/ui";
        BulkStatementBatch batch = batch(session);
        if (batch == null) return "redirect:" + BASE;
        BulkStatementItem item = item(batch, index);
        if (item != null && item.isRetryable()) {
            previewUseCase.retry(item, file, pdfPassword);
            session.setAttribute(BATCH_KEY, batch);
            activityLogHelper.log(session, "Bulk statement retry — " + item.getDebtAccountCode(),
                    item.hasError() ? item.getError() : "OK");
        }
        return "redirect:" + BASE + "/preview#item-" + index;
    }

    /** Step 3 — persist every card with its chosen action (single transaction). */
    @PostMapping("/execute")
    public String execute(HttpSession session, RedirectAttributes redirectAttributes) {
        if (session.getAttribute("userEmail") == null) return "redirect:/ui";
        BulkStatementBatch batch = batch(session);
        if (batch == null) return "redirect:" + BASE;
        try {
            BulkSyncResult result = syncUseCase.sync(batch);
            activityLogHelper.log(session, "Bulk statement sync", result);
            session.removeAttribute(BATCH_KEY);
            redirectAttributes.addFlashAttribute("result", result);
            return "redirect:" + BASE + "/result";
        } catch (Exception e) {
            Map<String, String> info = new LinkedHashMap<>();
            info.put("exception", e.getClass().getName());
            info.put("message", e.getMessage());
            if (e.getCause() != null) info.put("cause", e.getCause().getClass().getName() + ": " + e.getCause().getMessage());
            activityLogHelper.log(session, "Bulk statement sync ERROR", info);
            String msg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            if (e.getCause() != null && e.getCause().getMessage() != null) msg += " → " + e.getCause().getMessage();
            redirectAttributes.addFlashAttribute("syncError", msg);
            return "redirect:" + BASE + "/preview";
        }
    }

    @GetMapping("/result")
    public String result(HttpSession session, Model model) {
        String email = (String) session.getAttribute("userEmail");
        if (email == null) return "redirect:/ui";
        if (!(model.getAttribute("result") instanceof BulkSyncResult)) return "redirect:" + BASE;
        model.addAttribute("email", email);
        return "bulk-statements/result";
    }

    /** Discard the pending batch without persisting anything. */
    @PostMapping("/cancel")
    public String cancel(HttpSession session) {
        if (session.getAttribute("userEmail") == null) return "redirect:/ui";
        session.removeAttribute(BATCH_KEY);
        return "redirect:" + BASE;
    }

    // -------------------------------------------------------------------------

    private static BulkStatementBatch batch(HttpSession session) {
        return session.getAttribute(BATCH_KEY) instanceof BulkStatementBatch b ? b : null;
    }

    private static BulkStatementItem item(BulkStatementBatch batch, int index) {
        return index >= 0 && index < batch.items().size() ? batch.items().get(index) : null;
    }

    private static Map<String, Object> summary(BulkStatementBatch batch) {
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("files", batch.items().size());
        info.put("failed", batch.failedCount());
        info.put("new", batch.totalNew());
        info.put("updated", batch.totalUpdated());
        info.put("completed", batch.totalCompleted());
        batch.items().forEach(i -> info.put(i.getFileName(),
                i.hasError() ? "ERROR: " + i.getError() : i.getDebtAccountCode() + " [" + i.getAction() + "]"));
        return info;
    }
}
