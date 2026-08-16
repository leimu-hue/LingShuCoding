package com.dp.ai_code_agent.user.local.model;

import cn.xbatis.db.IdAutoType;
import cn.xbatis.db.annotations.Table;
import cn.xbatis.db.annotations.TableId;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 角色-权限关联实体（联合主键，业务查询主键取 roleId）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table("t_role_permission")
public class RolePermission {

    @TableId(IdAutoType.NONE)
    private Long roleId;

    private Long permissionId;
}
