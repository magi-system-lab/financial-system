package com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.application.port.in;

import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.enums.FixedExpenseKind;
import com.jimm0063.magi.debt.management.debtmanagementltesystem.domain.model.FixedExpense;

import java.util.List;

/** Moves monthly items between sections: fixed expenses ↔ credits, and a card debt into one of them. */
public interface MoveBetweenSectionsUseCase {

    /** Fixed expenses → credits or back; amount, day and category stay the same. */
    List<FixedExpense> moveFixedExpenses(List<Integer> ids, FixedExpenseKind to);

    /**
     * Turns a debt into a flat monthly item (credit or fixed expense): its monthly payment becomes the monthly cost, the
     * card's pay day the payment day, and the debt is soft-deleted so it stops counting as a card payment.
     */
    FixedExpense moveDebt(String userEmail, Integer debtId, FixedExpenseKind to, Integer catalogId);
}
