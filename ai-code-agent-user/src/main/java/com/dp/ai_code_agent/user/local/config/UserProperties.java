package com.dp.ai_code_agent.user.local.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * 用户模块配置项（{@code app.user.*}）。
 *
 * @param provider  实现开关，默认 {@code local}
 * @param sessionTtl 会话过期时间（滑续），默认 30 分钟
 */
@ConfigurationProperties(prefix = "app.user")
public record UserProperties(String provider, Duration sessionTtl) {

    public UserProperties {
        if (provider == null || provider.isBlank()) {
            provider = "local";
        }
        if (sessionTtl == null) {
            sessionTtl = Duration.ofMinutes(30);
        }
    }
}
