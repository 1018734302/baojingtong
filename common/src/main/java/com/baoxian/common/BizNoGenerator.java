package com.baoxian.common;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicLong;

/** 业务单号生成器：前缀 + 时间戳 + 自增序号 */
public final class BizNoGenerator {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final AtomicLong SEQ = new AtomicLong(0);

    private BizNoGenerator() {
    }

    public static String gen(String prefix) {
        long seq = SEQ.incrementAndGet();
        return prefix + LocalDateTime.now().format(FMT) + String.format("%04d", seq % 10000);
    }
}
