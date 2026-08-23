package com.dp.ai_code_agent.web.config;

import com.dp.ai_code_agent.user.spi.UserAuthService;
import com.dp.ai_code_agent.web.security.RestAccessDeniedHandler;
import com.dp.ai_code_agent.web.security.RestAuthenticationEntryPoint;
import com.dp.ai_code_agent.web.security.TokenAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import tools.jackson.databind.ObjectMapper;

/**
 * Spring Security 配置：无状态 Bearer 鉴权链路。
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http, UserAuthService userAuthService,
                                    ObjectMapper objectMapper) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/api/auth/register", "/api/auth/login").permitAll()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(e -> e
                        .authenticationEntryPoint(new RestAuthenticationEntryPoint(objectMapper))
                        .accessDeniedHandler(new RestAccessDeniedHandler(objectMapper)))
                .addFilterBefore(new TokenAuthenticationFilter(userAuthService),
                        UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    /**
     * 本系统采用无状态 Bearer Token + {@link UserAuthService} SPI 鉴权，不走表单登录，
     * 因此不提供基于用户名/密码的 UserDetails 加载逻辑。
     * <p>
     * 此处声明空实现仅为满足 {@code UserDetailsServiceAutoConfiguration} 的
     * {@code @ConditionalOnMissingBean} 条件，避免 Spring Boot 自动生成默认内存用户
     * （user + 随机密码）并在启动时打印 warning。
     */
    @Bean
    UserDetailsService userDetailsService() {
        return username -> {
            throw new UsernameNotFoundException(
                    "Authentication is delegated to UserAuthService SPI, not UserDetailsService");
        };
    }
}
