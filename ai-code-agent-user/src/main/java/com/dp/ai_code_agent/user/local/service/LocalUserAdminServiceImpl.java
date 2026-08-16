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
import com.dp.ai_code_agent.user.local.model.Permission;
import com.dp.ai_code_agent.user.local.model.Role;
import com.dp.ai_code_agent.user.local.model.RolePermission;
import com.dp.ai_code_agent.user.local.model.User;
import com.dp.ai_code_agent.user.local.model.UserRole;
import com.dp.ai_code_agent.user.local.repository.SessionRepository;
import com.dp.ai_code_agent.user.local.security.PasswordHasher;
import com.dp.ai_code_agent.user.spi.UserAdminService;
import com.dp.ai_code_agent.user.spi.model.PermissionDTO;
import com.dp.ai_code_agent.user.spi.model.RoleDTO;
import com.dp.ai_code_agent.user.spi.model.UserAdminDTO;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * {@link UserAdminService} 的本地实现：分页 / 详情 / 启停 / 重置密码 / 分配角色 / 授权。
 */
public class LocalUserAdminServiceImpl implements UserAdminService {

    private final UserMapper userMapper;
    private final RoleMapper roleMapper;
    private final PermissionMapper permissionMapper;
    private final UserRoleMapper userRoleMapper;
    private final RolePermissionMapper rolePermissionMapper;
    private final PasswordHasher passwordHasher;
    private final SessionRepository sessionRepository;
    private final UserConverter userConverter;

    public LocalUserAdminServiceImpl(UserMapper userMapper, RoleMapper roleMapper,
                                     PermissionMapper permissionMapper, UserRoleMapper userRoleMapper,
                                     RolePermissionMapper rolePermissionMapper, PasswordHasher passwordHasher,
                                     SessionRepository sessionRepository, UserConverter userConverter) {
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
        this.permissionMapper = permissionMapper;
        this.userRoleMapper = userRoleMapper;
        this.rolePermissionMapper = rolePermissionMapper;
        this.passwordHasher = passwordHasher;
        this.sessionRepository = sessionRepository;
        this.userConverter = userConverter;
    }

    @Override
    public PageResult<UserAdminDTO> page(int page, int size, String keyword, Integer status, Long roleId) {
        List<Long> userIds = null;
        if (roleId != null) {
            userIds = userRoleMapper.selectByRoleId(roleId).stream()
                    .map(UserRole::getUserId)
                    .toList();
            if (userIds.isEmpty()) {
                return PageResult.of(List.of(), 0, page, size);
            }
        }
        int total = userMapper.countPage(keyword, status, userIds);
        if (total == 0) {
            return PageResult.of(List.of(), 0, page, size);
        }
        int offset = (page - 1) * size;
        List<User> users = userMapper.selectPage(keyword, status, userIds, offset, size);
        List<UserAdminDTO> records = users.stream().map(this::toAdminDTO).toList();
        return PageResult.of(records, total, page, size);
    }

    @Override
    public UserAdminDTO detail(Long id) {
        return toAdminDTO(requireUser(id));
    }

    @Override
    @Transactional
    public void setStatus(Long id, boolean enabled) {
        User user = requireUser(id);
        user.setStatus(enabled ? 1 : 0);
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.update(user);
        if (!enabled) {
            sessionRepository.removeAllByUserId(id);
        }
    }

    @Override
    @Transactional
    public void resetPassword(Long id, String newPassword) {
        User user = requireUser(id);
        user.setPasswordHash(passwordHasher.hash(newPassword));
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.update(user);
        sessionRepository.removeAllByUserId(id);
    }

    @Override
    @Transactional
    public void assignRoles(Long id, List<Long> roleIds) {
        requireUser(id);
        userRoleMapper.deleteByUserId(id);
        roleIds.forEach(roleId -> userRoleMapper.save(new UserRole(id, roleId)));
    }

    @Override
    public List<RoleDTO> listRoles() {
        return loadRoles(roleMapper.listAll());
    }

    @Override
    @Transactional
    public void grantPermissions(Long roleId, List<Long> permissionIds) {
        rolePermissionMapper.deleteByRoleId(roleId);
        permissionIds.forEach(pid -> rolePermissionMapper.save(new RolePermission(roleId, pid)));
    }

    private User requireUser(Long id) {
        User user = userMapper.getById(id);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "用户不存在");
        }
        return user;
    }

    private UserAdminDTO toAdminDTO(User user) {
        return userConverter.toUserAdminDTO(user, loadRolesByUserId(user.getId()));
    }

    private List<RoleDTO> loadRolesByUserId(Long userId) {
        List<Long> roleIds = userRoleMapper.selectByUserId(userId).stream()
                .map(UserRole::getRoleId)
                .toList();
        if (roleIds.isEmpty()) {
            return List.of();
        }
        return loadRoles(roleMapper.selectByIds(roleIds));
    }

    private List<RoleDTO> loadRoles(List<Role> roles) {
        if (roles.isEmpty()) {
            return List.of();
        }
        List<Long> roleIds = roles.stream().map(Role::getId).toList();
        List<RolePermission> rps = rolePermissionMapper.selectByRoleIds(roleIds);
        Map<Long, Permission> permById = buildPermissionMap(rps);
        return roles.stream()
                .map(role -> toRoleDTO(role, rps, permById))
                .toList();
    }

    private RoleDTO toRoleDTO(Role role, List<RolePermission> rps, Map<Long, Permission> permById) {
        List<PermissionDTO> perms = rps.stream()
                .filter(rp -> rp.getRoleId().equals(role.getId()))
                .map(rp -> permById.get(rp.getPermissionId()))
                .filter(Objects::nonNull)
                .map(p -> new PermissionDTO(p.getId(), p.getCode(), p.getName()))
                .toList();
        return new RoleDTO(role.getId(), role.getCode(), role.getName(), perms);
    }

    private Map<Long, Permission> buildPermissionMap(List<RolePermission> rps) {
        List<Long> permIds = rps.stream().map(RolePermission::getPermissionId).distinct().toList();
        if (permIds.isEmpty()) {
            return Map.of();
        }
        return permissionMapper.selectByIds(permIds).stream()
                .collect(Collectors.toMap(Permission::getId, Function.identity()));
    }
}
