package com.dp.ai_code_agent.user.local.mapper;

import cn.xbatis.core.mybatis.mapper.MybatisMapper;
import cn.xbatis.core.sql.executor.chain.QueryChain;
import com.dp.ai_code_agent.user.local.model.User;

import java.util.Collection;
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

    default List<User> selectPage(String keyword, Integer status, Collection<Long> userIds, int offset, int limit) {
        return pageQuery(keyword, status, userIds).orderByDesc(User::getId).limit(offset, limit).list();
    }

    default int countPage(String keyword, Integer status, Collection<Long> userIds) {
        return pageQuery(keyword, status, userIds).count();
    }

    private QueryChain<User> pageQuery(String keyword, Integer status, Collection<Long> userIds) {
        QueryChain<User> q = QueryChain.of(this);
        if (status != null) {
            q.eq(User::getStatus, status);
        }
        if (userIds != null && !userIds.isEmpty()) {
            q.in(User::getId, userIds);
        }
        if (keyword != null && !keyword.isBlank()) {
            String kw = "%" + keyword.trim() + "%";
            q.andNested(c -> c.like(User::getUsername, kw).or().like(User::getNickname, kw));
        }
        return q;
    }
}

