package com.dp.ai_code_agent.user.spi.model;

/**
 * 登录结果（零框架依赖）
 *
 * @param token 不透明会话令牌
 * @param user  登录用户身份
 */
public record LoginResult(String token, UserIdentity user) {
}
