package com.baoxian.infra.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("account_entry")
public class AccountEntry {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String entryNo;
    private String accountNo;
    private String direction;
    private Long amountCent;
    private String bizNo;
    private String bizType;
    private Long beforeBalanceCent;
    private Long afterBalanceCent;
    private LocalDateTime createdAt;
}
