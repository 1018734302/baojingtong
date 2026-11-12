package com.baoxian.infra.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("commission_record")
public class CommissionRecord {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String commissionNo;
    private String policyNo;
    private Long premiumCent;
    private BigDecimal commissionRate;
    private Long commissionCent;
    private String status;
    private LocalDateTime settledAt;
    private LocalDateTime createdAt;
}
