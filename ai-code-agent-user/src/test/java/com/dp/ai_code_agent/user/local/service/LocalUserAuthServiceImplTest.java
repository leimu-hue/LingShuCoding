package com.dp.ai_code_agent.user.local.service;

import com.dp.ai_code_agent.common.exception.BusinessException;
import com.dp.ai_code_agent.common.exception.ErrorCode;
import com.dp.ai_code_agent.user.local.converter.UserConverter;
import com.dp.ai_code_agent.user.local.mapper.PermissionMapper;
import com.dp.ai_code_agent.user.local.mapper.RoleMapper;
import com.dp.ai_code_agent.user.local.mapper.RolePermissionMapper;
import com.dp.ai_code_agent.user.local.mapper.UserMapper;
import com.dp.ai_code_agent.user.local.mapper.UserRoleMapper;
import com.dp.ai_code_agent.user.local.model.Role;
import com.dp.ai_code_agent.user.local.model.User;
import com.dp.ai_code_agent.user.local.model.UserRole;
import com.dp.ai_code_agent.user.local.repository.SessionRepository;
import com.dp.ai_code_agent.user.local.security.PasswordHasher;
import com.dp.ai_code_agent.user.spi.model.LoginResult;
import com.dp.ai_code_agent.user.spi.model.UserIdentity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LocalUserAuthServiceImplTest {

    UserMapper userMapper;
    RoleMapper roleMapper;
    PermissionMapper permissionMapper;
    UserRoleMapper userRoleMapper;
    RolePermissionMapper rolePermissionMapper;
    PasswordHasher passwordHasher;
    SessionRepository sessionRepository;
    UserConverter userConverter;

    LocalUserAuthServiceImpl service;

    @BeforeEach
    void setUp() {
        userMapper = mock(UserMapper.class);
        roleMapper = mock(RoleMapper.class);
        permissionMapper = mock(PermissionMapper.class);
        userRoleMapper = mock(UserRoleMapper.class);
        rolePermissionMapper = mock(RolePermissionMapper.class);
        passwordHasher = mock(PasswordHasher.class);
        sessionRepository = mock(SessionRepository.class);
        userConverter = mock(UserConverter.class);
        service = new LocalUserAuthServiceImpl(userMapper, roleMapper, permissionMapper,
                userRoleMapper, rolePermissionMapper, passwordHasher, sessionRepository,
                userConverter, Duration.ofMinutes(30));
    }

    private User user(long id, int status) {
        User u = new User();
        u.setId(id);
        u.setUsername("alice");
        u.setNickname("Alice");
        u.setStatus(status);
        u.setPasswordHash("hash");
        return u;
    }

    @Test
    void register_duplicateUsername_throws() {
        when(userMapper.existsByUsername("alice")).thenReturn(true);

        assertThatThrownBy(() -> service.register("alice", "pwd", "Alice"))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.USERNAME_EXISTS));
        verify(userMapper, never()).save(any(User.class));
    }

    @Test
    void register_success_savesUserAndAssignsDefaultRole() {
        when(userMapper.existsByUsername("alice")).thenReturn(false);
        when(passwordHasher.hash("pwd")).thenReturn("hashed");
        when(roleMapper.selectByCode("USER")).thenReturn(role(2L, "USER"));
        doAnswer(inv -> {
            ((User) inv.getArgument(0)).setId(99L);
            return 1;
        }).when(userMapper).save(any(User.class));
        UserIdentity identity = new UserIdentity(99L, "alice", "Alice", List.of(), List.of(), true);
        when(userConverter.toUserIdentity(any(), any(), any())).thenReturn(identity);

        UserIdentity result = service.register("alice", "pwd", "Alice");

        assertThat(result).isEqualTo(identity);
        verify(userMapper).save(any(User.class));
        verify(userRoleMapper).save(new UserRole(99L, 2L));
    }

    @Test
    void login_success_returnsToken() {
        when(userMapper.selectByUsername("alice")).thenReturn(user(1L, 1));
        when(passwordHasher.matches("pwd", "hash")).thenReturn(true);
        UserIdentity identity = new UserIdentity(1L, "alice", "Alice", List.of(), List.of(), true);
        when(userConverter.toUserIdentity(any(), any(), any())).thenReturn(identity);

        LoginResult r = service.login("alice", "pwd");

        assertThat(r.token()).isNotBlank();
        assertThat(r.user()).isEqualTo(identity);
        verify(sessionRepository).save(eq(r.token()), eq(identity), any());
    }

    @Test
    void login_wrongPassword_throwsLoginFailed() {
        when(userMapper.selectByUsername("alice")).thenReturn(user(1L, 1));
        when(passwordHasher.matches("pwd", "hash")).thenReturn(false);

        assertThatThrownBy(() -> service.login("alice", "pwd"))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.LOGIN_FAILED));
    }

    @Test
    void login_unknownUser_throwsLoginFailed() {
        when(userMapper.selectByUsername("ghost")).thenReturn(null);

        assertThatThrownBy(() -> service.login("ghost", "pwd"))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.LOGIN_FAILED));
    }

    @Test
    void login_disabled_throwsAccountDisabled() {
        when(userMapper.selectByUsername("alice")).thenReturn(user(1L, 0));
        when(passwordHasher.matches("pwd", "hash")).thenReturn(true);

        assertThatThrownBy(() -> service.login("alice", "pwd"))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.ACCOUNT_DISABLED));
    }

    @Test
    void logout_removesSession() {
        service.logout("tok");
        verify(sessionRepository).remove("tok");
    }

    @Test
    void resolve_validToken_returnsIdentity() {
        UserIdentity identity = new UserIdentity(1L, "alice", "Alice", List.of(), List.of(), true);
        when(sessionRepository.find("tok", Duration.ofMinutes(30))).thenReturn(Optional.of(identity));

        assertThat(service.resolve("tok")).isEqualTo(identity);
    }

    @Test
    void resolve_missingToken_throwsTokenInvalid() {
        when(sessionRepository.find("tok", Duration.ofMinutes(30))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.resolve("tok"))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.TOKEN_INVALID));
    }

    @Test
    void resolve_disabledUser_throwsTokenInvalid() {
        UserIdentity identity = new UserIdentity(1L, "alice", "Alice", List.of(), List.of(), false);
        when(sessionRepository.find("tok", Duration.ofMinutes(30))).thenReturn(Optional.of(identity));

        assertThatThrownBy(() -> service.resolve("tok"))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.TOKEN_INVALID));
    }

    private Role role(long id, String code) {
        Role r = new Role();
        r.setId(id);
        r.setCode(code);
        r.setName(code);
        return r;
    }
}
