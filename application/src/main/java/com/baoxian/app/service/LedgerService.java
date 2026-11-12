package com.baoxian.app.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baoxian.common.BizException;
import com.baoxian.common.BizNoGenerator;
import com.baoxian.domain.Constants;
import com.baoxian.domain.rule.LedgerRule;
import com.baoxian.infra.entity.Account;
import com.baoxian.infra.entity.AccountEntry;
import com.baoxian.infra.mapper.AccountEntryMapper;
import com.baoxian.infra.mapper.AccountMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 复式记账服务：提交一批分录，强制 Σ借 == Σ贷，逐条更新账户余额并留下前后余额快照。
 * 分录只追加，不修改历史。
 */
@Service
@RequiredArgsConstructor
public class LedgerService {

    private final AccountMapper accountMapper;
    private final AccountEntryMapper accountEntryMapper;

    /** 一条分录腿 */
    public record EntryLeg(String accountNo, String direction, long amountCent) {
    }

    @Transactional
    public void post(String bizNo, String bizType, List<EntryLeg> legs) {
        long debit = legs.stream()
                .filter(l -> Constants.DR.equals(l.direction()))
                .mapToLong(EntryLeg::amountCent).sum();
        long credit = legs.stream()
                .filter(l -> Constants.CR.equals(l.direction()))
                .mapToLong(EntryLeg::amountCent).sum();
        if (debit != credit) {
            throw new BizException("借贷不平衡：借 " + debit + " 分，贷 " + credit + " 分");
        }

        for (EntryLeg leg : legs) {
            Account acc = accountMapper.selectOne(new LambdaQueryWrapper<Account>()
                    .eq(Account::getAccountNo, leg.accountNo()));
            if (acc == null) {
                throw new BizException("账户不存在: " + leg.accountNo());
            }
            long delta = LedgerRule.balanceDelta(acc.getAccountType(), leg.direction(), leg.amountCent());
            long before = acc.getBalanceCent();
            long after = before + delta;
            acc.setBalanceCent(after);
            accountMapper.updateById(acc);

            AccountEntry entry = new AccountEntry();
            entry.setEntryNo(BizNoGenerator.gen("E"));
            entry.setAccountNo(leg.accountNo());
            entry.setDirection(leg.direction());
            entry.setAmountCent(leg.amountCent());
            entry.setBizNo(bizNo);
            entry.setBizType(bizType);
            entry.setBeforeBalanceCent(before);
            entry.setAfterBalanceCent(after);
            accountEntryMapper.insert(entry);
        }
    }

    /** 借贷平衡自检：返回 [借方合计, 贷方合计] */
    public long[] totals() {
        long dr = 0L;
        long cr = 0L;
        for (AccountEntry e : accountEntryMapper.selectList(null)) {
            if (Constants.DR.equals(e.getDirection())) {
                dr += e.getAmountCent();
            } else {
                cr += e.getAmountCent();
            }
        }
        return new long[]{dr, cr};
    }

    public List<Account> accounts() {
        return accountMapper.selectList(new LambdaQueryWrapper<Account>().orderByAsc(Account::getAccountNo));
    }

    public void clear() {
        accountEntryMapper.delete(null);
        accountMapper.delete(null);
    }
}
