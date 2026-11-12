package com.baoxian.app.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baoxian.common.BizException;
import com.baoxian.common.BizNoGenerator;
import com.baoxian.domain.Constants;
import com.baoxian.infra.entity.Customer;
import com.baoxian.infra.mapper.CustomerMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerMapper customerMapper;
    private final EventService events;

    public Customer create(Customer c) {
        if (c.getName() == null || c.getName().isBlank()) {
            throw new BizException("客户姓名不能为空");
        }
        if (c.getAge() == null) {
            c.setAge(30);
        }
        if (c.getRiskLevel() == null) {
            c.setRiskLevel(1);
        }
        c.setId(null);
        c.setCustomerNo(BizNoGenerator.gen("C"));
        customerMapper.insert(c);
        events.log(Constants.TOPIC_CUSTOMER, c.getCustomerNo(), "新建客户 " + c.getName(), "OK",
                "年龄 " + c.getAge() + "，职业 " + safe(c.getOccupation()));
        return c;
    }

    public List<Customer> list(String keyword) {
        LambdaQueryWrapper<Customer> w = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            w.like(Customer::getName, keyword).or().like(Customer::getCustomerNo, keyword);
        }
        w.orderByDesc(Customer::getId);
        return customerMapper.selectList(w);
    }

    public Customer get(Long id) {
        Customer c = customerMapper.selectById(id);
        if (c == null) {
            throw new BizException("客户不存在: " + id);
        }
        return c;
    }

    public void delete(Long id) {
        customerMapper.deleteById(id);
    }

    private String safe(String s) {
        return s == null ? "-" : s;
    }
}
