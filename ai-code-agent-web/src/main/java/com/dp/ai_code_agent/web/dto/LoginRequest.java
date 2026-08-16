package com.dp.ai_code_agent.web.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 登录请求
 */
public record LoginRequest(
        @NotBlank String username,
        @NotBlank String password) {
}
