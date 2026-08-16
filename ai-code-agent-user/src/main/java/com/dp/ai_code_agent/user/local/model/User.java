package com.dp.ai_code_agent.user.local.model;

import cn.xbatis.db.annotations.Table;
import cn.xbatis.db.annotations.TableId;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户实体
 */
@Data
@Table("t_user")
public class User {

    @TableId
    private Long id;

    private String username;

    private String passwordHash;

    private String nickname;

    /** 1 正常 0 禁用 */
    private Integer status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
