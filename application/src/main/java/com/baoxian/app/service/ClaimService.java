package com.baoxian.app.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baoxian.common.BizException;
import com.baoxian.common.BizNoGenerator;
import com.baoxian.common.MoneyUtils;
import com.baoxian.domain.Constants;
import com.baoxian.infra.entity.Claim;
import com.baoxian.infra.entity.Policy;
import com.baoxian.infra.mapper.ClaimMapper;
import com.baoxian.infra.mapper.PolicyMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 理赔服务：报案 → 审核（通过/拒赔）→ 赔付（记账）。
 * 状态机单向流转，重复操作会被拒绝并留痕。
 */
@Service
@RequiredArgsConstructor
public class ClaimService {

    private final ClaimMapper claimMapper;
    private final PolicyMapper policyMapper;
    private final LedgerService ledger;
    private final EventService events;

    @Transactional
    public Claim submit(String policyNo, String accidentType, Long claimCent) {
        Policy policy = policyMapper.selectOne(new LambdaQueryWrapper<Policy>()
                .eq(Policy::getPolicyNo, policyNo));
        if (policy == null) {
            throw new BizException("保单不存在: " + policyNo);
        }
        if (!Constants.POLICY_ACTIVE.equals(policy.getStatus())) {
            throw new BizException("仅生效保单可报案，当前保单状态 " + policy.getStatus());
        }
        if (claimCent == null || claimCent <= 0) {
            throw new BizException("申请金额必须大于 0");
        }
        if (claimCent > policy.getCoverageCent()) {
            throw new BizException("申请金额不能超过保单保额");
        }

        Claim c = new Claim();
        c.setClaimNo(BizNoGenerator.gen("CLM"));
        c.setPolicyNo(policyNo);
        c.setCustomerId(policy.getCustomerId());
        c.setAccidentType(accidentType == null || accidentType.isBlank() ? "其他" : accidentType);
        c.setClaimCent(claimCent);
        c.setApprovedCent(0L);
        c.setStatus(Constants.CLAIM_SUBMITTED);
        c.setCreatedAt(LocalDateTime.now());
        c.setUpdatedAt(LocalDateTime.now());
        claimMapper.insert(c);

        events.log(Constants.TOPIC_CLAIM, c.getClaimNo(), "理赔报案：" + c.getAccidentType(), "OK",
                "保单 " + policyNo + "，申请 " + MoneyUtils.toYuan(claimCent) + " 元");
        return c;
    }

    @Transactional
    public Claim review(String claimNo, boolean approve, Long approvedCent, String remark) {
        Claim c = getByNo(claimNo);
        if (!Constants.CLAIM_SUBMITTED.equals(c.getStatus())
                && !Constants.CLAIM_REVIEWING.equals(c.getStatus())) {
            throw new BizException("当前理赔状态不可审核: " + c.getStatus());
        }
        if (approve) {
            long amount = (approvedCent != null && approvedCent > 0) ? approvedCent : c.getClaimCent();
            if (amount > c.getClaimCent()) {
                throw new BizException("核定金额不能超过申请金额");
            }
            c.setStatus(Constants.CLAIM_APPROVED);
            c.setApprovedCent(amount);
            c.setReviewRemark(remark == null ? "审核通过" : remark);
            events.log(Constants.TOPIC_CLAIM, claimNo, "理赔审核通过", "OK",
                    "核定 " + MoneyUtils.toYuan(amount) + " 元");
        } else {
            c.setStatus(Constants.CLAIM_REJECTED);
            c.setApprovedCent(0L);
            c.setReviewRemark(remark == null ? "审核拒绝" : remark);
            events.log(Constants.TOPIC_CLAIM, claimNo, "理赔拒赔", "REJECTED",
                    c.getReviewRemark());
        }
        c.setUpdatedAt(LocalDateTime.now());
        claimMapper.updateById(c);
        return c;
    }

    @Transactional
    public Claim pay(String claimNo) {
        Claim c = getByNo(claimNo);
        if (!Constants.CLAIM_APPROVED.equals(c.getStatus())) {
            throw new BizException("仅审核通过的理赔可赔付，当前状态 " + c.getStatus());
        }
        long amount = c.getApprovedCent();
        if (amount <= 0) {
            throw new BizException("核定金额为 0，无法赔付");
        }
        ledger.post(claimNo, "CLAIM_PAY", List.of(
                new LedgerService.EntryLeg(Constants.ACC_CLAIM_EXPENSE, Constants.DR, amount),
                new LedgerService.EntryLeg(Constants.ACC_BANK, Constants.CR, amount)
        ));
        c.setStatus(Constants.CLAIM_PAID);
        c.setUpdatedAt(LocalDateTime.now());
        claimMapper.updateById(c);
        events.log(Constants.TOPIC_CLAIM, claimNo, "理赔赔付完成", "OK",
                "赔付 " + MoneyUtils.toYuan(amount) + " 元，已记账");
        return c;
    }

    public List<Claim> list() {
        return claimMapper.selectList(new LambdaQueryWrapper<Claim>().orderByDesc(Claim::getId));
    }

    public Claim getByNo(String claimNo) {
        Claim c = claimMapper.selectOne(new LambdaQueryWrapper<Claim>()
                .eq(Claim::getClaimNo, claimNo));
        if (c == null) {
            throw new BizException("理赔案件不存在: " + claimNo);
        }
        return c;
    }

    public void clear() {
        claimMapper.delete(null);
    }
}
