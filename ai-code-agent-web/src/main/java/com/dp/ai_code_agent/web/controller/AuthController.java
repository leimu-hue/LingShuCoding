package com.dp.ai_code_agent.web.controller;

import com.dp.ai_code_agent.common.result.Result;
import com.dp.ai_code_agent.user.spi.UserAuthService;
import com.dp.ai_code_agent.user.spi.model.LoginResult;
import com.dp.ai_code_agent.user.spi.model.UserIdentity;
import com.dp.ai_code_agent.web.dto.LoginRequest;
import com.dp.ai_code_agent.web.dto.RegisterRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证接口：注册 / 登录 / 注销 / 当前用户。
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final String BEARER_PREFIX = "Bearer ";

    private final UserAuthService userAuthService;

    @PostMapping("/register")
    public Result<UserIdentity> register(@Valid @RequestBody RegisterRequest req) {
        return Result.ok(userAuthService.register(req.username(), req.password(), req.nickname()));
    }

    @PostMapping("/login")
    public Result<LoginResult> login(@Valid @RequestBody LoginRequest req) {
        return Result.ok(userAuthService.login(req.username(), req.password()));
    }

    @PostMapping("/logout")
    public Result<Void> logout(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        String token = authHeader != null && authHeader.startsWith(BEARER_PREFIX)
                ? authHeader.substring(BEARER_PREFIX.length()) : "";
        userAuthService.logout(token);
        return Result.ok();
    }

    @GetMapping("/me")
    public Result<UserIdentity> me(@AuthenticationPrincipal UserIdentity user) {
        return Result.ok(user);
    }
}
