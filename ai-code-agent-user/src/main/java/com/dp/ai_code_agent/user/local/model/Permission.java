package com.dp.ai_code_agent.user.local.model;

import cn.xbatis.db.annotations.Table;
import cn.xbatis.db.annotations.TableId;
import lombok.Data;

/**
 * 权限实体
 */
@Data
@Table("t_permission")
public class Permission {

    @TableId
    private Long id;

    private String code;

    private String name;

    private String description;
}
