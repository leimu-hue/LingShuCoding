package com.dp.ai_code_agent.web.dto;

import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * 分配角色请求
 */
public record AssignRolesRequest(
        @NotNull List<@NotNull Long> roleIds) {
}
