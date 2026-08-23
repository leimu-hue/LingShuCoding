package com.dp.ai_code_agent.user.local.service;

import com.dp.ai_code_agent.common.exception.BusinessException;
import com.dp.ai_code_agent.common.exception.ErrorCode;
import com.dp.ai_code_agent.common.util.TokenGenerator;
import com.dp.ai_code_agent.user.local.converter.UserConverter;
import com.dp.ai_code_agent.user.local.mapper.UserMapper;
import com.dp.ai_code_agent.user.local.model.User;
import com.dp.ai_code_agent.user.local.repository.SessionRepository;
import com.dp.ai_code_agent.user.local.security.PasswordHasher;
import com.dp.ai_code_agent.user.spi.UserAuthService;
import com.dp.ai_code_agent.user.spi.model.LoginResult;
import com.dp.ai_code_agent.user.spi.model.UserIdentity;
import com.dp.ai_code_agent.user.spi.model.UserRole;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * {@link UserAuthService} 的本地实现：注册 / 登录 / 注销 / resolve。
 * <p>
 * 本地仅做最小权限验证，注册用户默认角色为 {@link UserRole#USER}。
 */
public class LocalUserAuthServiceImpl implements UserAuthService {

    private final UserMapper userMapper;
    private final PasswordHasher passwordHasher;
    private final SessionRepository sessionRepository;
    private final UserConverter userConverter;
    private final Duration sessionTtl;

    public LocalUserAuthServiceImpl(UserMapper userMapper, PasswordHasher passwordHasher,
                                    SessionRepository sessionRepository, UserConverter userConverter,
                                    Duration sessionTtl) {
        this.userMapper = userMapper;
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
        user.setNickname(nickname == null ? "" : nickname);
        user.setStatus(1);
        user.setUserRole(UserRole.USER);
        user.setIsDeleted(false);
        user.setCreateAt(0L);
        user.setUpdateUserId(0L);
        LocalDateTime now = LocalDateTime.now();
        user.setCreatedTime(now);
        user.setUpdateTime(now);
        userMapper.save(user);
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
        String token = TokenGenerator.generate();
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
                .filter(u -> u.enabled())
                .orElseThrow(() -> new BusinessException(ErrorCode.TOKEN_INVALID));
    }

    private UserIdentity buildIdentity(User user) {
        return userConverter.toUserIdentity(user);
    }
}
