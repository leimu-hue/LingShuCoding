package com.dp.ai_code_agent.common.util;

import java.security.SecureRandom;
import java.util.Base64;

/**
 * 随机令牌生成工具。
 */
public final class TokenGenerator {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private TokenGenerator() {
    }

    /**
     * 生成 URL 安全的随机令牌（32 字节，Base64 URL 编码、无填充）。
     */
    public static String generate() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
