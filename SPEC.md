# 保经通 BaoJingTong — 保险经纪核心业务系统 · 技术规格说明书

| 项 | 内容 |
|---|---|
| 版本 | v1.0 |
| 日期 | 2026-11-12 |
| 技术栈 | JDK 17 / Spring Boot 3.2.5 / MyBatis-Plus 3.5.7 / H2 |
| 状态 | 已实现，端到端自检 10 项全部通过 |
| 目标读者 | 项目所有者（求职作品集项目） |

> 本文档定义"做什么、为什么这么做、做到什么程度"。所有描述与 `baoxian` 工程实际代码一致；未实现的项在 §10 如实标注。

---

## 1. 项目定位与目标

### 1.1 一句话定位

模拟保险经纪机构的**产品配置 → 投保出单 → 核保 → 承保 → 理赔 → 佣金计提与结算**全链路，核心不是 CRUD 功能数量，而是**保险业务独有的领域复杂度**：保单生命周期、核保规则、佣金与保费的资金一致性。

- 业务壳：**保险经纪核心业务系统**（面向 ToB 的经纪/代理机构，而非面向 C 端的保险商城）
- 简历定位：**保险业务的领域建模 + 规则引擎 + 资金闭环**
- 与前序项目的关系：
  - `CloudExplorer`：交付视角（资源协调、客户沟通、留痕）
  - `UniHub`：产品视角（业务抽象、模型统一、架构权衡）
  - `LedgerCore`（jinrong）：领域深度（金融结算、借贷平衡、幂等）
  - **`BaoJingTong`（本项目）：把前两者的"业务抽象"落到保险行业，并复用金融结算的"记账内核"**

### 1.2 业务角色

| 角色 | 说明 |
|---|---|
| 经纪人 / 内勤 | 维护客户与产品，发起投保，跟进理赔 |
| 投保人 | 购买保险，是被保人与保费支付主体 |
| 核保引擎 | 依据年龄、保额、产品风险等级给出承保结论 |
| 保险人（保险公司） | 提供产品与条款，本项目为 Mock 产品库 |
| 理赔审核员 | 对报案做审核（通过 / 拒赔），触发赔付 |
| 财务 | 佣金计提与结算，查看科目账户余额 |

### 1.3 端到端链路

```
新建客户/产品
→ 投保申请（保额、被保人年龄）
→ 核保规则判定（标准承保 / 加费承保 / 拒保）
→ 承保出单（生成保单，状态 ACTIVE）
→ 保费记账（借 银行存款 / 贷 保费收入）
→ 佣金计提（借 佣金费用 / 贷 应付佣金，状态 ACCRUED）
→ 理赔报案（状态 SUBMITTED）
→ 理赔审核（APPROVED / REJECTED）
→ 理赔赔付（借 赔付支出 / 贷 银行存款，状态 PAID）
→ 佣金结算（借 应付佣金 / 贷 银行存款，状态 SETTLED）
→ 看板校验借贷平衡
```

### 1.4 明确的非目标（Out of Scope）

- 不接真实保险公司核心系统、不接银保监/中保信真实报送通道
- 不做 C 端保险商城、比价、在线支付
- 不做多租户 SaaS、组织架构与数据权限
- 不引入工作流引擎（Flowable/Camunda）、MQ、Redis、微服务（理由见 §5.3）
- 不做精算定价、再保险、准备金评估
- 不做监管报送（C-ROSS / EAST）

### 1.5 成功标准

| 指标 | 本项目结果 |
|---|---|
| 业务闭环 | 投保 → 核保 → 承保 → 理赔 → 赔付 → 佣金结算，页面可手工跑通 |
| 核保规则 | 拒保 / 加费 / 标准体三档可复现 |
| 出单幂等 | 同一 `idempotency_key` 重放不重复出单 |
| 资金一致性 | 所有记账分录强制 `Σ借 == Σ贷`，看板实时校验 |
| 可追溯 | 任意保单可反查 佣金 → 分录 → 账户余额快照 |
| 自检 | `smoke-test.ps1` 10 项断言全部通过 |

---

## 2. 领域模型

### 2.1 核心概念

