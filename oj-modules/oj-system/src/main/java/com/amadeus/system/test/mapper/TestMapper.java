package com.amadeus.system.test.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.amadeus.system.test.domain.TestDomain;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TestMapper  extends BaseMapper<TestDomain> {
}
