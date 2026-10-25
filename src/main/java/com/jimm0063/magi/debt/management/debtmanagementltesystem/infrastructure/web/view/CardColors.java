package com.jimm0063.magi.debt.management.debtmanagementltesystem.infrastructure.web.view;

import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.enums.AccountStatementType;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.model.DebtAccount;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Card colours for the Wallet-style UI. A card uses its own colour when the user picked one, otherwise
 * a default from its bank (statement type), otherwise a stable pick from the palette. Every colour keeps
 * white text readable (≥ 4.5:1). Used in templates as {@code ${@cardColors.of(account)}}.
 */
@Component("cardColors")
public class CardColors {

    public static final List<String> PALETTE = List.of(
            "#0B3D91", "#086A9C", "#0F766E", "#2F6B2F", "#8A4B08",
            "#C2331E", "#A3166F", "#6D28D9", "#3A3A3C", "#1F2937");

    private static final Pattern HEX = Pattern.compile("^#[0-9A-Fa-f]{6}$");

    private static final Map<AccountStatementType, String> BY_BANK = Map.of(
            AccountStatementType.BBVA, "#0B3D91",
            AccountStatementType.RAPPI, "#C2331E",
            AccountStatementType.LIVERPOOL, "#A3166F",
            AccountStatementType.PALACIO, "#3A3A3C",
            AccountStatementType.MERCADO_PAGO, "#086A9C");

    public String of(DebtAccount account) {
        if (account == null) return PALETTE.get(0);
        if (isValid(account.getColor())) return account.getColor();
        String bank = BY_BANK.get(account.getAccountStatementType());
        if (bank != null) return bank;
        String key = account.getCode() != null ? account.getCode() : "";
        return PALETTE.get(Math.floorMod(key.hashCode(), PALETTE.size()));
    }

    public List<String> palette() {
        return PALETTE;
    }

    public static boolean isValid(String color) {
        return color != null && HEX.matcher(color).matches();
    }
}
