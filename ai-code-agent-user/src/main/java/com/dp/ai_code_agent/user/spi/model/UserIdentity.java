package com.dp.ai_code_agent.user.spi.model;

/**
 * 用户身份（零框架依赖），作为 Spring Security 的 principal 使用。
 *
 * @param id       用户 ID
 * @param username 用户名
 * @param nickname 昵称
 * @param userRole 用户角色（管理员 / 普通用户）
 * @param enabled  是否启用
 */
public record UserIdentity(Long id, String username, String nickname,
                           UserRole userRole, boolean enabled) {
}
