package com.dp.ai_code_agent.common.result;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统一响应体
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Result<T> {

    private int code;

    private String message;

    private T data;

    public static <T> Result<T> ok() {
        return new Result<>(0, "OK", null);
    }

    public static <T> Result<T> ok(T data) {
        return new Result<>(0, "OK", data);
    }

    public static <T> Result<T> fail(int code, String message) {
        return new Result<>(code, message, null);
    }
}