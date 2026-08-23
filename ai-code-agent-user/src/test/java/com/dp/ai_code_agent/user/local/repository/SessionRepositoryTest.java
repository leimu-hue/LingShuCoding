package com.dp.ai_code_agent.user.local.repository;

import com.dp.ai_code_agent.user.spi.model.UserIdentity;
import com.dp.ai_code_agent.user.spi.model.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SessionRepositoryTest {

    StringRedisTemplate redis;
    ValueOperations<String, String> valueOps;
    SetOperations<String, String> setOps;
    ObjectMapper objectMapper;
    SessionRepository repo;

    @BeforeEach
    void setUp() {
        redis = mock(StringRedisTemplate.class);
        valueOps = mock(ValueOperations.class);
        setOps = mock(SetOperations.class);
        when(redis.opsForValue()).thenReturn(valueOps);
        when(redis.opsForSet()).thenReturn(setOps);
        objectMapper = new ObjectMapper();
        repo = new SessionRepository(redis, objectMapper);
    }

    private UserIdentity user() {
        return new UserIdentity(1L, "alice", "Alice", UserRole.USER, true);
    }

    @Test
    void save_writesSessionAndRegistersToken() throws Exception {
        UserIdentity u = user();
        Duration ttl = Duration.ofMinutes(30);
        repo.save("tok", u, ttl);

        String json = objectMapper.writeValueAsString(u);
        verify(valueOps).set(eq("ua:session:tok"), eq(json), eq(ttl));
        verify(setOps).add("ua:user-sessions:1", "tok");
        verify(redis).expire("ua:user-sessions:1", ttl);
    }

    @Test
    void find_returnsIdentityAndRenewsTtl() throws Exception {
        UserIdentity u = user();
        String json = objectMapper.writeValueAsString(u);
        when(valueOps.get("ua:session:tok")).thenReturn(json);

        Optional<UserIdentity> result = repo.find("tok", Duration.ofMinutes(30));

        assertThat(result).contains(u);
        verify(redis).expire("ua:session:tok", Duration.ofMinutes(30));
    }

    @Test
    void find_missingTokenReturnsEmpty() {
        when(valueOps.get("ua:session:tok")).thenReturn(null);
        assertThat(repo.find("tok", Duration.ofMinutes(30))).isEmpty();
        verify(redis, never()).expire(anyString(), any(Duration.class));
    }

    @Test
    void remove_deletesSessionAndDeregistersToken() throws Exception {
        UserIdentity u = user();
        when(valueOps.get("ua:session:tok")).thenReturn(objectMapper.writeValueAsString(u));

        repo.remove("tok");

        verify(redis).delete("ua:session:tok");
        verify(setOps).remove("ua:user-sessions:1", "tok");
    }

    @Test
    void removeAllByUserId_deletesAllSessionsAndSet() {
        when(setOps.members("ua:user-sessions:1")).thenReturn(Set.of("t1", "t2"));

        repo.removeAllByUserId(1L);

        verify(redis).delete("ua:session:t1");
        verify(redis).delete("ua:session:t2");
        verify(redis).delete("ua:user-sessions:1");
    }
}
