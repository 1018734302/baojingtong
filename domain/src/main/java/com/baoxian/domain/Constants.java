package com.baoxian.domain;

/** 领域常量：状态值、账户编号、科目性质、事件主题 */
public final class Constants {

    private Constants() {
    }

    /* ===== 保单状态 ===== */
    public static final String POLICY_UNDERWRITING = "UNDERWRITING";
    public static final String POLICY_ISSUED = "ISSUED";
    public static final String POLICY_ACTIVE = "ACTIVE";
    public static final String POLICY_SURRENDERED = "SURRENDERED";
    public static final String POLICY_EXPIRED = "EXPIRED";

    /* ===== 理赔状态 ===== */
    public static final String CLAIM_SUBMITTED = "SUBMITTED";
    public static final String CLAIM_REVIEWING = "REVIEWING";
    public static final String CLAIM_APPROVED = "APPROVED";
    public static final String CLAIM_REJECTED = "REJECTED";
    public static final String CLAIM_PAID = "PAID";

    /* ===== 佣金状态 ===== */
    public static final String COMMISSION_ACCRUED = "ACCRUED";
    public static final String COMMISSION_SETTLED = "SETTLED";

    /* ===== 核保结果 ===== */
    public static final String UW_STANDARD = "STANDARD";
    public static final String UW_RATED = "RATED";
    public static final String UW_REJECTED = "REJECTED";

    /* ===== 账户编号 ===== */
    public static final String ACC_BANK = "ACC_BANK";
    public static final String ACC_PREMIUM = "ACC_PREMIUM";
    public static final String ACC_COMMISSION_PAYABLE = "ACC_COMMISSION_PAYABLE";
    public static final String ACC_COMMISSION_EXPENSE = "ACC_COMMISSION_EXPENSE";
    public static final String ACC_CLAIM_EXPENSE = "ACC_CLAIM_EXPENSE";

    /* ===== 科目性质 ===== */
    public static final String NATURE_ASSET = "ASSET";
    public static final String NATURE_LIABILITY = "LIABILITY";
    public static final String NATURE_INCOME = "INCOME";
    public static final String NATURE_EXPENSE = "EXPENSE";

    /* ===== 借贷方向 ===== */
    public static final String DR = "DR";
    public static final String CR = "CR";

    /* ===== 事件主题 ===== */
    public static final String TOPIC_CUSTOMER = "CUSTOMER";
    public static final String TOPIC_POLICY = "POLICY";
    public static final String TOPIC_PRODUCT = "PRODUCT";
    public static final String TOPIC_CLAIM = "CLAIM";
    public static final String TOPIC_COMMISSION = "COMMISSION";
    public static final String TOPIC_LEDGER = "LEDGER";
    public static final String TOPIC_SYSTEM = "SYSTEM";
}
