package com.jimm0063.magi.debt.management.debtmanagementltesystem.infrastructure.web.view;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Money formatting for the redesigned templates, used as {@code ${@fmt.money(x)}}.
 * Always "$1,234.56" (MXN and USD share the format), whatever the UI language.
 */
@Component("fmt")
public class UiFormat {

    private static final DecimalFormatSymbols SYMBOLS = DecimalFormatSymbols.getInstance(Locale.US);

    public String money(Number value) {
        return format(value, "$#,##0.00");
    }

    /** No cents: "$1,235". */
    public String money0(Number value) {
        return format(value, "$#,##0");
    }

    /** Whole part of {@link #money}: "$1,234" (the big number on Insights). */
    public String whole(Number value) {
        String full = money(value);
        return full.substring(0, full.length() - 3);
    }

    /** Cents part of {@link #money}: ".56". */
    public String cents(Number value) {
        String full = money(value);
        return full.substring(full.length() - 3);
    }

    /** Signed difference without cents: "+$1,200" / "−$1,200". */
    public String signed0(Number value) {
        BigDecimal v = toBigDecimal(value);
        return (v.signum() < 0 ? "−" : "+") + money0(v.abs());
    }

    /** "November 2026" / "Noviembre 2026". */
    public String month(YearMonth month, Locale locale) {
        return capitalize(month.format(DateTimeFormatter.ofPattern("MMMM yyyy", locale)), locale);
    }

    /** "Nov 2026" / "Nov 2026" (Spanish abbreviations lose their trailing dot). */
    public String monthYear(YearMonth month, Locale locale) {
        return monthShort(month, locale) + " " + month.getYear();
    }

    /** "Nov" / "Nov". */
    public String monthShort(YearMonth month, Locale locale) {
        String s = month.format(DateTimeFormatter.ofPattern("MMM", locale)).replace(".", "");
        return capitalize(s, locale);
    }

    /** "N" — chart axis label. */
    public String monthInitial(YearMonth month, Locale locale) {
        return monthShort(month, locale).substring(0, 1);
    }

    private static String capitalize(String s, Locale locale) {
        return s.isEmpty() ? s : s.substring(0, 1).toUpperCase(locale) + s.substring(1);
    }

    private static String format(Number value, String pattern) {
        return new DecimalFormat(pattern, SYMBOLS).format(toBigDecimal(value).setScale(2, RoundingMode.HALF_UP));
    }

    private static BigDecimal toBigDecimal(Number value) {
        if (value == null) return BigDecimal.ZERO;
        if (value instanceof BigDecimal bd) return bd;
        return new BigDecimal(value.toString());
    }
}
