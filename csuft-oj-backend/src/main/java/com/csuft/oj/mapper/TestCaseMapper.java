package com.csuft.oj.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.csuft.oj.entity.TestCase;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TestCaseMapper extends BaseMapper<TestCase> {
}
