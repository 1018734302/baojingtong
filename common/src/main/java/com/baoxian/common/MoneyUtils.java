package com.baoxian.common;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** 金额工具：内部一律以「分」为单位的 long 存储，杜绝浮点误差 */
public final class MoneyUtils {

    private MoneyUtils() {
    }

    /** 分 → 元字符串，保留两位 */
    public static String toYuan(long cent) {
        return BigDecimal.valueOf(cent, 2).toPlainString();
    }

    /** 元（数字或字符串）→ 分 */
    public static long toCent(Object yuan) {
        if (yuan == null || String.valueOf(yuan).isBlank()) {
            return 0L;
        }
        BigDecimal b = new BigDecimal(String.valueOf(yuan).trim());
        return b.multiply(BigDecimal.valueOf(100)).setScale(0, RoundingMode.HALF_UP).longValueExact();
    }

    /** 按费率计算金额（分），四舍五入 */
    public static long multiplyRate(long cent, BigDecimal rate) {
        return BigDecimal.valueOf(cent)
                .multiply(rate)
                .setScale(0, RoundingMode.HALF_UP)
                .longValueExact();
    }
}
