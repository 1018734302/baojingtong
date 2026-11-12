package com.baoxian.app.service;

import com.baoxian.infra.mapper.AccountEntryMapper;
import com.baoxian.infra.mapper.AccountMapper;
import com.baoxian.infra.mapper.ClaimMapper;
import com.baoxian.infra.mapper.CommissionRecordMapper;
import com.baoxian.infra.mapper.CustomerMapper;
import com.baoxian.infra.mapper.EventLogMapper;
import com.baoxian.infra.mapper.InsuranceProductMapper;
import com.baoxian.infra.mapper.PolicyMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 一键重置为干净演示状态：清空业务表后重新播种 */
@Service
@RequiredArgsConstructor
public class ResetService {

    private final PolicyMapper policyMapper;
    private final ClaimMapper claimMapper;
    private final CommissionRecordMapper commissionMapper;
    private final AccountEntryMapper accountEntryMapper;
    private final AccountMapper accountMapper;
    private final CustomerMapper customerMapper;
    private final InsuranceProductMapper productMapper;
    private final EventLogMapper eventLogMapper;
    private final SeedService seedService;

    @Transactional
    public void reset() {
        policyMapper.delete(null);
        claimMapper.delete(null);
        commissionMapper.delete(null);
        accountEntryMapper.delete(null);
        accountMapper.delete(null);
        customerMapper.delete(null);
        productMapper.delete(null);
        eventLogMapper.delete(null);
        seedService.init();
    }
}
