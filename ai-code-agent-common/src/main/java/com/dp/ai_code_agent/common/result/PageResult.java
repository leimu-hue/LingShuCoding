package com.dp.ai_code_agent.common.result;

import java.util.List;

/**
 * 分页统一响应体
 */
public record PageResult<T>(List<T> records, long total, long pageNum, long pageSize, long pages) {

    public static <T> PageResult<T> of(List<T> records, long total, long pageNum, long pageSize) {
        long pages = pageSize <= 0 ? 0 : (total + pageSize - 1) / pageSize;
        return new PageResult<>(records, total, pageNum, pageSize, pages);
    }
}