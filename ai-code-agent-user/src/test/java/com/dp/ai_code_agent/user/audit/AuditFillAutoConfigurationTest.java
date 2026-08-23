package com.dp.ai_code_agent.user.audit;

import cn.xbatis.core.XbatisGlobalConfig;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AuditFillAutoConfigurationTest {

    @Test
    void registersGlobalAuditListeners() {
        new AuditFillAutoConfiguration().registerGlobalAuditListener();

        assertThat(XbatisGlobalConfig.getGlobalOnInsertListener()).isNotNull();
        assertThat(XbatisGlobalConfig.getGlobalOnUpdateListener()).isNotNull();
    }
}
