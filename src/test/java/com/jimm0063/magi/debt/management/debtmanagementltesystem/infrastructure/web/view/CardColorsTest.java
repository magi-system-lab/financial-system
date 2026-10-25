package com.jimm0063.magi.debt.management.debtmanagementltesystem.infrastructure.web.view;

import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.enums.AccountStatementType;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.model.DebtAccount;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CardColorsTest {

    private final CardColors colors = new CardColors();

    private DebtAccount account(String code, AccountStatementType type, String color) {
        DebtAccount a = new DebtAccount();
        a.setCode(code);
        a.setAccountStatementType(type);
        a.setColor(color);
        return a;
    }

    @Test
    void chosen_colour_wins() {
        assertThat(colors.of(account("x", AccountStatementType.BBVA, "#0F766E"))).isEqualTo("#0F766E");
    }

    @Test
    void bank_default_when_none_chosen_or_invalid() {
        assertThat(colors.of(account("x", AccountStatementType.LIVERPOOL, null))).isEqualTo("#A3166F");
        assertThat(colors.of(account("x", AccountStatementType.RAPPI, "red; background:url(x)"))).isEqualTo("#C2331E");
    }

    @Test
    void other_banks_get_a_stable_palette_colour() {
        String first = colors.of(account("card-1", AccountStatementType.UNIVERSAL, null));
        assertThat(CardColors.PALETTE).contains(first);
        assertThat(colors.of(account("card-1", AccountStatementType.MANUAL, null))).isEqualTo(first);
    }

    @Test
    void only_hex_colours_are_valid() {
        assertThat(CardColors.isValid("#a3166f")).isTrue();
        assertThat(CardColors.isValid("auto")).isFalse();
        assertThat(CardColors.isValid("#12345")).isFalse();
        assertThat(CardColors.isValid(null)).isFalse();
    }
}
