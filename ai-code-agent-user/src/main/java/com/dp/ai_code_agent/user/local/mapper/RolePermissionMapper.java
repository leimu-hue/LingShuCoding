package com.dp.ai_code_agent.user.local.mapper;

import cn.xbatis.core.mybatis.mapper.MybatisMapper;
import cn.xbatis.core.sql.executor.chain.DeleteChain;
import cn.xbatis.core.sql.executor.chain.QueryChain;
import com.dp.ai_code_agent.user.local.model.RolePermission;

import java.util.Collection;
import java.util.List;

/**
 * 角色-权限关联 Mapper
 */
public interface RolePermissionMapper extends MybatisMapper<RolePermission> {

    default List<RolePermission> selectByRoleIds(Collection<Long> roleIds) {
        return QueryChain.of(this).in(RolePermission::getRoleId, roleIds).list();
    }

    default int deleteByRoleId(Long roleId) {
        return DeleteChain.of(this).eq(RolePermission::getRoleId, roleId).execute();
    }
}
