package com.dp.ai_code_agent.user.spi;

import com.dp.ai_code_agent.user.spi.model.LoginResult;
import com.dp.ai_code_agent.user.spi.model.UserIdentity;

/**
 * 用户认证门面（SPI 契约，零框架依赖）。
 * <p>
 * web 层仅依赖本接口，具体实现由 {@code app.user.provider} 开关决定（默认为 local）。
 */
public interface UserAuthService {

    /**
     * 注册新用户，用户名唯一，默认分配 {@code USER} 角色。
     */
    UserIdentity register(String username, String password, String nickname);

    /**
     * 登录，成功返回不透明 token 与用户身份。
     */
    LoginResult login(String username, String password);

    /**
     * 注销当前 token（单设备）。
     */
    void logout(String token);

    /**
     * 解析 token 为当前用户身份，token 无效/过期/账号禁用时抛业务异常。
     */
    UserIdentity resolve(String token);
}
