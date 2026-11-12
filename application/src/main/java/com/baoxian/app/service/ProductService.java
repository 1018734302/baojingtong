package com.baoxian.app.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baoxian.common.BizException;
import com.baoxian.common.BizNoGenerator;
import com.baoxian.domain.Constants;
import com.baoxian.infra.entity.InsuranceProduct;
import com.baoxian.infra.mapper.InsuranceProductMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final InsuranceProductMapper productMapper;
    private final EventService events;

    public InsuranceProduct create(InsuranceProduct p) {
        if (p.getProductName() == null || p.getProductName().isBlank()) {
            throw new BizException("产品名称不能为空");
        }
        p.setId(null);
        if (p.getProductCode() == null || p.getProductCode().isBlank()) {
            p.setProductCode(BizNoGenerator.gen("P"));
        }
        if (p.getStatus() == null) {
            p.setStatus("ON_SALE");
        }
        if (p.getCommissionRate() == null) {
            p.setCommissionRate(BigDecimal.ZERO);
        }
        if (p.getRiskLevel() == null) {
            p.setRiskLevel(1);
        }
        if (p.getMaxAge() == null) {
            p.setMaxAge(65);
        }
        if (p.getTermYears() == null) {
            p.setTermYears(1);
        }
        productMapper.insert(p);
        events.log(Constants.TOPIC_PRODUCT, p.getProductCode(), "新增产品 " + p.getProductName(), "OK",
                "类型 " + p.getProductType() + "，承保公司 " + p.getInsurer());
        return p;
    }

    public List<InsuranceProduct> list() {
        return productMapper.selectList(new LambdaQueryWrapper<InsuranceProduct>()
                .orderByDesc(InsuranceProduct::getId));
    }

    public InsuranceProduct get(Long id) {
        InsuranceProduct p = productMapper.selectById(id);
        if (p == null) {
            throw new BizException("产品不存在: " + id);
        }
        return p;
    }

    public void delete(Long id) {
        productMapper.deleteById(id);
    }
}
