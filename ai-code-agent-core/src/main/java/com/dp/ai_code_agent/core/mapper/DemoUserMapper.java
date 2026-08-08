package com.dp.ai_code_agent.core.mapper;

import cn.xbatis.core.mybatis.mapper.MybatisMapper;
import com.dp.ai_code_agent.core.model.DemoUser;

/**
 * 示例 Mapper：继承 MybatisMapper 获得通用 CRUD
 */
public interface DemoUserMapper extends MybatisMapper<DemoUser> {
}