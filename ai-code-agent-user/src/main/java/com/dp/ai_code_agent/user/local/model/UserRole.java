package com.dp.ai_code_agent.user.local.model;

import cn.xbatis.db.IdAutoType;
import cn.xbatis.db.annotations.Table;
import cn.xbatis.db.annotations.TableId;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户-角色关联实体（联合主键，业务查询主键取 userId）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table("t_user_role")
public class UserRole {

    @TableId(IdAutoType.NONE)
    private Long userId;

    private Long roleId;
}
