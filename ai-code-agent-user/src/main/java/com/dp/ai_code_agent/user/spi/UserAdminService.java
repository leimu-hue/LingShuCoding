package com.dp.ai_code_agent.user.spi;

import com.dp.ai_code_agent.common.result.PageResult;
import com.dp.ai_code_agent.user.spi.model.UserAdminDTO;
import com.dp.ai_code_agent.user.spi.model.UserRole;

/**
 * 用户管理门面（SPI 契约，零框架依赖），供管理端调用。
 * <p>
 * 本地实现仅提供用户维度的最小管理能力；角色/权限等复杂能力后续经第三方权限管理 SPI 接入。
 */
public interface UserAdminService {

    /**
     * 分页查询用户，支持关键字（username/nickname 模糊）、状态、角色过滤。
     */
    PageResult<UserAdminDTO> page(int page, int size, String keyword, Integer status, UserRole userRole);

    /**
     * 用户详情。
     */
    UserAdminDTO detail(Long id);

    /**
     * 启用/禁用用户；禁用时级联清空其全部会话（踢人）。
     */
    void setStatus(Long id, boolean enabled);

    /**
     * 重置密码（BCrypt 后落库），并级联清空会话。
     */
    void resetPassword(Long id, String newPassword);
}
