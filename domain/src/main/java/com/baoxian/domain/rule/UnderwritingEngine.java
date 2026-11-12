package com.baoxian.domain.rule;

import com.baoxian.domain.Constants;

import java.math.BigDecimal;

/**
 * 核保规则引擎（纯 Java，零框架依赖）：
 * 1. 年龄越界 / 超保额 → 拒保
 * 2. 年龄偏大或产品风险等级高 → 加费承保（保费上浮 20%）
 * 3. 其余 → 标准体承保
 */
public final class UnderwritingEngine {

    private static final BigDecimal LOADING_RATE = new BigDecimal("1.20");

    private UnderwritingEngine() {
    }

    public static Decision decide(int age, long coverageCent, int productRiskLevel,
                                  int productMaxAge, long productMaxCoverageCent) {
        if (age < 18) {
            return new Decision(Constants.UW_REJECTED, "投保年龄低于 18 周岁", BigDecimal.ONE);
        }
        if (age > productMaxAge) {
            return new Decision(Constants.UW_REJECTED, "投保年龄超过产品最高承保年龄 " + productMaxAge + " 周岁", BigDecimal.ONE);
        }
        if (coverageCent > productMaxCoverageCent) {
            return new Decision(Constants.UW_REJECTED, "申请保额超过产品保额上限", BigDecimal.ONE);
        }
        if (age >= 55 || productRiskLevel >= 4) {
            return new Decision(Constants.UW_RATED, "年龄偏大或产品风险等级较高，加费承保 20%", LOADING_RATE);
        }
        return new Decision(Constants.UW_STANDARD, "标准体承保", BigDecimal.ONE);
    }

    /** 核保结论 */
    public static final class Decision {
        private final String result;
        private final String reason;
        private final BigDecimal loadingRate;

        public Decision(String result, String reason, BigDecimal loadingRate) {
            this.result = result;
            this.reason = reason;
            this.loadingRate = loadingRate;
        }

        public String getResult() {
            return result;
        }

        public String getReason() {
            return reason;
        }

        public BigDecimal getLoadingRate() {
            return loadingRate;
        }

        public boolean isRejected() {
            return Constants.UW_REJECTED.equals(result);
        }
    }
}