| 概念 | 说明 |
|---|---|
| `Customer` 客户 | 投保主体，含年龄、职业、风险等级 |
| `InsuranceProduct` 保险产品 | 险种、承保公司、保额上限、基准保费、佣金率、风险等级、最高承保年龄、保障年限 |
| `Policy` 保单 | 一次承保的结果，记录保额、保费、佣金、核保结论与生效/到期日 |
| `Claim` 理赔案件 | 基于某张保单的报案、审核结论与赔付金额 |
| `CommissionRecord` 佣金记录 | 出单时按保费 × 佣金率计提，结算后落账 |
| `Account` 科目账户 | 银行存款 / 保费收入 / 应付佣金 / 佣金费用 / 赔付支出 |
| `AccountEntry` 分录 | 账本最小单位，只追加，带前后余额快照 |
| `EventLog` 事件流 | 所有业务动作留痕，前端实时展示 |

### 2.2 核保规则（`domain/rule/UnderwritingEngine`）

按顺序判定，命中即返回：

```
1. 年龄 < 18                                   → REJECTED  拒保
2. 年龄 > 产品.maxAge                          → REJECTED  拒保
3. 申请保额 > 产品.coverageCent（保额上限）    → REJECTED  拒保
4. 年龄 >= 55 或 产品.riskLevel >= 4           → RATED     加费 20%
5. 其余                                        → STANDARD  标准体承保
```

- `RATED` 时保费 = 产品基准保费 × `1.20`
- 拒保件仍落一条 `policy` 记录（状态 `REJECTED`）并写事件，便于统计拒保率

### 2.3 佣金与保费规则（`domain/rule/CommissionCalculator`）

```
premium    = product.premiumCent × loadingRate   (loadingRate = 1.00 / 1.20)
commission = premium × product.commissionRate    (四舍五入到分)
```

- 金额一律以**分为单位的 `long`** 存储，费率计算用 `BigDecimal`
- 全工程不使用 `double` 表示金额

### 2.4 记账规则（`domain/rule/LedgerRule`）

采用按科目性质区分借贷方向的模型：

```
balanceDelta = sign(科目性质) × (DR ? +amount : -amount)
sign(ASSET) = +1        sign(EXPENSE) = +1
sign(LIABILITY) = -1    sign(INCOME) = -1
```

| 业务动作 | 借方 | 贷方 |
|---|---|---|
| 承保收保费 | 银行存款（ASSET） | 保费收入（INCOME） |
| 佣金计提 | 佣金费用（EXPENSE） | 应付佣金（LIABILITY） |
| 佣金结算 | 应付佣金（LIABILITY） | 银行存款（ASSET） |
| 理赔赔付 | 赔付支出（EXPENSE） | 银行存款（ASSET） |

约束：

1. 一批分录提交前强制 `Σ借 == Σ贷`，不平衡抛 `BizException`
2. 分录只追加，逐条记录 `before_balance_cent` / `after_balance_cent`
3. 账户余额按科目性质方向累加

### 2.5 状态机（单向流转）

```
Policy     : ACTIVE ──退保──→ SURRENDERED
             核保不通过 ──→ REJECTED

Claim      : SUBMITTED ──审核通过──→ APPROVED ──赔付──→ PAID
                        └─审核拒绝──→ REJECTED

Commission : ACCRUED ──结算──→ SETTLED
```

约束：

- 仅 `ACTIVE` 保单可报案、可退保
- 仅 `SUBMITTED` 理赔可审核，仅 `APPROVED` 理赔可赔付
- 重复/非法流转抛业务异常并留事件，不静默成功

---

## 3. 数据库设计（H2，`MODE=MySQL`）

金额字段一律 `BIGINT`（单位：分）。DDL 使用 `CREATE TABLE IF NOT EXISTS`，应用每次启动可安全重放。

| 表 | 关键字段 | 关键约束 |
|---|---|---|
| `customer` | `customer_no`, `name`, `age`, `gender`, `occupation`, `risk_level` | `uk_customer_no` |
| `insurance_product` | `product_code`, `product_name`, `product_type`, `insurer`, `coverage_cent`, `premium_cent`, `commission_rate`, `risk_level`, `max_age`, `term_years`, `status` | `uk_product_code` |
| `policy` | `policy_no`, `customer_id`, `product_id`, `insured_name`, `insured_age`, `coverage_cent`, `premium_cent`, `commission_cent`, `status`, `underwriting_result`, `underwriting_reason`, `effective_date`, `expire_date`, `idempotency_key`, `version` | `uk_policy_no`, **`uk_policy_idem(idempotency_key)`** |
| `claim` | `claim_no`, `policy_no`, `customer_id`, `accident_type`, `claim_cent`, `approved_cent`, `status`, `review_remark` | `uk_claim_no` |
| `commission_record` | `commission_no`, `policy_no`, `premium_cent`, `commission_rate`, `commission_cent`, `status`, `settled_at` | `uk_commission_no` |
| `account` | `account_no`, `account_name`, `account_type`, `balance_cent`, `opening_balance_cent`, `version` | `uk_account_no` |
| `account_entry` | `entry_no`, `account_no`, `direction(DR/CR)`, `amount_cent`, `biz_no`, `biz_type`, `before_balance_cent`, `after_balance_cent` | `uk_entry_no`，只追加 |
| `event_log` | `topic`, `biz_no`, `title`, `status`, `detail` | `idx` 由主键倒序查询 |

