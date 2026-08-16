package com.dp.ai_code_agent.user.spi.model;

import java.util.List;

/**
 * 角色 DTO（零框架依赖）
 *
 * @param id          角色 ID
 * @param code        角色码，如 {@code ADMIN}
 * @param name        角色名称
 * @param permissions 角色拥有的权限码列表
 */
public record RoleDTO(Long id, String code, String name, List<PermissionDTO> permissions) {
}
