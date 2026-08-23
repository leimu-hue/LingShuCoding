package com.dp.ai_code_agent.web.controller;

import com.dp.ai_code_agent.user.spi.UserAuthService;
import com.dp.ai_code_agent.user.spi.model.LoginResult;
import com.dp.ai_code_agent.user.spi.model.UserIdentity;
import com.dp.ai_code_agent.user.spi.model.UserRole;
import com.dp.ai_code_agent.web.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthControllerTest {

    UserAuthService userAuthService;
    MockMvc mvc;

    @BeforeEach
    void setUp() {
        userAuthService = mock(UserAuthService.class);
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mvc = MockMvcBuilders.standaloneSetup(new AuthController(userAuthService))
                .setValidator(validator)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void login_ok() throws Exception {
        when(userAuthService.login("alice", "pwd"))
                .thenReturn(new LoginResult("tok", new UserIdentity(1L, "alice", "Alice", UserRole.USER, true)));

        mvc.perform(post("/api/auth/login").contentType(APPLICATION_JSON)
                        .content("{\"username\":\"alice\",\"password\":\"pwd\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.token").value("tok"));
    }

    @Test
    void register_ok() throws Exception {
        when(userAuthService.register("bob", "secret1", "Bob"))
                .thenReturn(new UserIdentity(2L, "bob", "Bob", UserRole.USER, true));

        mvc.perform(post("/api/auth/register").contentType(APPLICATION_JSON)
                        .content("{\"username\":\"bob\",\"password\":\"secret1\",\"nickname\":\"Bob\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("bob"));
    }

    @Test
    void login_invalidRequest_returnsBadRequest() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(APPLICATION_JSON)
                        .content("{\"username\":\"\",\"password\":\"\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
    }
}
