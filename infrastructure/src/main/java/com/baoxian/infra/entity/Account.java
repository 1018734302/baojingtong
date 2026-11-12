package com.baoxian.infra.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("account")
public class Account {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String accountNo;
    private String accountName;
    private String accountType;
    private Long balanceCent;
    private Long openingBalanceCent;
    private Long version;
    private LocalDateTime createdAt;
}
