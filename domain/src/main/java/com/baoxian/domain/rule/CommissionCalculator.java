package com.baoxian.domain.rule;

import com.baoxian.common.MoneyUtils;

import java.math.BigDecimal;

/** 佣金计算：佣金 = 保费 × 佣金率，单位分，四舍五入 */
public final class CommissionCalculator {

    private CommissionCalculator() {
    }

    public static long calc(long premiumCent, BigDecimal commissionRate) {
        if (premiumCent <= 0) {
            return 0L;
        }
        BigDecimal rate = commissionRate == null ? BigDecimal.ZERO : commissionRate;
        return MoneyUtils.multiplyRate(premiumCent, rate);
    }
}
