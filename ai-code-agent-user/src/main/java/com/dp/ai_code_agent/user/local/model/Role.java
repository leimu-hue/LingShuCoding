package com.dp.ai_code_agent.user.local.model;

import cn.xbatis.db.annotations.Table;
import cn.xbatis.db.annotations.TableId;
import lombok.Data;

/**
 * 角色实体
 */
@Data
@Table("t_role")
public class Role {

    @TableId
    private Long id;

    private String code;

    private String name;

    private String description;
}
