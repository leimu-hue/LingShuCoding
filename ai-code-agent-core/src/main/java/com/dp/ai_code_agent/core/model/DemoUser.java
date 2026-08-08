package com.dp.ai_code_agent.core.model;

import cn.xbatis.db.annotations.Table;
import cn.xbatis.db.annotations.TableId;
import lombok.Data;

/**
 * 示例实体：用户表
 */
@Data
@Table("t_demo_user")
public class DemoUser {

    @TableId
    private Long id;

    private String name;

    private Integer age;
}