package com.dp.ai_code_agent.user.spi.context;

import com.dp.ai_code_agent.user.spi.model.UserIdentity;
import com.dp.ai_code_agent.user.spi.model.UserRole;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserContextTest {

    private static final UserIdentity ALICE =
            new UserIdentity(1L, "alice", "Alice", UserRole.USER, true);

    @Test
    void get_returnsNullWhenUnbound() {
        assertThat(UserContext.get()).isNull();
        assertThat(UserContext.getUserId()).isNull();
        assertThat(UserContext.getUsername()).isNull();
    }

    @Test
    void scoped_bindsIdentityWithinScopeAndClearsAfter() throws Exception {
        UserIdentity inside = UserContext.scoped(ALICE, () -> UserContext.get());
        assertThat(inside).isEqualTo(ALICE);
        assertThat(UserContext.get()).isNull();
    }

    @Test
    void scoped_exposesUserIdAndUsername() throws Exception {
        assertThat(UserContext.scoped(ALICE, () -> UserContext.getUserId())).isEqualTo(1L);
        assertThat(UserContext.scoped(ALICE, () -> UserContext.getUsername())).isEqualTo("alice");
    }
}
