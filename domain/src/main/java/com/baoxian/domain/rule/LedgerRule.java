package com.baoxian.domain.rule;

import com.baoxian.domain.Constants;

/**
 * 复式记账规则（按科目性质区分借贷方向）：
 * balanceDelta = sign(科目性质) × (DR ? +amount : -amount)
 * sign(ASSET)=+1  sign(LIABILITY)=-1  sign(INCOME)=-1  sign(EXPENSE)=+1
 */
public final class LedgerRule {

    private LedgerRule() {
    }

    public static int sign(String nature) {
        return switch (nature) {
            case Constants.NATURE_ASSET, Constants.NATURE_EXPENSE -> 1;
            case Constants.NATURE_LIABILITY, Constants.NATURE_INCOME -> -1;
            default -> throw new IllegalArgumentException("未知科目性质: " + nature);
        };
    }

    public static long balanceDelta(String nature, String direction, long amountCent) {
        int sign = sign(nature);
        return Constants.DR.equals(direction) ? sign * amountCent : -sign * amountCent;
    }
}