索引：`idx_policy_customer`、`idx_claim_policy`、`idx_commission_policy`、`idx_entry_biz`。

---

## 4. 技术架构

### 4.1 模块与依赖（严格单向）

```
common          统一响应、金额工具、单号生成、业务异常
  ↑
domain          核保规则、佣金计算、记账规则、领域常量（零框架依赖）
  ↑
infrastructure  8 张表实体 + MyBatis-Plus Mapper
  ↑
application     出单核保、理赔、佣金结算、复式记账、看板、重置
  ↑
interfaces      REST API + Thymeleaf 演示控制台
  ↑
bootstrap       启动类、配置、schema.sql、种子初始化
```

### 4.2 关键设计

| 设计 | 说明 |
|---|---|
| `domain` 零框架 | 核保与记账规则是纯 Java，可脱离 Spring/数据库单测 |
| 复式记账复用 | 佣金结算与理赔赔付共用 `LedgerService`，不另起一套 |
| 事件流 | 每个业务动作写 `event_log`，前端 1.5 秒轮询，形成可追溯时间线 |
| 幂等下沉数据库 | 出单靠 `idempotency_key` 唯一索引兜底，不依赖应用层判重 |
| 金额零浮点 | `long` 分 + `BigDecimal` 费率 |

### 4.3 刻意不引入的技术及理由

| 未引入 | 理由 |
|---|---|
| Flowable / Camunda 工作流 | 当前只有「报案 → 审核 → 赔付」三段流转，状态机足够；引入工作流引擎会为了演示而演示 |
| MQ（RocketMQ/Kafka） | 单机 MVP 下异步通知没有真实消费方，引入后只会新增一致性问题 |
| Redis | 幂等已由数据库唯一索引保证，缓存无强需求 |
| 微服务拆分 | 单机单体更容易保证资金一致性，拆分是规模问题不是展示问题 |
| MySQL 生产库 | 默认 H2 文件库，免安装、可一条命令演示；切换 MySQL 仅需改 `application.yml` 与 DDL 少量语法 |

---

## 5. 接口清单（REST）

