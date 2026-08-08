package com.dp.ai_code_agent.common.exception;

import lombok.Getter;

/**
 * 业务错误码
 */
@Getter
public enum ErrorCode {

    SUCCESS(0, "成功"),
    BAD_REQUEST(400, "参数错误"),
    UNAUTHORIZED(401, "未授权"),
    NOT_FOUND(404, "资源不存在"),
    INTERNAL_ERROR(500, "系统内部错误");

    private final int code;

    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

}