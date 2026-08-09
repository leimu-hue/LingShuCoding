package com.dp.ai_code_agent.web.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * 全局跨域配置项
 *
 * @param allowedOriginPatterns 允许的来源匹配模式
 * @param allowedMethods        允许的请求方法
 * @param allowedHeaders        允许的请求头
 * @param allowCredentials      是否允许携带凭证
 * @param maxAge                预检请求缓存时间（秒）
 */
@ConfigurationProperties(prefix = "web.cors")
public record CorsProperties(
        List<String> allowedOriginPatterns,
        List<String> allowedMethods,
        List<String> allowedHeaders,
        boolean allowCredentials,
        long maxAge
) {

    public CorsProperties {
        if (allowedOriginPatterns == null || allowedOriginPatterns.isEmpty()) {
            allowedOriginPatterns = List.of("*");
        }
        if (allowedMethods == null || allowedMethods.isEmpty()) {
            allowedMethods = List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS");
        }
        if (allowedHeaders == null || allowedHeaders.isEmpty()) {
            allowedHeaders = List.of("*");
        }
        if (maxAge <= 0) {
            maxAge = 3600;
        }
    }
}