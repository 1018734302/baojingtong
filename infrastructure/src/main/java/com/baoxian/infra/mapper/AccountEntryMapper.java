package com.baoxian.infra.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baoxian.infra.entity.AccountEntry;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AccountEntryMapper extends BaseMapper<AccountEntry> {
}
