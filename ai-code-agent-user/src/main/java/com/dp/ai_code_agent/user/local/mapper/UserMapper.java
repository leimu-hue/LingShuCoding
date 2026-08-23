package com.dp.ai_code_agent.user.local.mapper;

import cn.xbatis.core.mybatis.mapper.MybatisMapper;
import cn.xbatis.core.sql.executor.chain.QueryChain;
import com.dp.ai_code_agent.user.local.model.User;
import com.dp.ai_code_agent.user.spi.model.UserRole;

import java.util.List;

/**
 * 用户 Mapper
 */
public interface UserMapper extends MybatisMapper<User> {

    default User selectByUsername(String username) {
        return QueryChain.of(this).eq(User::getUsername, username).get();
    }

    default boolean existsByUsername(String username) {
        return QueryChain.of(this).eq(User::getUsername, username).exists();
    }

    default List<User> selectPage(String keyword, Integer status, UserRole userRole, int offset, int limit) {
        return pageQuery(keyword, status, userRole).orderByDesc(User::getId).limit(offset, limit).list();
    }

    default int countPage(String keyword, Integer status, UserRole userRole) {
        return pageQuery(keyword, status, userRole).count();
    }

    private QueryChain<User> pageQuery(String keyword, Integer status, UserRole userRole) {
        QueryChain<User> q = QueryChain.of(this);
        if (status != null) {
            q.eq(User::getStatus, status);
        }
        if (userRole != null) {
            q.eq(User::getUserRole, userRole);
        }
        if (keyword != null && !keyword.isBlank()) {
            String kw = "%" + keyword.trim() + "%";
            q.andNested(c -> c.like(User::getUsername, kw).or().like(User::getNickname, kw));
        }
        return q;
    }
}
