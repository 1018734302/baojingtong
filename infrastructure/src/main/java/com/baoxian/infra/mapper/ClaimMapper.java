package com.baoxian.infra.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baoxian.infra.entity.Claim;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ClaimMapper extends BaseMapper<Claim> {
}
