-- BaoJingTong DDL (H2, MODE=MySQL)
-- 金额一律 BIGINT，单位：分
-- 使用 IF NOT EXISTS，应用每次启动可安全重放

CREATE TABLE IF NOT EXISTS customer (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  customer_no VARCHAR(64) NOT NULL,
  name VARCHAR(64) NOT NULL,
  id_card VARCHAR(32),
  phone VARCHAR(32),
  gender VARCHAR(8),
  age INT NOT NULL DEFAULT 30,
  occupation VARCHAR(64),
  risk_level INT NOT NULL DEFAULT 1,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT uk_customer_no UNIQUE (customer_no)
);

CREATE TABLE IF NOT EXISTS insurance_product (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  product_code VARCHAR(64) NOT NULL,
  product_name VARCHAR(128) NOT NULL,
  product_type VARCHAR(32) NOT NULL,
  insurer VARCHAR(128) NOT NULL,
  coverage_cent BIGINT NOT NULL DEFAULT 0,
  premium_cent BIGINT NOT NULL DEFAULT 0,
  commission_rate DECIMAL(10, 6) NOT NULL DEFAULT 0,
  risk_level INT NOT NULL DEFAULT 1,
  max_age INT NOT NULL DEFAULT 65,
  term_years INT NOT NULL DEFAULT 1,
  status VARCHAR(16) NOT NULL DEFAULT 'ON_SALE',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT uk_product_code UNIQUE (product_code)
);

CREATE TABLE IF NOT EXISTS policy (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  policy_no VARCHAR(64) NOT NULL,
  customer_id BIGINT NOT NULL,
  product_id BIGINT NOT NULL,
  product_name VARCHAR(128) NOT NULL,
  insured_name VARCHAR(64) NOT NULL,
  insured_age INT NOT NULL,
  coverage_cent BIGINT NOT NULL,
  premium_cent BIGINT NOT NULL,
  commission_cent BIGINT NOT NULL DEFAULT 0,
  status VARCHAR(24) NOT NULL,
  underwriting_result VARCHAR(24),
  underwriting_reason VARCHAR(255),
  effective_date DATE,
  expire_date DATE,
  idempotency_key VARCHAR(128) NOT NULL,
  version BIGINT NOT NULL DEFAULT 0,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT uk_policy_no UNIQUE (policy_no),
  CONSTRAINT uk_policy_idem UNIQUE (idempotency_key)
);

CREATE TABLE IF NOT EXISTS claim (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  claim_no VARCHAR(64) NOT NULL,
  policy_no VARCHAR(64) NOT NULL,
  customer_id BIGINT NOT NULL,
  accident_type VARCHAR(64) NOT NULL,
  claim_cent BIGINT NOT NULL,
  approved_cent BIGINT NOT NULL DEFAULT 0,
  status VARCHAR(24) NOT NULL,
  review_remark VARCHAR(255),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT uk_claim_no UNIQUE (claim_no)
);

CREATE TABLE IF NOT EXISTS commission_record (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  commission_no VARCHAR(64) NOT NULL,
  policy_no VARCHAR(64) NOT NULL,
  premium_cent BIGINT NOT NULL,
  commission_rate DECIMAL(10, 6) NOT NULL,
  commission_cent BIGINT NOT NULL,
  status VARCHAR(24) NOT NULL DEFAULT 'ACCRUED',
  settled_at TIMESTAMP,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT uk_commission_no UNIQUE (commission_no)
);

CREATE TABLE IF NOT EXISTS account (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  account_no VARCHAR(64) NOT NULL,
  account_name VARCHAR(128) NOT NULL,
  account_type VARCHAR(32) NOT NULL,
  balance_cent BIGINT NOT NULL DEFAULT 0,
  opening_balance_cent BIGINT NOT NULL DEFAULT 0,
  version BIGINT NOT NULL DEFAULT 0,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT uk_account_no UNIQUE (account_no)
);

CREATE TABLE IF NOT EXISTS account_entry (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  entry_no VARCHAR(64) NOT NULL,
  account_no VARCHAR(64) NOT NULL,
  direction VARCHAR(4) NOT NULL,
  amount_cent BIGINT NOT NULL,
  biz_no VARCHAR(64) NOT NULL,
  biz_type VARCHAR(32) NOT NULL,
  before_balance_cent BIGINT NOT NULL,
  after_balance_cent BIGINT NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT uk_entry_no UNIQUE (entry_no)
);

CREATE TABLE IF NOT EXISTS event_log (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  topic VARCHAR(32) NOT NULL,
  biz_no VARCHAR(64),
  title VARCHAR(128) NOT NULL,
  status VARCHAR(16) NOT NULL DEFAULT 'OK',
  detail VARCHAR(1024),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_policy_customer ON policy(customer_id);
CREATE INDEX IF NOT EXISTS idx_claim_policy ON claim(policy_no);
CREATE INDEX IF NOT EXISTS idx_commission_policy ON commission_record(policy_no);
CREATE INDEX IF NOT EXISTS idx_entry_biz ON account_entry(biz_no);
