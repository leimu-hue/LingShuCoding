package com.dp.ai_code_agent.user.spi.model;

/**
 * 权限码 DTO（零框架依赖）
 *
 * @param id   权限 ID
 * @param code 权限码，如 {@code user:create}
 * @param name 权限名称
 */
public record PermissionDTO(Long id, String code, String name) {
}
