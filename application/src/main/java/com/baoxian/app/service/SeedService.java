package com.baoxian.app.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baoxian.common.BizNoGenerator;
import com.baoxian.domain.Constants;
import com.baoxian.infra.entity.Account;
import com.baoxian.infra.entity.Customer;
import com.baoxian.infra.entity.InsuranceProduct;
import com.baoxian.infra.mapper.AccountMapper;
import com.baoxian.infra.mapper.CustomerMapper;
import com.baoxian.infra.mapper.InsuranceProductMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/** 种子数据：幂等初始化科目账户、保险产品与客户 */
@Service
@RequiredArgsConstructor
public class SeedService {

    private final AccountMapper accountMapper;
    private final InsuranceProductMapper productMapper;
    private final CustomerMapper customerMapper;
    private final EventService events;

    @Transactional
    public void init() {
        account(Constants.ACC_BANK, "银行存款", Constants.NATURE_ASSET, 100_000_000L);
        account(Constants.ACC_PREMIUM, "保费收入", Constants.NATURE_INCOME, 0L);
        account(Constants.ACC_COMMISSION_PAYABLE, "应付佣金", Constants.NATURE_LIABILITY, 0L);
        account(Constants.ACC_COMMISSION_EXPENSE, "佣金费用", Constants.NATURE_EXPENSE, 0L);
        account(Constants.ACC_CLAIM_EXPENSE, "赔付支出", Constants.NATURE_EXPENSE, 0L);

        product("LB-001", "安心终身寿险", "寿险", "华泰人寿",
                100_000_000L, 500_000L, new BigDecimal("0.25"), 2, 60, 20);
        product("HL-001", "康健百万医疗", "健康险", "平安健康",
                200_000_000L, 120_000L, new BigDecimal("0.20"), 3, 65, 1);
        product("AC-001", "出行意外保", "意外险", "中国人保",
                50_000_000L, 30_000L, new BigDecimal("0.30"), 1, 70, 1);
        product("HL-002", "尊享重疾险", "健康险", "中国人寿",
                50_000_000L, 800_000L, new BigDecimal("0.35"), 4, 55, 10);

        customer("张伟", "13800000001", "男", 35, "工程师", 1);
        customer("李娜", "13800000002", "女", 42, "教师", 1);
        customer("王强", "13800000003", "男", 58, "企业主", 2);

        events.log(Constants.TOPIC_SYSTEM, "BOOT", "种子数据就绪", "OK",
                "5 个科目账户、4 个保险产品、3 个客户；数据落盘 ./data/baoxian.mv.db");
    }

    private void account(String no, String name, String nature, long opening) {
        Long c = accountMapper.selectCount(new LambdaQueryWrapper<Account>()
                .eq(Account::getAccountNo, no));
        if (c != null && c > 0) {
            return;
        }
        Account a = new Account();
        a.setAccountNo(no);
        a.setAccountName(name);
        a.setAccountType(nature);
        a.setOpeningBalanceCent(opening);
        a.setBalanceCent(opening);
        a.setVersion(0L);
        accountMapper.insert(a);
    }

    private void product(String code, String name, String type, String insurer,
                         long coverageCent, long premiumCent, BigDecimal rate,
                         int riskLevel, int maxAge, int termYears) {
        Long c = productMapper.selectCount(new LambdaQueryWrapper<InsuranceProduct>()
                .eq(InsuranceProduct::getProductCode, code));
        if (c != null && c > 0) {
            return;
        }
        InsuranceProduct p = new InsuranceProduct();
        p.setProductCode(code);
        p.setProductName(name);
        p.setProductType(type);
        p.setInsurer(insurer);
        p.setCoverageCent(coverageCent);
        p.setPremiumCent(premiumCent);
        p.setCommissionRate(rate);
        p.setRiskLevel(riskLevel);
        p.setMaxAge(maxAge);
        p.setTermYears(termYears);
        p.setStatus("ON_SALE");
        productMapper.insert(p);
    }

    private void customer(String name, String phone, String gender, int age,
                          String occupation, int riskLevel) {
        Long c = customerMapper.selectCount(new LambdaQueryWrapper<Customer>()
                .eq(Customer::getName, name));
        if (c != null && c > 0) {
            return;
        }
        Customer cu = new Customer();
        cu.setCustomerNo(BizNoGenerator.gen("C"));
        cu.setName(name);
        cu.setPhone(phone);
        cu.setGender(gender);
        cu.setAge(age);
        cu.setOccupation(occupation);
        cu.setRiskLevel(riskLevel);
        customerMapper.insert(cu);
    }
}
