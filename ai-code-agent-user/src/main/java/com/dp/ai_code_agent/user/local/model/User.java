package com.dp.ai_code_agent.user.local.model;

import cn.xbatis.db.annotations.Table;
import cn.xbatis.db.annotations.TableId;
import com.dp.ai_code_agent.common.model.BaseEntity;
import com.dp.ai_code_agent.user.spi.model.UserRole;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户实体。
 * <p>
 * 本地实现仅维护 {@link #userRole} 做最小权限区分，复杂权限后续经 SPI 接入第三方权限管理。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Table("t_user")
public class User extends BaseEntity {

    @TableId
    private Long id;

    private String username;

    private String passwordHash;

    private String nickname;

    /** 用户角色：管理员 / 普通用户 */
    private UserRole userRole;

    /** 1 正常 0 禁用 */
    private Integer status;
}
