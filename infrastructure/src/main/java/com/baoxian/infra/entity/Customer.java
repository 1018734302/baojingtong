package com.baoxian.infra.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("customer")
public class Customer {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String customerNo;
    private String name;
    private String idCard;
    private String phone;
    private String gender;
    private Integer age;
    private String occupation;
    private Integer riskLevel;
    private LocalDateTime createdAt;
}
