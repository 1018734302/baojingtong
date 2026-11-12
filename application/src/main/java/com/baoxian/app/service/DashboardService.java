package com.baoxian.app.service;

import com.baoxian.domain.Constants;
import com.baoxian.infra.entity.Account;
import com.baoxian.infra.entity.Claim;
import com.baoxian.infra.entity.CommissionRecord;
import com.baoxian.infra.entity.Policy;
import com.baoxian.infra.mapper.ClaimMapper;
import com.baoxian.infra.mapper.CommissionRecordMapper;
import com.baoxian.infra.mapper.PolicyMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 看板：保单/保费/佣金/理赔汇总与借贷平衡校验 */
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final PolicyMapper policyMapper;
    private final ClaimMapper claimMapper;
    private final CommissionRecordMapper commissionMapper;
    private final LedgerService ledger;

    public Map<String, Object> summary() {
        List<Policy> policies = policyMapper.selectList(null);
        List<Claim> claims = claimMapper.selectList(null);
        List<CommissionRecord> commissions = commissionMapper.selectList(null);

        long policyCount = policies.size();
        long activeCount = policies.stream()
                .filter(p -> Constants.POLICY_ACTIVE.equals(p.getStatus())).count();
        long rejectedCount = policies.stream()
                .filter(p -> Constants.UW_REJECTED.equals(p.getStatus())).count();
        long totalPremium = policies.stream()
                .mapToLong(p -> p.getPremiumCent() == null ? 0 : p.getPremiumCent()).sum();
        long totalCommission = commissions.stream()
                .mapToLong(c -> c.getCommissionCent() == null ? 0 : c.getCommissionCent()).sum();
        long accruedCommission = commissions.stream()
                .filter(c -> Constants.COMMISSION_ACCRUED.equals(c.getStatus()))
                .mapToLong(c -> c.getCommissionCent() == null ? 0 : c.getCommissionCent()).sum();
        long claimCount = claims.size();
        long paidClaim = claims.stream()
                .filter(c -> Constants.CLAIM_PAID.equals(c.getStatus()))
                .mapToLong(c -> c.getApprovedCent() == null ? 0 : c.getApprovedCent()).sum();

        long[] totals = ledger.totals();
        List<Account> accounts = ledger.accounts();

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("policyCount", policyCount);
        m.put("activeCount", activeCount);
        m.put("rejectedCount", rejectedCount);
        m.put("totalPremiumCent", totalPremium);
        m.put("totalCommissionCent", totalCommission);
        m.put("accruedCommissionCent", accruedCommission);
        m.put("claimCount", claimCount);
        m.put("paidClaimCent", paidClaim);
        m.put("debitCent", totals[0]);
        m.put("creditCent", totals[1]);
        m.put("balanced", totals[0] == totals[1]);
        m.put("accounts", accounts);
        return m;
    }
}
