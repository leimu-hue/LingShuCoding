package com.dp.ai_code_agent.web.dto;

import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * 角色授权请求
 */
public record GrantPermissionsRequest(
        @NotNull List<@NotNull Long> permissionIds) {
}
