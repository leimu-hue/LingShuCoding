package com.dp.ai_code_agent.user.local.config;

import com.dp.ai_code_agent.user.local.converter.UserConverter;
import com.dp.ai_code_agent.user.local.mapper.PermissionMapper;
import com.dp.ai_code_agent.user.local.mapper.RoleMapper;
import com.dp.ai_code_agent.user.local.mapper.RolePermissionMapper;
import com.dp.ai_code_agent.user.local.mapper.UserMapper;
import com.dp.ai_code_agent.user.local.mapper.UserRoleMapper;
import com.dp.ai_code_agent.user.local.repository.SessionRepository;
import com.dp.ai_code_agent.user.local.security.PasswordHasher;
import com.dp.ai_code_agent.user.local.service.LocalUserAdminServiceImpl;
import com.dp.ai_code_agent.user.local.service.LocalUserAuthServiceImpl;
import com.dp.ai_code_agent.user.spi.UserAdminService;
import com.dp.ai_code_agent.user.spi.UserAuthService;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * 本地用户 provider 自动装配。
 * <p>
 * 通过 {@code app.user.provider=local}（默认）启用；存在外部 {@link UserAuthService}/{@link UserAdminService}
 * 实现时自动退位（{@link ConditionalOnMissingBean}）。
 */
@AutoConfiguration
@EnableConfigurationProperties(UserProperties.class)
@ConditionalOnProperty(name = "app.user.provider", havingValue = "local", matchIfMissing = true)
public class LocalUserProviderAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(UserAuthService.class)
    UserAuthService userAuthService(UserMapper userMapper, RoleMapper roleMapper, PermissionMapper permissionMapper,
                                    UserRoleMapper userRoleMapper, RolePermissionMapper rolePermissionMapper,
                                    PasswordHasher passwordHasher, SessionRepository sessionRepository,
                                    UserConverter userConverter, UserProperties properties) {
        return new LocalUserAuthServiceImpl(userMapper, roleMapper, permissionMapper, userRoleMapper,
                rolePermissionMapper, passwordHasher, sessionRepository, userConverter, properties.sessionTtl());
    }

    @Bean
    @ConditionalOnMissingBean(UserAdminService.class)
    UserAdminService userAdminService(UserMapper userMapper, RoleMapper roleMapper, PermissionMapper permissionMapper,
                                      UserRoleMapper userRoleMapper, RolePermissionMapper rolePermissionMapper,
                                      PasswordHasher passwordHasher, SessionRepository sessionRepository,
                                      UserConverter userConverter) {
        return new LocalUserAdminServiceImpl(userMapper, roleMapper, permissionMapper, userRoleMapper,
                rolePermissionMapper, passwordHasher, sessionRepository, userConverter);
    }
}
