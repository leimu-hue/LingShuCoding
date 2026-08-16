package com.dp.ai_code_agent.web.dto;

import jakarta.validation.constraints.NotNull;

/**
 * 启用/禁用请求
 */
public record UpdateStatusRequest(
        @NotNull Boolean enabled) {
}
