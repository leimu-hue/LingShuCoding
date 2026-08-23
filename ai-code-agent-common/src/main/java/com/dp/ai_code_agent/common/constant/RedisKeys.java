package com.dp.ai_code_agent.common.constant;

/**
 * Redis 键名常量与拼接工具，集中管理避免散落各处。
 * <p>
 * 后续新增缓存/会话/队列等键统一在此维护，禁止各模块手写硬编码 key。
 */
public final class RedisKeys {

    private RedisKeys() {
    }

    /** 用户认证会话快照前缀：ua:session:{token} */
    public static final String AUTH_SESSION_PREFIX = "ua:session:";

    /** 用户有效会话集合前缀：ua:user-sessions:{userId} */
    public static final String AUTH_USER_SESSIONS_PREFIX = "ua:user-sessions:";

    /** 会话快照键：ua:session:{token} */
    public static String session(String token) {
        return AUTH_SESSION_PREFIX + token;
    }

    /** 用户会话集合键：ua:user-sessions:{userId} */
    public static String userSessions(Long userId) {
        return AUTH_USER_SESSIONS_PREFIX + userId;
    }
}
