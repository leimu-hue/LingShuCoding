package com.dp.ai_code_agent.user.spi;

import com.dp.ai_code_agent.common.result.PageResult;
import com.dp.ai_code_agent.user.spi.model.RoleDTO;
import com.dp.ai_code_agent.user.spi.model.UserAdminDTO;

import java.util.List;

/**
 * 用户管理门面（SPI 契约，零框架依赖），供管理端调用。
 */
public interface UserAdminService {

    /**
     * 分页查询用户，支持关键字（username/nickname 模糊）、状态、角色过滤。
     */
    PageResult<UserAdminDTO> page(int page, int size, String keyword, Integer status, Long roleId);

    /**
     * 用户详情（含角色与权限）。
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

    /**
     * 分配角色（先删后插）。
     */
    void assignRoles(Long id, List<Long> roleIds);

    /**
     * 全部角色列表（含权限码树）。
     */
    List<RoleDTO> listRoles();

    /**
     * 给角色分配权限（先删后插）。
     */
    void grantPermissions(Long roleId, List<Long> permissionIds);
}
