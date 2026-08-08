package com.dp.ai_code_agent.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.mybatis.spring.annotation.MapperScan;

/**
 * 应用入口
 */
@SpringBootApplication(scanBasePackages = "com.dp.ai_code_agent")
@MapperScan("com.dp.ai_code_agent.core.mapper")
public class AiCodeAgentApplication {

    static void main(String[] args) {
        SpringApplication.run(AiCodeAgentApplication.class, args);
    }
}