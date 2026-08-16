package com.dp.ai_code_agent.user.local.mapper;

import cn.xbatis.core.mybatis.mapper.MybatisMapper;
import cn.xbatis.core.sql.executor.chain.DeleteChain;
import cn.xbatis.core.sql.executor.chain.QueryChain;
import com.dp.ai_code_agent.user.local.model.UserRole;

import java.util.List;

/**
 * 用户-角色关联 Mapper
 */
public interface UserRoleMapper extends MybatisMapper<UserRole> {

    default List<UserRole> selectByUserId(Long userId) {
        return QueryChain.of(this).eq(UserRole::getUserId, userId).list();
    }

    default List<UserRole> selectByRoleId(Long roleId) {
        return QueryChain.of(this).eq(UserRole::getRoleId, roleId).list();
    }

    default int deleteByUserId(Long userId) {
        return DeleteChain.of(this).eq(UserRole::getUserId, userId).execute();
    }
}
