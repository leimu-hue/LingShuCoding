package com.dp.ai_code_agent.common.model;

import cn.xbatis.db.annotations.LogicDelete;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 实体公共字段父类。
 * <p>
 * 所有模块的持久化实体均需继承本类，统一包含审计与逻辑删除字段：
 * <ul>
 *     <li>{@code createdTime}  创建时间</li>
 *     <li>{@code updateTime}   更新时间</li>
 *     <li>{@code createAt}     创建人 ID</li>
 *     <li>{@code updateUserId} 更新人 ID</li>
 *     <li>{@code isDeleted}    逻辑删除标记</li>
 * </ul>
 */
@Data
public abstract class BaseEntity {

    /** 创建时间 */
    private LocalDateTime createdTime;

    /** 更新时间 */
    private LocalDateTime updateTime;

    /** 创建人 ID */
    private Long createAt;

    /** 更新人 ID */
    private Long updateUserId;

    /** 逻辑删除标记 */
    @LogicDelete(beforeValue = "false", afterValue = "true")
    private Boolean isDeleted;
}
