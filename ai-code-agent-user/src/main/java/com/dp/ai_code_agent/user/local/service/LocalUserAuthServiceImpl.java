package com.dp.ai_code_agent.user.local.service;

import com.dp.ai_code_agent.common.exception.BusinessException;
import com.dp.ai_code_agent.common.exception.ErrorCode;
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
import com.dp.ai_code_agent.user.spi.UserAuthService;
import com.dp.ai_code_agent.user.spi.model.LoginResult;
import com.dp.ai_code_agent.user.spi.model.PermissionDTO;
import com.dp.ai_code_agent.user.spi.model.RoleDTO;
import com.dp.ai_code_agent.user.spi.model.UserIdentity;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * {@link UserAuthService} 的本地实现：注册 / 登录 / 注销 / resolve。
 */
public class LocalUserAuthServiceImpl implements UserAuthService {

    private static final String DEFAULT_ROLE = "USER";

    private final UserMapper userMapper;
    private final RoleMapper roleMapper;
    private final PermissionMapper permissionMapper;
    private final UserRoleMapper userRoleMapper;
    private final RolePermissionMapper rolePermissionMapper;
    private final PasswordHasher passwordHasher;
    private final SessionRepository sessionRepository;
    private final UserConverter userConverter;
    private final Duration sessionTtl;
    private final SecureRandom secureRandom = new SecureRandom();

    public LocalUserAuthServiceImpl(UserMapper userMapper, RoleMapper roleMapper,
                                    PermissionMapper permissionMapper, UserRoleMapper userRoleMapper,
                                    RolePermissionMapper rolePermissionMapper, PasswordHasher passwordHasher,
                                    SessionRepository sessionRepository, UserConverter userConverter,
                                    Duration sessionTtl) {
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
        this.permissionMapper = permissionMapper;
        this.userRoleMapper = userRoleMapper;
        this.rolePermissionMapper = rolePermissionMapper;
        this.passwordHasher = passwordHasher;
        this.sessionRepository = sessionRepository;
        this.userConverter = userConverter;
        this.sessionTtl = sessionTtl;
    }

    @Override
    @Transactional
    public UserIdentity register(String username, String password, String nickname) {
        if (userMapper.existsByUsername(username)) {
            throw new BusinessException(ErrorCode.USERNAME_EXISTS);
        }
        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(passwordHasher.hash(password));
        user.setNickname(nickname);
        user.setStatus(1);
        LocalDateTime now = LocalDateTime.now();
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        userMapper.save(user);

        Role defaultRole = roleMapper.selectByCode(DEFAULT_ROLE);
        if (defaultRole != null) {
            userRoleMapper.save(new UserRole(user.getId(), defaultRole.getId()));
        }
        return buildIdentity(user);
    }

    @Override
    public LoginResult login(String username, String password) {
        User user = userMapper.selectByUsername(username);
        if (user == null || !passwordHasher.matches(password, user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.LOGIN_FAILED);
        }
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new BusinessException(ErrorCode.ACCOUNT_DISABLED);
        }
        String token = generateToken();
        UserIdentity identity = buildIdentity(user);
        sessionRepository.save(token, identity, sessionTtl);
        return new LoginResult(token, identity);
    }

    @Override
    public void logout(String token) {
        sessionRepository.remove(token);
    }

    @Override
    public UserIdentity resolve(String token) {
        return sessionRepository.find(token, sessionTtl)
                .filter(UserIdentity::enabled)
                .orElseThrow(() -> new BusinessException(ErrorCode.TOKEN_INVALID));
    }

    private String generateToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private UserIdentity buildIdentity(User user) {
        List<Long> roleIds = userRoleMapper.selectByUserId(user.getId()).stream()
                .map(UserRole::getRoleId)
                .distinct()
                .toList();
        List<Role> roles = roleIds.isEmpty() ? List.of() : roleMapper.selectByIds(roleIds);
        List<RolePermission> rps = roleIds.isEmpty() ? List.of() : rolePermissionMapper.selectByRoleIds(roleIds);

        Map<Long, Permission> permById = buildPermissionMap(rps);

        List<RoleDTO> roleDTOs = new ArrayList<>();
        List<PermissionDTO> flatPerms = new ArrayList<>();
        Set<String> seenCodes = new HashSet<>();
        for (Role role : roles) {
            List<PermissionDTO> perms = rps.stream()
                    .filter(rp -> rp.getRoleId().equals(role.getId()))
                    .map(rp -> permById.get(rp.getPermissionId()))
                    .filter(Objects::nonNull)
                    .map(p -> new PermissionDTO(p.getId(), p.getCode(), p.getName()))
                    .toList();
            roleDTOs.add(new RoleDTO(role.getId(), role.getCode(), role.getName(), perms));
            perms.forEach(p -> {
                if (seenCodes.add(p.code())) {
                    flatPerms.add(p);
                }
            });
        }
        return userConverter.toUserIdentity(user, roleDTOs, flatPerms);
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
