package com.dp.ai_code_agent.user.audit;

import cn.xbatis.core.XbatisGlobalConfig;
import jakarta.annotation.PostConstruct;
import org.springframework.boot.autoconfigure.AutoConfiguration;

/**
 * 审计字段自动填充自动装配：启动时向 xbatis 注册全局 OnInsert/OnUpdate 监听器。
 * <p>
 * 与认证 provider 无关（不依赖 app.user.provider），始终生效；全局监听器 set-once，
 * 且需在首个实体操作前注册（无启动期实体操作，故 @PostConstruct 时机安全）。
 */
@AutoConfiguration
public class AuditFillAutoConfiguration {

    @PostConstruct
    void registerGlobalAuditListener() {
        XbatisGlobalConfig.setGlobalOnInsertListener(new AuditFieldFillListener());
        XbatisGlobalConfig.setGlobalOnUpdateListener(new AuditFieldFillListener());
    }
}
