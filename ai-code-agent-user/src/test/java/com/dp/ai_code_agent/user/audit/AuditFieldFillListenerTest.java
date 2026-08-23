package com.dp.ai_code_agent.user.audit;

import com.dp.ai_code_agent.common.model.BaseEntity;
import com.dp.ai_code_agent.user.spi.context.UserContext;
import com.dp.ai_code_agent.user.spi.model.UserIdentity;
import com.dp.ai_code_agent.user.spi.model.UserRole;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AuditFieldFillListenerTest {

    private static final UserIdentity ALICE =
            new UserIdentity(1L, "alice", "Alice", UserRole.USER, true);

    private final AuditFieldFillListener listener = new AuditFieldFillListener();

    @Test
    void onInsert_withoutLogin_fillsTimeAndZeroUser() {
        BaseEntity entity = new BaseEntity() {};
        listener.onInsert(entity);
        assertThat(entity.getCreatedTime()).isNotNull();
        assertThat(entity.getUpdateTime()).isNotNull();
        assertThat(entity.getCreateAt()).isEqualTo(0L);
        assertThat(entity.getUpdateUserId()).isEqualTo(0L);
    }

    @Test
    void onInsert_withLogin_fillsCurrentUserId() throws Exception {
        BaseEntity entity = new BaseEntity() {};
        UserContext.scoped(ALICE, () -> {
            listener.onInsert(entity);
            return null;
        });
        assertThat(entity.getCreateAt()).isEqualTo(1L);
        assertThat(entity.getUpdateUserId()).isEqualTo(1L);
    }

    @Test
    void onUpdate_fillsTimeAndCurrentUserId() throws Exception {
        BaseEntity entity = new BaseEntity() {};
        UserContext.scoped(ALICE, () -> {
            listener.onUpdate(entity);
            return null;
        });
        assertThat(entity.getUpdateTime()).isNotNull();
        assertThat(entity.getUpdateUserId()).isEqualTo(1L);
    }

    @Test
    void onUpdate_withoutLogin_fillsZeroUser() {
        BaseEntity entity = new BaseEntity() {};
        listener.onUpdate(entity);
        assertThat(entity.getUpdateTime()).isNotNull();
        assertThat(entity.getUpdateUserId()).isEqualTo(0L);
    }

    @Test
    void ignoresNonBaseEntity() {
        listener.onInsert(new Object());
        listener.onUpdate(new Object());
    }
}
