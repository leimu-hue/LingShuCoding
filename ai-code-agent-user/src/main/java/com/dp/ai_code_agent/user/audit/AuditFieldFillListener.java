package com.dp.ai_code_agent.user.audit;

import cn.xbatis.listener.OnInsertListener;
import cn.xbatis.listener.OnUpdateListener;
import com.dp.ai_code_agent.common.model.BaseEntity;
import com.dp.ai_code_agent.user.spi.context.UserContext;

import java.time.LocalDateTime;

/**
 * 审计字段自动填充：xbatis 全局 OnInsert/OnUpdate 监听器。
 * <p>
 * INSERT 填 createdTime/updateTime/createAt/updateUserId；UPDATE 填 updateTime/updateUserId；
 * 无登录态 createAt/updateUserId 填 0（表示系统）。isDeleted 由 {@code @LogicDelete} 处理，此处不碰。
 */
public class AuditFieldFillListener
        implements OnInsertListener<Object>, OnUpdateListener<Object> {

    @Override
    public void onInsert(Object entity) {
        if (entity instanceof BaseEntity base) {
            LocalDateTime now = LocalDateTime.now();
            long userId = currentUserId();
            base.setCreatedTime(now);
            base.setUpdateTime(now);
            base.setCreateAt(userId);
            base.setUpdateUserId(userId);
        }
    }

    @Override
    public void onUpdate(Object entity) {
        if (entity instanceof BaseEntity base) {
            base.setUpdateTime(LocalDateTime.now());
            base.setUpdateUserId(currentUserId());
        }
    }

    private static long currentUserId() {
        Long userId = UserContext.getUserId();
        return userId == null ? 0L : userId;
    }
}
