package com.dp.ai_code_agent.user.local.mapper;

import cn.xbatis.core.mybatis.mapper.MybatisMapper;
import cn.xbatis.core.sql.executor.chain.QueryChain;
import com.dp.ai_code_agent.user.local.model.Role;

import java.util.Collection;
import java.util.List;

/**
 * 角色 Mapper
 */
public interface RoleMapper extends MybatisMapper<Role> {

    default Role selectByCode(String code) {
        return QueryChain.of(this).eq(Role::getCode, code).get();
    }

    default List<Role> selectByIds(Collection<Long> ids) {
        return QueryChain.of(this).in(Role::getId, ids).list();
    }
}
