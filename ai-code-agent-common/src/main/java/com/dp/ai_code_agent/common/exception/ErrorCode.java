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
    FORBIDDEN(403, "无权限访问"),
    NOT_FOUND(404, "资源不存在"),
    INTERNAL_ERROR(500, "系统内部错误"),

    // 用户模块
    USERNAME_EXISTS(1001, "用户名已存在"),
    LOGIN_FAILED(1002, "用户名或密码错误"),
    ACCOUNT_DISABLED(1003, "账号已被禁用"),
    TOKEN_INVALID(1004, "登录已失效"),
    TOKEN_EXPIRED(1005, "登录已过期");

    private final int code;

    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

}