统一响应：`{ "code": 0, "message": "ok", "data": ... }`；`code != 0` 为业务失败。

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/customers?keyword=` | 客户列表 |
| POST | `/api/customers` | 新建客户 |
| DELETE | `/api/customers/{id}` | 删除客户 |
| GET | `/api/products` | 产品列表 |
| POST | `/api/products` | 新建产品 |
| DELETE | `/api/products/{id}` | 删除产品 |
| GET | `/api/policies` | 保单列表 |
| POST | `/api/policies/apply` | 投保出单（核保 + 承保 + 计提佣金 + 记账） |
| POST | `/api/policies/{policyNo}/surrender` | 保单退保 |
| GET | `/api/claims` | 理赔列表 |
| POST | `/api/claims` | 理赔报案 |
| POST | `/api/claims/{claimNo}/review` | 理赔审核 |
| POST | `/api/claims/{claimNo}/pay` | 理赔赔付（记账） |
| GET | `/api/commissions` | 佣金列表 |
| POST | `/api/commissions/settle` | 结算全部待结算佣金 |
| POST | `/api/commissions/settle/{policyNo}` | 按保单结算佣金 |
| GET | `/api/dashboard` | 看板汇总 + 借贷平衡 |
| GET | `/api/events?limit=` | 事件流 |
| POST | `/api/reset` | 清空并重置为种子状态 |

投保请求示例：

```json
{
  "customerId": 1,
  "productId": 1,
  "coverageYuan": 1000000,
  "insuredAge": 35,
  "insuredName": "张伟",
  "idempotencyKey": "可选，不传自动生成"
}
```

---

## 6. 一致性设计

1. **出单幂等**：`policy.idempotency_key` 唯一约束；重复请求返回既有保单并记 `IDEMPOTENT` 事件。
2. **事务边界**：出单在一个事务内完成「保单 + 佣金 + 记账」；赔付在事务内完成「状态 + 记账」。
3. **借贷平衡**：`LedgerService.post` 提交前校验，任何一张凭证不平衡直接拒绝。
4. **分录只追加**：历史分录不做 UPDATE/DELETE，余额快照随分录落库。
5. **状态机校验**：非法流转抛 `BizException`，由全局异常处理器统一返回。

---

## 7. 种子数据

| 类型 | 内容 |
|---|---|
| 科目账户（5） | 银行存款（期初 100 万元）、保费收入、应付佣金、佣金费用、赔付支出 |
| 保险产品（4） | 安心终身寿险、康健百万医疗、出行意外保、尊享重疾险 |
| 客户（3） | 张伟（35 岁）、李娜（42 岁）、王强（58 岁，会被加费） |

启动时由 `SeedService` 幂等播种；`POST /api/reset` 可清空后重新播种。

---

## 8. 页面与演示

单页控制台（Thymeleaf + 原生 JS）：

- **顶部 KPI**：保单总数、生效保单、拒保件数、总保费、佣金总额、待结佣金、理赔案件、已赔付
- **借贷平衡徽章**：实时显示借方/贷方合计，不平则标红
- **左侧操作台**：① 新建客户 ② 新建产品 ③ 投保出单 ④ 理赔报案 ⑤ 理赔审核与赔付 ⑥ 佣金结算
- **右侧数据面板**：保单 / 理赔 / 佣金 / 账户 / 事件流 五个标签页，3 秒轮询，事件流 1.5 秒

---

## 9. 端到端自检

`smoke-test.ps1` 覆盖 10 项断言：

1. 重置为干净状态
2. 出单成功且状态 `ACTIVE`
3. 保费 = 5000.00 元（500000 分）
4. 佣金 = 保费 × 25% = 1250.00 元（125000 分）
5. 同一幂等键重复投保返回同一保单
6. 理赔报案状态 `SUBMITTED`
7. 审核通过状态 `APPROVED`，核定金额 8000 元
8. 赔付状态 `PAID`
9. 至少 1 笔佣金结算
10. 看板借贷平衡（借 == 贷）

---

## 10. 与真实保险系统的差距（如实说明）

| 真实系统 | 本项目现状 | 说明 |
|---|---|---|
| 产品工厂 + 条款引擎 | 产品表配置化，未做条款/责任/费率因子引擎 | 现为演示级配置 |
| 核保规则引擎（Drools/DSL） | 纯 Java 规则方法 | 规则与代码未解耦，扩展需改代码 |
| 理赔工作流（多级审批、查勘） | 单节点审核 | 未引入 Flowable |
| 续期缴费、批改、复效 | 未实现 | 生命周期只覆盖承保与退保 |
| 退保退费冲正 | **未实现**，退保仅改状态 | 待补反向凭证 |
| 再保险、准备金 | 未实现 | 非本阶段目标 |
| 监管报送（C-ROSS/EAST） | 未实现 | 后续可做独立项目 |
| 用户与权限 | 无登录、无 RBAC | 演示用单页 |
| 生产数据库与高并发 | H2 单机 | 切换 MySQL 成本较低 |

---

## 11. 后续演进路线

1. **退保冲正**：退保生成反向凭证（借 保费收入 / 贷 银行存款），与 `LedgerCore` 的冲正思路一致
2. **续期缴费**：缴费计划表 + 定时任务 + 逾期提醒
3. **核保规则外置**：规则表 / DSL，支持热更新与可解释决策
4. **理赔工作流**：引入 Flowable，做查勘 → 初审 → 复核多级审批
5. **监管报送**：EAST 类数据报送与一致性校验，可复用对账思路

---

## 12. 面试怎么讲

主线：**「保险的钱和状态，比普通订单复杂在哪」**

1. **业务复杂度**：一张保单要经历投保、核保、承保、批改、续期、理赔、退保，每一步都改变资金或责任状态；普通电商订单只有「创建 → 支付 → 完成」。
2. **核保规则**：为什么规则要独立成引擎而不是 if-else —— 可配置、可解释、可审计。
3. **资金一致性**：佣金计提与结算是两笔钱、两个时点，用复式记账保证「计提有据、结算可查、借贷平衡」。
4. **幂等与状态机**：重复投保、重复赔付的防护，靠数据库唯一索引 + 状态单向流转。
5. **架构权衡**：为什么这个阶段刻意不上工作流、MQ、微服务 —— 一致性优先于技术堆叠。
6. **现场演示**：从投保到佣金结算走一遍，再到 H2 控制台查 `account_entry`，证明数据真实落库。
