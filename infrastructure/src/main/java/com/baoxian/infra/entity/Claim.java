package com.baoxian.infra.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("claim")
public class Claim {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String claimNo;
    private String policyNo;
    private Long customerId;
    private String accidentType;
    private Long claimCent;
    private Long approvedCent;
    private String status;
    private String reviewRemark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
