package com.dp.ai_code_agent.user.local.config;

import com.dp.ai_code_agent.user.local.converter.UserConverter;
import com.dp.ai_code_agent.user.local.mapper.PermissionMapper;
import com.dp.ai_code_agent.user.local.mapper.RoleMapper;
import com.dp.ai_code_agent.user.local.mapper.RolePermissionMapper;
import com.dp.ai_code_agent.user.local.mapper.UserMapper;
import com.dp.ai_code_agent.user.local.mapper.UserRoleMapper;
import com.dp.ai_code_agent.user.local.repository.SessionRepository;
import com.dp.ai_code_agent.user.local.security.PasswordHasher;
import com.dp.ai_code_agent.user.spi.UserAdminService;
import com.dp.ai_code_agent.user.spi.UserAuthService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class LocalUserProviderAutoConfigurationTest {

    private ApplicationContextRunner runner() {
        return new ApplicationContextRunner()
                .withUserConfiguration(LocalUserProviderAutoConfiguration.class)
                .withBean(UserMapper.class, () -> mock(UserMapper.class))
                .withBean(RoleMapper.class, () -> mock(RoleMapper.class))
                .withBean(PermissionMapper.class, () -> mock(PermissionMapper.class))
                .withBean(UserRoleMapper.class, () -> mock(UserRoleMapper.class))
                .withBean(RolePermissionMapper.class, () -> mock(RolePermissionMapper.class))
                .withBean(PasswordHasher.class, PasswordHasher::new)
                .withBean(SessionRepository.class, () -> mock(SessionRepository.class))
                .withBean(UserConverter.class, () -> mock(UserConverter.class));
    }

    @Test
    void defaultProvider_registersBothBeans() {
        runner().run(ctx -> {
            assertThat(ctx).hasSingleBean(UserAuthService.class);
            assertThat(ctx).hasSingleBean(UserAdminService.class);
        });
    }

    @Test
    void nonLocalProvider_doesNotRegisterBeans() {
        runner().withPropertyValues("app.user.provider=keycloak").run(ctx -> {
            assertThat(ctx).doesNotHaveBean(UserAuthService.class);
            assertThat(ctx).doesNotHaveBean(UserAdminService.class);
        });
    }

    @Test
    void externalUserAuthService_overridesLocal() {
        UserAuthService external = mock(UserAuthService.class);
        runner().withBean(UserAuthService.class, () -> external).run(ctx -> {
            assertThat(ctx).hasSingleBean(UserAuthService.class);
            assertThat(ctx.getBean(UserAuthService.class)).isSameAs(external);
        });
    }

    @Test
    void externalUserAdminService_overridesLocal() {
        UserAdminService external = mock(UserAdminService.class);
        runner().withBean(UserAdminService.class, () -> external).run(ctx -> {
            assertThat(ctx).hasSingleBean(UserAdminService.class);
            assertThat(ctx.getBean(UserAdminService.class)).isSameAs(external);
        });
    }
}
