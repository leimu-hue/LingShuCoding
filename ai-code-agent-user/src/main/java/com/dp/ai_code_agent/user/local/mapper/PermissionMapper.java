package com.dp.ai_code_agent.user.local.mapper;

import cn.xbatis.core.mybatis.mapper.MybatisMapper;
import cn.xbatis.core.sql.executor.chain.QueryChain;
import com.dp.ai_code_agent.user.local.model.Permission;

import java.util.Collection;
import java.util.List;

/**
 * 权限 Mapper
 */
public interface PermissionMapper extends MybatisMapper<Permission> {

    default List<Permission> selectByIds(Collection<Long> ids) {
        return QueryChain.of(this).in(Permission::getId, ids).list();
    }
}
