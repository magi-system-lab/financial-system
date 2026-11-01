package com.jimm0063.magi.debt.management.debtmanagementltesystem.infrastructure.web.view;

import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.dto.CoachTipDto;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.enums.CoachTipType;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import java.util.Locale;

/**
 * A coach tip ready for the templates: message keys {@code coach.tip.<TYPE>.title} / {@code coach.tip.<TYPE>},
 * arguments already formatted (money, months) and the page that helps act on it.
 */
public record CoachTipView(String type, String severity, Object[] args, String link) {

    public String titleKey() { return "coach.tip." + type + ".title"; }
    public String bodyKey() { return "coach.tip." + type; }

    /** iOS row icon colour per severity. */
    public String iconClass() {
        return switch (severity) {
            case "ALERT" -> "bg-pink";
            case "WARN" -> "bg-orange";
            case "GOOD" -> "bg-green";
            default -> "bg-blue";
        };
    }

    public String icon() {
        return switch (severity) {
            case "ALERT", "WARN" -> "alert";
            case "GOOD" -> "check";
            default -> "bulb";
        };
    }

    public static List<CoachTipView> from(List<CoachTipDto> tips, UiFormat fmt, Locale locale) {
        return tips.stream().map(t -> new CoachTipView(t.type().name(), t.severity().name(),
                t.args().stream().map(a -> format(a, fmt, locale)).toArray(), link(t.type()))).toList();
    }

    private static Object format(Object arg, UiFormat fmt, Locale locale) {
        if (arg instanceof BigDecimal money) return fmt.money0(money);
        if (arg instanceof YearMonth month) return fmt.monthYear(month, locale);
        if (arg instanceof Double d) return String.format(Locale.ROOT, "%.1f", d);
        return arg;   // whole numbers stay numbers so {0,choice,...} plurals work
    }

    static String link(CoachTipType type) {
        return switch (type) {
            case NO_SALARY, OVERSPENT, OVER_LIMIT, SAFETY_LOW -> "/ui/insights";
            case INTEREST_FIRST, QUICK_WIN, SNOWBALL_ONLY -> "/ui/coach#plan";
            case GOAL_READY_PAYOFF, GOAL_REACHED, GOAL_BEHIND, GOAL_DEBT_GONE, FREED_THIS_MONTH, RELIEF_NEXT, NO_GOALS -> "/ui/coach#goals";
            case NEW_OUTPACING, WISH_READY -> "/ui/upcoming-debts#cooldown";
            case UPCOMING_NEXT -> "/ui/upcoming-debts";
            case DEBT_FREE -> null;
        };
    }
}
