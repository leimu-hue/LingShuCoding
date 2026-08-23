package com.dp.ai_code_agent.web.security;

import com.dp.ai_code_agent.common.exception.BusinessException;
import com.dp.ai_code_agent.common.exception.ErrorCode;
import com.dp.ai_code_agent.user.spi.UserAuthService;
import com.dp.ai_code_agent.user.spi.context.UserContext;
import com.dp.ai_code_agent.user.spi.model.UserIdentity;
import com.dp.ai_code_agent.user.spi.model.UserRole;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TokenAuthenticationFilterTest {

    private static final UserIdentity ALICE =
            new UserIdentity(1L, "alice", "Alice", UserRole.USER, true);

    private final UserAuthService userAuthService = mock(UserAuthService.class);
    private final TokenAuthenticationFilter filter = new TokenAuthenticationFilter(userAuthService);

    @Test
    void validToken_bindsUserContextDuringFilterAndClearsAfter() throws Exception {
        when(userAuthService.resolve("tok")).thenReturn(ALICE);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer tok");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = (req, res) -> assertThat(UserContext.get()).isEqualTo(ALICE);

        filter.doFilter(request, response, chain);

        assertThat(UserContext.get()).isNull();
    }

    @Test
    void invalidToken_doesNotBindUserContext() throws Exception {
        when(userAuthService.resolve("tok")).thenThrow(new BusinessException(ErrorCode.TOKEN_INVALID));
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer tok");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = (req, res) -> assertThat(UserContext.get()).isNull();

        filter.doFilter(request, response, chain);
    }

    @Test
    void missingToken_doesNotBindUserContext() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = (req, res) -> assertThat(UserContext.get()).isNull();

        filter.doFilter(request, response, chain);
    }
}
