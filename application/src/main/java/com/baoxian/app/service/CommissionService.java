package com.baoxian.app.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baoxian.common.BizException;
import com.baoxian.common.MoneyUtils;
import com.baoxian.domain.Constants;
import com.baoxian.infra.entity.CommissionRecord;
import com.baoxian.infra.mapper.CommissionRecordMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 佣金服务：计提（出单时）→ 结算（借应付佣金 / 贷银行存款）。
 * 结算逻辑复用复式记账，保证佣金支付可追溯。
 */
@Service
@RequiredArgsConstructor
public class CommissionService {

    private final CommissionRecordMapper commissionMapper;
    private final LedgerService ledger;
    private final EventService events;

    public List<CommissionRecord> list() {
        return commissionMapper.selectList(new LambdaQueryWrapper<CommissionRecord>()
                .orderByDesc(CommissionRecord::getId));
    }

    /** 结算全部待结算佣金 */
    @Transactional
    public int settleAll() {
        List<CommissionRecord> pending = commissionMapper.selectList(
                new LambdaQueryWrapper<CommissionRecord>()
                        .eq(CommissionRecord::getStatus, Constants.COMMISSION_ACCRUED));
        int count = 0;
        for (CommissionRecord c : pending) {
            settleOne(c);
            count++;
        }
        events.log(Constants.TOPIC_COMMISSION, "-", "佣金批量结算完成", "OK",
                "本批结算 " + count + " 笔");
        return count;
    }

    /** 按保单结算佣金 */
    @Transactional
    public CommissionRecord settleByPolicy(String policyNo) {
        CommissionRecord c = commissionMapper.selectOne(
                new LambdaQueryWrapper<CommissionRecord>()
                        .eq(CommissionRecord::getPolicyNo, policyNo)
                        .eq(CommissionRecord::getStatus, Constants.COMMISSION_ACCRUED)
                        .last("LIMIT 1"));
        if (c == null) {
            throw new BizException("该保单没有待结算佣金: " + policyNo);
        }
        settleOne(c);
        return c;
    }

    private void settleOne(CommissionRecord c) {
        long amount = c.getCommissionCent();
        if (amount > 0) {
            ledger.post(c.getCommissionNo(), "COMMISSION_SETTLE", List.of(
                    new LedgerService.EntryLeg(Constants.ACC_COMMISSION_PAYABLE, Constants.DR, amount),
                    new LedgerService.EntryLeg(Constants.ACC_BANK, Constants.CR, amount)
            ));
        }
        c.setStatus(Constants.COMMISSION_SETTLED);
        c.setSettledAt(LocalDateTime.now());
        commissionMapper.updateById(c);
        events.log(Constants.TOPIC_COMMISSION, c.getCommissionNo(), "佣金结算", "OK",
                "保单 " + c.getPolicyNo() + "，支付 " + MoneyUtils.toYuan(amount) + " 元");
    }

    public void clear() {
        commissionMapper.delete(null);
    }
}
