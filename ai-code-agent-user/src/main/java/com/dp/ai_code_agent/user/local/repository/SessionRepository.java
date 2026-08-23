package com.dp.ai_code_agent.user.local.repository;

import com.dp.ai_code_agent.common.constant.RedisKeys;
import com.dp.ai_code_agent.user.spi.model.UserIdentity;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.Optional;
import java.util.Set;

/**
 * Redis 会话仓库：会话存取、滑续、级联删除。
 * <p>
 * 键统一由 {@link RedisKeys} 维护：
 * <ul>
 *     <li>{@code ua:session:{token}} —— 会话快照 JSON</li>
 *     <li>{@code ua:user-sessions:{userId}} —— 该用户全部有效 token 集合</li>
 * </ul>
 */
@Component
public class SessionRepository {

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    public SessionRepository(StringRedisTemplate redis, ObjectMapper objectMapper) {
        this.redis = redis;
        this.objectMapper = objectMapper;
    }

    public void save(String token, UserIdentity user, Duration ttl) {
        String sessionKey = RedisKeys.session(token);
        String userSessionsKey = RedisKeys.userSessions(user.id());
        redis.opsForValue().set(sessionKey, toJson(user), ttl);
        redis.opsForSet().add(userSessionsKey, token);
        redis.expire(userSessionsKey, ttl);
    }

    public Optional<UserIdentity> find(String token, Duration ttl) {
        String sessionKey = RedisKeys.session(token);
        String json = redis.opsForValue().get(sessionKey);
        if (json == null) {
            return Optional.empty();
        }
        redis.expire(sessionKey, ttl); // 访问即滑续
        return Optional.ofNullable(fromJson(json));
    }

    public void remove(String token) {
        String sessionKey = RedisKeys.session(token);
        String json = redis.opsForValue().get(sessionKey);
        redis.delete(sessionKey);
        if (json != null) {
            UserIdentity user = fromJson(json);
            if (user != null) {
                redis.opsForSet().remove(RedisKeys.userSessions(user.id()), token);
            }
        }
    }

    public void removeAllByUserId(Long userId) {
        String userSessionsKey = RedisKeys.userSessions(userId);
        Set<String> tokens = redis.opsForSet().members(userSessionsKey);
        if (tokens != null) {
            tokens.forEach(t -> redis.delete(RedisKeys.session(t)));
        }
        redis.delete(userSessionsKey);
    }

    private String toJson(UserIdentity user) {
        try {
            return objectMapper.writeValueAsString(user);
        } catch (JacksonException e) {
            throw new IllegalStateException("序列化会话失败", e);
        }
    }

    private UserIdentity fromJson(String json) {
        try {
            return objectMapper.readValue(json, UserIdentity.class);
        } catch (JacksonException e) {
            return null;
        }
    }
}
