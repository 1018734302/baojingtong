package com.baoxian.infra.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baoxian.infra.entity.Customer;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CustomerMapper extends BaseMapper<Customer> {
}
