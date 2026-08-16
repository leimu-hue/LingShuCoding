package com.dp.ai_code_agent.user.spi.model;

import java.util.List;

/**
 * 用户身份（零框架依赖），作为 Spring Security 的 principal 使用。
 *
 * @param id          用户 ID
 * @param username    用户名
 * @param nickname    昵称
 * @param roles       角色列表
 * @param permissions 权限码扁平列表
 * @param enabled     是否启用
 */
public record UserIdentity(Long id, String username, String nickname,
                           List<RoleDTO> roles, List<PermissionDTO> permissions,
                           boolean enabled) {
}
