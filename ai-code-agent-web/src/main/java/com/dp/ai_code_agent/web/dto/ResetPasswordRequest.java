package com.dp.ai_code_agent.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 重置密码请求
 */
public record ResetPasswordRequest(
        @NotBlank @Size(min = 6, max = 64) String newPassword) {
}
