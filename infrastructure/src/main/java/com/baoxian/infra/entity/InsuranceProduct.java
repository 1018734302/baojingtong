package com.baoxian.infra.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("insurance_product")
public class InsuranceProduct {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String productCode;
    private String productName;
    private String productType;
    private String insurer;
    private Long coverageCent;
    private Long premiumCent;
    private BigDecimal commissionRate;
    private Integer riskLevel;
    private Integer maxAge;
    private Integer termYears;
    private String status;
    private LocalDateTime createdAt;
}
