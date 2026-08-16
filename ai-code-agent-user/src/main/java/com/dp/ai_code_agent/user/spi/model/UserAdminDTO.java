package com.dp.ai_code_agent.user.spi.model;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户管理视图 DTO（零框架依赖），web 层直接复用。
 *
 * @param id        用户 ID
 * @param username  用户名
 * @param nickname  昵称
 * @param enabled   是否启用
 * @param createdAt 创建时间
 * @param roles     角色列表
 */
public record UserAdminDTO(Long id, String username, String nickname, boolean enabled,
                           LocalDateTime createdAt, List<RoleDTO> roles) {
}
