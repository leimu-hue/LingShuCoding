package com.dp.ai_code_agent.user.local.mapper;

import cn.xbatis.core.mybatis.mapper.MybatisMapper;
import cn.xbatis.core.sql.executor.chain.QueryChain;
import com.dp.ai_code_agent.user.local.model.User;

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
}
