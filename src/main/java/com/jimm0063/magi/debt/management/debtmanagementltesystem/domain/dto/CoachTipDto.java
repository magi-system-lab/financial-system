package com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.dto;

import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.enums.CoachTipSeverity;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.enums.CoachTipType;

import java.io.Serializable;
import java.util.List;

/** One piece of advice; the text comes from the UI's messages for {@code type}, filled with {@code args}. */
public record CoachTipDto(CoachTipType type, CoachTipSeverity severity, List<Object> args) implements Serializable {
    public static CoachTipDto of(CoachTipType type, CoachTipSeverity severity, Object... args) {
        return new CoachTipDto(type, severity, List.of(args));
    }
}
