package com.baoxian.app.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baoxian.common.BizException;
import com.baoxian.common.BizNoGenerator;
import com.baoxian.common.MoneyUtils;
import com.baoxian.domain.Constants;
import com.baoxian.domain.rule.CommissionCalculator;
import com.baoxian.domain.rule.UnderwritingEngine;
import com.baoxian.infra.entity.CommissionRecord;
import com.baoxian.infra.entity.Customer;
import com.baoxian.infra.entity.InsuranceProduct;
import com.baoxian.infra.entity.Policy;
import com.baoxian.infra.mapper.CommissionRecordMapper;
import com.baoxian.infra.mapper.CustomerMapper;
import com.baoxian.infra.mapper.InsuranceProductMapper;
import com.baoxian.infra.mapper.PolicyMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 保单服务：投保出单（核保 → 承保 → 佣金计提 → 记账）与保单生命周期管理。
 * 出单幂等由 idempotency_key 唯一约束兜底。
 */
@Service
@RequiredArgsConstructor
public class PolicyService {

    private final PolicyMapper policyMapper;
    private final CustomerMapper customerMapper;
    private final InsuranceProductMapper productMapper;
    private final CommissionRecordMapper commissionMapper;
    private final LedgerService ledger;
    private final EventService events;

    @Transactional
    public Policy apply(Long customerId, Long productId, Long coverageCent,
                        Integer insuredAge, String insuredName, String idempotencyKey) {
        Customer customer = customerMapper.selectById(customerId);
        if (customer == null) {
            throw new BizException("客户不存在: " + customerId);
        }
        InsuranceProduct product = productMapper.selectById(productId);
        if (product == null) {
            throw new BizException("产品不存在: " + productId);
        }

        String key = (idempotencyKey == null || idempotencyKey.isBlank())
                ? BizNoGenerator.gen("IDEM") : idempotencyKey;
        Policy exist = policyMapper.selectOne(new LambdaQueryWrapper<Policy>()
                .eq(Policy::getIdempotencyKey, key));
        if (exist != null) {
            events.log(Constants.TOPIC_POLICY, exist.getPolicyNo(), "重复投保请求（幂等命中）",
                    "IDEMPOTENT", "idempotencyKey=" + key + "，未重复出单");
            return exist;
        }

        int age = insuredAge != null ? insuredAge : customer.getAge();
        long coverage = (coverageCent != null && coverageCent > 0)
                ? coverageCent : product.getCoverageCent();
        String name = (insuredName == null || insuredName.isBlank())
                ? customer.getName() : insuredName;

        UnderwritingEngine.Decision decision = UnderwritingEngine.decide(
                age, coverage, product.getRiskLevel(), product.getMaxAge(), product.getCoverageCent());

        Policy policy = new Policy();
        policy.setPolicyNo(BizNoGenerator.gen("POL"));
        policy.setCustomerId(customerId);
        policy.setProductId(productId);
        policy.setProductName(product.getProductName());
        policy.setInsuredName(name);
        policy.setInsuredAge(age);
        policy.setCoverageCent(coverage);
        policy.setUnderwritingResult(decision.getResult());
        policy.setUnderwritingReason(decision.getReason());
        policy.setIdempotencyKey(key);
        policy.setVersion(0L);

        if (decision.isRejected()) {
            policy.setPremiumCent(0L);
            policy.setCommissionCent(0L);
            policy.setStatus(Constants.UW_REJECTED);
            policyMapper.insert(policy);
            events.log(Constants.TOPIC_POLICY, policy.getPolicyNo(), "核保拒保：" + name,
                    "REJECTED", decision.getReason());
            return policy;
        }

        long premium = MoneyUtils.multiplyRate(product.getPremiumCent(), decision.getLoadingRate());
        long commission = CommissionCalculator.calc(premium, product.getCommissionRate());

        policy.setPremiumCent(premium);
        policy.setCommissionCent(commission);
        policy.setStatus(Constants.POLICY_ACTIVE);
        policy.setEffectiveDate(LocalDate.now());
        policy.setExpireDate(LocalDate.now().plusYears(product.getTermYears()));
        policyMapper.insert(policy);

        CommissionRecord cr = new CommissionRecord();
        cr.setCommissionNo(BizNoGenerator.gen("COM"));
        cr.setPolicyNo(policy.getPolicyNo());
        cr.setPremiumCent(premium);
        cr.setCommissionRate(product.getCommissionRate());
        cr.setCommissionCent(commission);
        cr.setStatus(Constants.COMMISSION_ACCRUED);
        commissionMapper.insert(cr);

        List<LedgerService.EntryLeg> legs = new ArrayList<>();
        if (premium > 0) {
            legs.add(new LedgerService.EntryLeg(Constants.ACC_BANK, Constants.DR, premium));
            legs.add(new LedgerService.EntryLeg(Constants.ACC_PREMIUM, Constants.CR, premium));
        }
        if (commission > 0) {
            legs.add(new LedgerService.EntryLeg(Constants.ACC_COMMISSION_EXPENSE, Constants.DR, commission));
            legs.add(new LedgerService.EntryLeg(Constants.ACC_COMMISSION_PAYABLE, Constants.CR, commission));
        }
        if (!legs.isEmpty()) {
            ledger.post(policy.getPolicyNo(), "POLICY_ISSUE", legs);
        }

        events.log(Constants.TOPIC_POLICY, policy.getPolicyNo(),
                "承保出单：" + product.getProductName(), "OK",
                "被保人 " + name + "/" + age + " 岁，保额 " + MoneyUtils.toYuan(coverage)
                        + " 元，保费 " + MoneyUtils.toYuan(premium) + " 元，核保 " + decision.getResult());
        events.log(Constants.TOPIC_COMMISSION, cr.getCommissionNo(),
                "佣金计提 " + MoneyUtils.toYuan(commission) + " 元", "OK",
                "保单 " + policy.getPolicyNo());
        return policy;
    }

    @Transactional
    public Policy surrender(String policyNo) {
        Policy p = getByNo(policyNo);
        if (!Constants.POLICY_ACTIVE.equals(p.getStatus())) {
            throw new BizException("仅生效保单可退保，当前状态 " + p.getStatus());
        }
        p.setStatus(Constants.POLICY_SURRENDERED);
        policyMapper.updateById(p);
        events.log(Constants.TOPIC_POLICY, policyNo, "保单退保", "OK",
                "产品 " + p.getProductName() + "，保费 " + MoneyUtils.toYuan(p.getPremiumCent()) + " 元");
        return p;
    }

    public List<Policy> list() {
        return policyMapper.selectList(new LambdaQueryWrapper<Policy>().orderByDesc(Policy::getId));
    }

    public Policy getByNo(String policyNo) {
        Policy p = policyMapper.selectOne(new LambdaQueryWrapper<Policy>()
                .eq(Policy::getPolicyNo, policyNo));
        if (p == null) {
            throw new BizException("保单不存在: " + policyNo);
        }
        return p;
    }

    public void clear() {
        policyMapper.delete(null);
    }
}
