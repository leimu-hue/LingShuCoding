package com.dp.ai_code_agent.web.security;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * 架构守卫：守护模块依赖方向。
 * <ul>
 *     <li>web 层禁止依赖 user.local（只能依赖 spi 契约）</li>
 *     <li>user.spi 契约禁止依赖任何框架类（Spring/xbatis/redis/jackson/lombok/mapstruct）</li>
 * </ul>
 */
class ArchitectureGuardTest {

    private final JavaClasses classes = new ClassFileImporter().importPackages("com.dp.ai_code_agent");

    @Test
    void webMustNotDependOnUserLocal() {
        ArchRule rule = noClasses().that().resideInAPackage("com.dp.ai_code_agent.web..")
                .should().dependOnClassesThat().resideInAnyPackage("com.dp.ai_code_agent.user.local..")
                .because("web 层只能依赖 spi 契约，禁止触碰本地实现");
        rule.check(classes);
    }

    @Test
    void spiMustBeFrameworkFree() {
        ArchRule rule = noClasses().that().resideInAPackage("com.dp.ai_code_agent.user.spi..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "org.springframework..", "cn.xbatis..", "org.apache.ibatis..",
                        "redis.clients..", "io.lettuce.core..", "tools.jackson..",
                        "com.fasterxml.jackson..", "lombok..", "org.mapstruct..")
                .because("SPI 契约必须保持零框架依赖");
        rule.check(classes);
    }
}
