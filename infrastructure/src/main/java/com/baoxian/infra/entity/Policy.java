package com.baoxian.infra.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("policy")
public class Policy {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String policyNo;
    private Long customerId;
    private Long productId;
    private String productName;
    private String insuredName;
    private Integer insuredAge;
    private Long coverageCent;
    private Long premiumCent;
    private Long commissionCent;
    private String status;
    private String underwritingResult;
    private String underwritingReason;
    private LocalDate effectiveDate;
    private LocalDate expireDate;
    private String idempotencyKey;
    private Long version;
    private LocalDateTime createdAt;
}
