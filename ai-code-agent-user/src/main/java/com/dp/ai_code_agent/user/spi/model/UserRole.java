package com.dp.ai_code_agent.user.spi.model;

/**
 * 用户角色（零框架依赖）。
 * <p>
 * 本地实现仅做最小验证：{@link #ADMIN} 拥有全部后台权限，{@link #USER} 为普通用户。
 * 复杂权限后续通过第三方权限管理以 SPI 方式接入，本地不展开。
 */
public enum UserRole {

    /** 管理员 */
    ADMIN,

    /** 普通用户 */
    USER
}
