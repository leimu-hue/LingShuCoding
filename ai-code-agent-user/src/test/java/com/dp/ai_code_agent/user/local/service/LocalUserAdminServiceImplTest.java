package com.dp.ai_code_agent.user.local.service;

import com.dp.ai_code_agent.common.exception.BusinessException;
import com.dp.ai_code_agent.common.exception.ErrorCode;
import com.dp.ai_code_agent.common.result.PageResult;
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
import com.dp.ai_code_agent.user.spi.model.RoleDTO;
import com.dp.ai_code_agent.user.spi.model.UserAdminDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LocalUserAdminServiceImplTest {

    UserMapper userMapper;
    RoleMapper roleMapper;
    PermissionMapper permissionMapper;
    UserRoleMapper userRoleMapper;
    RolePermissionMapper rolePermissionMapper;
    PasswordHasher passwordHasher;
    SessionRepository sessionRepository;
    UserConverter userConverter;

    LocalUserAdminServiceImpl service;

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
        service = new LocalUserAdminServiceImpl(userMapper, roleMapper, permissionMapper,
                userRoleMapper, rolePermissionMapper, passwordHasher, sessionRepository, userConverter);
    }

    private User user(long id, int status) {
        User u = new User();
        u.setId(id);
        u.setUsername("alice");
        u.setNickname("Alice");
        u.setStatus(status);
        u.setPasswordHash("hash");
        u.setCreatedAt(LocalDateTime.now());
        return u;
    }

    @Test
    void setStatus_disable_cascadesSessionRemoval() {
        when(userMapper.getById(1L)).thenReturn(user(1L, 1));

        service.setStatus(1L, false);

        verify(userMapper).update(any(User.class));
        verify(sessionRepository).removeAllByUserId(1L);
    }

    @Test
    void setStatus_enable_doesNotClearSessions() {
        when(userMapper.getById(1L)).thenReturn(user(1L, 0));

        service.setStatus(1L, true);

        verify(userMapper).update(any(User.class));
        verify(sessionRepository, org.mockito.Mockito.never()).removeAllByUserId(1L);
    }

    @Test
    void assignRoles_deletesThenInserts() {
        when(userMapper.getById(1L)).thenReturn(user(1L, 1));
        service.assignRoles(1L, List.of(2L, 3L));

        var inOrder = inOrder(userRoleMapper);
        inOrder.verify(userRoleMapper).deleteByUserId(1L);
        inOrder.verify(userRoleMapper).save(new UserRole(1L, 2L));
        inOrder.verify(userRoleMapper).save(new UserRole(1L, 3L));
    }

    @Test
    void resetPassword_updatesHashAndClearsSessions() {
        when(userMapper.getById(1L)).thenReturn(user(1L, 1));
        when(passwordHasher.hash("newpwd")).thenReturn("newhash");

        service.resetPassword(1L, "newpwd");

        verify(passwordHasher).hash("newpwd");
        verify(userMapper).update(any(User.class));
        verify(sessionRepository).removeAllByUserId(1L);
    }

    @Test
    void page_returnsPageResult() {
        when(userMapper.countPage("al", 1, null)).thenReturn(1);
        User u = user(1L, 1);
        when(userMapper.selectPage("al", 1, null, 0, 10)).thenReturn(List.of(u));
        UserAdminDTO dto = new UserAdminDTO(1L, "alice", "Alice", true, u.getCreatedAt(), List.of());
        when(userConverter.toUserAdminDTO(eq(u), any())).thenReturn(dto);

        PageResult<UserAdminDTO> result = service.page(1, 10, "al", 1, null);

        assertThat(result.total()).isEqualTo(1);
        assertThat(result.records()).containsExactly(dto);
    }

    @Test
    void detail_notFound_throws() {
        when(userMapper.getById(99L)).thenReturn(null);

        assertThatThrownBy(() -> service.detail(99L))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND));
    }

    @Test
    void listRoles_returnsRolesWithPermissions() {
        Role admin = new Role();
        admin.setId(1L);
        admin.setCode("ADMIN");
        admin.setName("管理员");
        when(roleMapper.listAll()).thenReturn(List.of(admin));

        List<RoleDTO> roles = service.listRoles();

        assertThat(roles).hasSize(1);
        assertThat(roles.get(0).code()).isEqualTo("ADMIN");
    }
}
