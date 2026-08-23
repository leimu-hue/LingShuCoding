# 审计字段自动填充 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: 使用 `executing-plans`（本项目未引入 subagent-driven-development，一律走 Inline Execution）。步骤用 `- [ ]` checkbox 跟踪。
> 对应 spec：`docs/superpowers/specs/2026-08-23-audit-field-fill-design.md`

**Goal:** 建立 `UserContext`（ScopedValue）统一当前用户上下文，并借 xbatis 全局 `OnInsert/OnUpdate` 监听器自动填充 `BaseEntity` 审计字段，`isDeleted` 迁移 `@LogicDelete`，业务代码不再手写审计字段。

**Architecture:** `TokenAuthenticationFilter` 在请求边界用 `UserContext.scoped(...)` 绑定身份；xbatis 在 `save`/`update` 落库时回调全局监听器 `AuditFieldFillListener`，从 `UserContext` 取当前用户 ID 填充 `createdTime/updateTime/createAt/updateUserId`（无登录填 0）；`@LogicDelete` 让查询自动过滤、删除转 update。

**Tech Stack:** Java 25、Spring Boot 4.1.0、xbatis 1.10.6-spring-boot4、JDK `ScopedValue`、JUnit5 + AssertJ + Mockito。

## Global Constraints

- 包路径 `com.dp.ai_code_agent.<module>`（下划线，无连字符）。
- `user-spi` 必须零框架依赖（仅 JDK），由 `ArchitectureGuardTest.spiMustBeFrameworkFree` 守护。
- 审计字段无登录填 `0`（不改表结构，与 `DEFAULT 0` 一致）。
- 填充为无条件覆盖（`updateUserId` 恒等于最后操作者）。
- 对象转换用 MapStruct；本计划的字段填充属基础设施，直接用 setter，不适用 MapStruct。
- **构建/测试命令**（Git Bash 下 `./mvnw`/`mvn` 会因 MSYS 路径报 `ClassNotFoundException: plexus.classworlds.launcher.Launcher`，必须用 Windows 原生 mvn.cmd，JDK 25 GraalVM）：
  - Maven：`D:\base_envir_soft\apache-maven-3.9.16-bin\bin\mvn.cmd`
  - 单模块跑指定测试：`D:/base_envir_soft/apache-maven-3.9.16-bin/bin/mvn.cmd -pl <module> -am test -Dtest=<Class> -Dsurefire.failIfNoSpecifiedTests=false`
  - 集成测试额外要求：本地 PG（库 `ai_code_agent_test`）与 Redis 已启动（podman，`localhost:5432`/`localhost:6379`）。

---

### Task 1: UserContext 用户上下文（user-spi）

**Files:**
- Create: `ai-code-agent-user/src/main/java/com/dp/ai_code_agent/user/spi/context/UserContext.java`
- Test: `ai-code-agent-user/src/test/java/com/dp/ai_code_agent/user/spi/context/UserContextTest.java`

**Interfaces:**
- Produces: `UserContext.get():UserIdentity`、`getUserId():Long`、`getUsername():String`、`<R, X extends Throwable> scoped(UserIdentity, ScopedValue.CallableOp<R,X>):R`（`throws X`）。

- [ ] **Step 1: 写失败测试**

```java
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
```

- [ ] **Step 2: 运行测试确认失败**

Run: `D:/base_envir_soft/apache-maven-3.9.16-bin/bin/mvn.cmd -pl ai-code-agent-user -am test -Dtest=UserContextTest -Dsurefire.failIfNoSpecifiedTests=false`
Expected: 编译失败（`UserContext` 不存在）。

- [ ] **Step 3: 写最小实现**

```java
package com.dp.ai_code_agent.user.spi.context;

import com.dp.ai_code_agent.user.spi.model.UserIdentity;

/**
 * 当前请求的用户上下文（零框架依赖，仅 JDK）。
 * <p>
 * 基于 {@link ScopedValue} 而非 ThreadLocal：作用域退出自动解绑，
 * 杜绝 servlet 线程池复用导致的串号/泄漏。由 web 层在请求边界绑定。
 */
public final class UserContext {

    private static final ScopedValue<UserIdentity> CURRENT = ScopedValue.newInstance();

    private UserContext() {
    }

    /** 当前登录用户身份；未绑定时返回 {@code null}。 */
    public static UserIdentity get() {
        return CURRENT.isBound() ? CURRENT.get() : null;
    }

    /** 当前用户 ID；未绑定时返回 {@code null}。 */
    public static Long getUserId() {
        UserIdentity identity = get();
        return identity == null ? null : identity.id();
    }

    /** 当前用户名；未绑定时返回 {@code null}。 */
    public static String getUsername() {
        UserIdentity identity = get();
        return identity == null ? null : identity.username();
    }

    /** 在给定身份作用域内执行 {@code op}，作用域退出后自动解绑。 */
    public static <R, X extends Throwable> R scoped(UserIdentity identity,
                                                    ScopedValue.CallableOp<R, X> op) throws X {
        return ScopedValue.where(CURRENT, identity).call(op);
    }
}
```

- [ ] **Step 4: 运行测试确认通过**

Run: `D:/base_envir_soft/apache-maven-3.9.16-bin/bin/mvn.cmd -pl ai-code-agent-user -am test -Dtest=UserContextTest -Dsurefire.failIfNoSpecifiedTests=false`
Expected: PASS（3 tests）。

- [ ] **Step 5: 提交**

```bash
git add ai-code-agent-user/src/main/java/com/dp/ai_code_agent/user/spi/context/UserContext.java \
        ai-code-agent-user/src/test/java/com/dp/ai_code_agent/user/spi/context/UserContextTest.java
git commit -m "feat(user): 新增 UserContext 用户上下文（ScopedValue）"
```

---

### Task 2: AuditFieldFillListener 填充监听器（user.audit）

**Files:**
- Create: `ai-code-agent-user/src/main/java/com/dp/ai_code_agent/user/audit/AuditFieldFillListener.java`
- Test: `ai-code-agent-user/src/test/java/com/dp/ai_code_agent/user/audit/AuditFieldFillListenerTest.java`

**Interfaces:**
- Consumes: `UserContext.getUserId()`（Task 1）。
- Produces: `AuditFieldFillListener`（实现 `cn.xbatis.listener.OnInsertListener`、`cn.xbatis.listener.OnUpdateListener`）。

> 实现说明：监听器用 `OnInsertListener<Object>`/`OnUpdateListener<Object>` + `instanceof BaseEntity` 守卫（而非 spec 里的 `<BaseEntity>` 泛型），避免未来出现非 `BaseEntity` 实体时桥接方法强转抛 `ClassCastException`；行为与 spec 一致。

- [ ] **Step 1: 写失败测试**

```java
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
        UserContext.scoped(ALICE, () -> { listener.onInsert(entity); return null; });
        assertThat(entity.getCreateAt()).isEqualTo(1L);
        assertThat(entity.getUpdateUserId()).isEqualTo(1L);
    }

    @Test
    void onUpdate_fillsTimeAndCurrentUserId() throws Exception {
        BaseEntity entity = new BaseEntity() {};
        UserContext.scoped(ALICE, () -> { listener.onUpdate(entity); return null; });
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
```

- [ ] **Step 2: 运行测试确认失败**

Run: `D:/base_envir_soft/apache-maven-3.9.16-bin/bin/mvn.cmd -pl ai-code-agent-user -am test -Dtest=AuditFieldFillListenerTest -Dsurefire.failIfNoSpecifiedTests=false`
Expected: 编译失败（`AuditFieldFillListener` 不存在）。

- [ ] **Step 3: 写最小实现**

```java
package com.dp.ai_code_agent.user.audit;

import cn.xbatis.listener.OnInsertListener;
import cn.xbatis.listener.OnUpdateListener;
import com.dp.ai_code_agent.common.model.BaseEntity;
import com.dp.ai_code_agent.user.spi.context.UserContext;

import java.time.LocalDateTime;

/**
 * 审计字段自动填充：xbatis 全局 OnInsert/OnUpdate 监听器。
 * <p>
 * INSERT 填 createdTime/updateTime/createAt/updateUserId；UPDATE 填 updateTime/updateUserId；
 * 无登录态 createAt/updateUserId 填 0（表示系统）。isDeleted 由 {@code @LogicDelete} 处理，此处不碰。
 */
public class AuditFieldFillListener
        implements OnInsertListener<Object>, OnUpdateListener<Object> {

    @Override
    public void onInsert(Object entity) {
        if (entity instanceof BaseEntity base) {
            LocalDateTime now = LocalDateTime.now();
            long userId = currentUserId();
            base.setCreatedTime(now);
            base.setUpdateTime(now);
            base.setCreateAt(userId);
            base.setUpdateUserId(userId);
        }
    }

    @Override
    public void onUpdate(Object entity) {
        if (entity instanceof BaseEntity base) {
            base.setUpdateTime(LocalDateTime.now());
            base.setUpdateUserId(currentUserId());
        }
    }

    private static long currentUserId() {
        Long userId = UserContext.getUserId();
        return userId == null ? 0L : userId;
    }
}
```

- [ ] **Step 4: 运行测试确认通过**

Run: `D:/base_envir_soft/apache-maven-3.9.16-bin/bin/mvn.cmd -pl ai-code-agent-user -am test -Dtest=AuditFieldFillListenerTest -Dsurefire.failIfNoSpecifiedTests=false`
Expected: PASS（5 tests）。

- [ ] **Step 5: 提交**

```bash
git add ai-code-agent-user/src/main/java/com/dp/ai_code_agent/user/audit/AuditFieldFillListener.java \
        ai-code-agent-user/src/test/java/com/dp/ai_code_agent/user/audit/AuditFieldFillListenerTest.java
git commit -m "feat(user): 新增审计字段自动填充监听器"
```

---

### Task 3: 全局注册 AuditFillAutoConfiguration

**Files:**
- Create: `ai-code-agent-user/src/main/java/com/dp/ai_code_agent/user/audit/AuditFillAutoConfiguration.java`
- Modify: `ai-code-agent-user/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
- Test: `ai-code-agent-user/src/test/java/com/dp/ai_code_agent/user/audit/AuditFillAutoConfigurationTest.java`

**Interfaces:**
- Consumes: `AuditFieldFillListener`（Task 2）。
- Produces: xbatis 全局 `OnInsertListener`/`OnUpdateListener` 已注册（`XbatisGlobalConfig.getGlobalOnInsertListener()` / `getGlobalOnUpdateListener()` 非空）。

- [ ] **Step 1: 写失败测试**

```java
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
```

- [ ] **Step 2: 运行测试确认失败**

Run: `D:/base_envir_soft/apache-maven-3.9.16-bin/bin/mvn.cmd -pl ai-code-agent-user -am test -Dtest=AuditFillAutoConfigurationTest -Dsurefire.failIfNoSpecifiedTests=false`
Expected: 编译失败（`AuditFillAutoConfiguration` 不存在）。

- [ ] **Step 3: 写最小实现**

```java
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
```

- [ ] **Step 4: 修改 AutoConfiguration.imports**

在 `ai-code-agent-user/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` 末尾追加一行：

```
com.dp.ai_code_agent.user.audit.AuditFillAutoConfiguration
```

（原文件内容为单行 `com.dp.ai_code_agent.user.local.config.LocalUserProviderAutoConfiguration`，追加后共两行。）

- [ ] **Step 5: 运行测试确认通过**

Run: `D:/base_envir_soft/apache-maven-3.9.16-bin/bin/mvn.cmd -pl ai-code-agent-user -am test -Dtest=AuditFillAutoConfigurationTest -Dsurefire.failIfNoSpecifiedTests=false`
Expected: PASS（1 test）。

- [ ] **Step 6: 提交**

```bash
git add ai-code-agent-user/src/main/java/com/dp/ai_code_agent/user/audit/AuditFillAutoConfiguration.java \
        ai-code-agent-user/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports \
        ai-code-agent-user/src/test/java/com/dp/ai_code_agent/user/audit/AuditFillAutoConfigurationTest.java
git commit -m "feat(user): 注册 xbatis 全局审计填充监听器"
```

---

### Task 4: isDeleted 迁移 @LogicDelete + 服务去手写审计字段

**Files:**
- Modify: `ai-code-agent-common/pom.xml`
- Modify: `ai-code-agent-common/src/main/java/com/dp/ai_code_agent/common/model/BaseEntity.java`
- Modify: `ai-code-agent-user/src/main/java/com/dp/ai_code_agent/user/local/service/LocalUserAuthServiceImpl.java`
- Modify: `ai-code-agent-user/src/main/java/com/dp/ai_code_agent/user/local/service/LocalUserAdminServiceImpl.java`
- Test: `ai-code-agent-user/src/test/java/com/dp/ai_code_agent/user/local/service/LocalUserAuthServiceImplTest.java`

**Interfaces:**
- Consumes: Task 2/3 的监听器与 `@LogicDelete`（此处只是移除手写赋值，让框架接管）。
- Produces: `register/setStatus/resetPassword` 不再手写审计字段。

- [ ] **Step 1: 写失败测试（服务不再手写审计字段）**

在 `LocalUserAuthServiceImplTest` 中新增测试（需补充 `import org.mockito.ArgumentCaptor;`）：

```java
@Test
void register_doesNotSetAuditFields() {
    when(userMapper.existsByUsername("alice")).thenReturn(false);
    when(passwordHasher.hash("pwd")).thenReturn("hashed");
    when(userConverter.toUserIdentity(any(User.class))).thenReturn(
            new UserIdentity(99L, "alice", "Alice", UserRole.USER, true));

    service.register("alice", "pwd", "Alice");

    ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
    verify(userMapper).save(captor.capture());
    User saved = captor.getValue();
    assertThat(saved.getCreatedTime()).isNull();
    assertThat(saved.getUpdateTime()).isNull();
    assertThat(saved.getCreateAt()).isNull();
    assertThat(saved.getUpdateUserId()).isNull();
    assertThat(saved.getIsDeleted()).isNull();
}
```

- [ ] **Step 2: 运行测试确认失败**

Run: `D:/base_envir_soft/apache-maven-3.9.16-bin/bin/mvn.cmd -pl ai-code-agent-user -am test -Dtest=LocalUserAuthServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false`
Expected: FAIL——`register()` 仍手写 `createdTime/updateTime/createAt/updateUserId/isDeleted`。

- [ ] **Step 3: common 引入 xbatis-annotation + BaseEntity 加 @LogicDelete**

`ai-code-agent-common/pom.xml` 新增依赖（`isDeleted` 字段在 `BaseEntity`，而非 `User`）：

```xml
<dependency>
    <groupId>cn.xbatis</groupId>
    <artifactId>xbatis-annotation</artifactId>
    <version>1.10.6</version>
</dependency>
```

`BaseEntity.java` 增加 import 并对 `isDeleted` 字段加注解：

```java
import cn.xbatis.db.annotations.LogicDelete;
```

```java
    /** 逻辑删除标记 */
    @LogicDelete(beforeValue = "false", afterValue = "true")
    private Boolean isDeleted;
```

- [ ] **Step 4: 服务去手写审计字段**

`LocalUserAuthServiceImpl.register()` 删除以下 7 行（并删除不再使用的 `import java.time.LocalDateTime;`）：

```java
        user.setIsDeleted(false);
        user.setCreateAt(0L);
        user.setUpdateUserId(0L);
        LocalDateTime now = LocalDateTime.now();
        user.setCreatedTime(now);
        user.setUpdateTime(now);
```

`LocalUserAdminServiceImpl` 的 `setStatus()` 与 `resetPassword()` 各删除一行 `user.setUpdateTime(LocalDateTime.now());`，并删除不再使用的 `import java.time.LocalDateTime;`。

- [ ] **Step 5: 运行 user 模块全部单测确认通过**

Run: `D:/base_envir_soft/apache-maven-3.9.16-bin/bin/mvn.cmd -pl ai-code-agent-user -am test -Dsurefire.failIfNoSpecifiedTests=false`
Expected: 全部 PASS（含既有 `LocalUserAuthServiceImplTest`/`LocalUserAdminServiceImplTest`/`SessionRepositoryTest`/`PasswordHasherTest`/`AuditFieldFillListenerTest` 等）。

- [ ] **Step 6: 提交**

```bash
git add ai-code-agent-common/pom.xml \
        ai-code-agent-common/src/main/java/com/dp/ai_code_agent/common/model/BaseEntity.java \
        ai-code-agent-user/src/main/java/com/dp/ai_code_agent/user/local/service/LocalUserAuthServiceImpl.java \
        ai-code-agent-user/src/main/java/com/dp/ai_code_agent/user/local/service/LocalUserAdminServiceImpl.java \
        ai-code-agent-user/src/test/java/com/dp/ai_code_agent/user/local/service/LocalUserAuthServiceImplTest.java
git commit -m "refactor(user): 审计字段改为框架自动填充，isDeleted 迁移 @LogicDelete"
```

---

### Task 5: TokenAuthenticationFilter 绑定 UserContext

**Files:**
- Modify: `ai-code-agent-web/src/main/java/com/dp/ai_code_agent/web/security/TokenAuthenticationFilter.java`
- Test: `ai-code-agent-web/src/test/java/com/dp/ai_code_agent/web/security/TokenAuthenticationFilterTest.java`

**Interfaces:**
- Consumes: `UserContext.scoped(UserIdentity, ScopedValue.CallableOp<R,X>)`（Task 1）。
- Produces: 认证通过时下游 filterChain 在 `UserContext` 作用域内执行，作用域结束自动解绑。

- [ ] **Step 1: 写失败测试**

```java
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
```

- [ ] **Step 2: 运行测试确认失败**

Run: `D:/base_envir_soft/apache-maven-3.9.16-bin/bin/mvn.cmd -pl ai-code-agent-web -am test -Dtest=TokenAuthenticationFilterTest -Dsurefire.failIfNoSpecifiedTests=false`
Expected: FAIL——`validToken_bindsUserContextDuringFilterAndClearsAfter` 因下游 `UserContext.get()` 为 null 而失败。

- [ ] **Step 3: 改造 doFilterInternal**

`TokenAuthenticationFilter.java` 增加 import `com.dp.ai_code_agent.user.spi.context.UserContext;`，并将 `doFilterInternal` 替换为：

```java
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        UserIdentity user = resolveUser(request);
        if (user == null) {
            SecurityContextHolder.clearContext();
            filterChain.doFilter(request, response);
            return;
        }
        List<GrantedAuthority> authorities = List.of(
                new SimpleGrantedAuthority("ROLE_" + user.userRole().name()));
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(user, null, authorities);
        SecurityContextHolder.getContext().setAuthentication(authentication);
        try {
            UserContext.scoped(user, () -> {
                filterChain.doFilter(request, response);
                return null;
            });
        } catch (ServletException | IOException e) {
            throw e;
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }

    private UserIdentity resolveUser(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            return null;
        }
        String token = header.substring(BEARER_PREFIX.length());
        try {
            return userAuthService.resolve(token);
        } catch (BusinessException ignored) {
            return null;
        }
    }
```

- [ ] **Step 4: 运行测试确认通过**

Run: `D:/base_envir_soft/apache-maven-3.9.16-bin/bin/mvn.cmd -pl ai-code-agent-web -am test -Dtest=TokenAuthenticationFilterTest -Dsurefire.failIfNoSpecifiedTests=false`
Expected: PASS（3 tests）。

- [ ] **Step 5: 运行 web 模块全部测试（含 ArchUnit 守卫）**

Run: `D:/base_envir_soft/apache-maven-3.9.16-bin/bin/mvn.cmd -pl ai-code-agent-web -am test -Dsurefire.failIfNoSpecifiedTests=false`
Expected: 全部 PASS（`ArchitectureGuardTest` 确认 `user.spi` 依旧零框架依赖）。

- [ ] **Step 6: 提交**

```bash
git add ai-code-agent-web/src/main/java/com/dp/ai_code_agent/web/security/TokenAuthenticationFilter.java \
        ai-code-agent-web/src/test/java/com/dp/ai_code_agent/web/security/TokenAuthenticationFilterTest.java
git commit -m "feat(web): TokenAuthenticationFilter 绑定 UserContext"
```

---

### Task 6: 集成测试（审计字段自动填充 + 逻辑删除）

**Files:**
- Test: `ai-code-agent-app/src/test/java/com/dp/ai_code_agent/app/AuditFillIT.java`

**Interfaces:**
- Consumes: Task 3（全局监听器）、Task 4（@LogicDelete）、Task 5（filter 绑定 UserContext）。
- Produces: 端到端证明注册自动落审计字段、管理员操作记录 `updateUserId`、逻辑删除查询过滤。

> 前置：本地 PG（库 `ai_code_agent_test`）+ Redis 已启动；Flyway 启动时自动迁移。

- [ ] **Step 1: 写失败测试**

```java
package com.dp.ai_code_agent.app;

import com.dp.ai_code_agent.user.local.mapper.UserMapper;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuditFillIT {

    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    UserMapper userMapper;

    private String register(String username, String password) throws Exception {
        MvcResult result = mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.data.id").toString();
    }

    private String login(String username, String password) throws Exception {
        MvcResult result = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.data.token");
    }

    @Test
    void register_autoFillsAuditFields() throws Exception {
        String username = "audit" + System.nanoTime();
        register(username, "secret1");

        Map<String, Object> row = jdbc.queryForMap(
                "SELECT created_time, update_time, create_at, update_user_id, is_deleted FROM t_user WHERE username = ?",
                username);
        assertThat(row.get("created_time")).isNotNull();
        assertThat(row.get("update_time")).isNotNull();
        assertThat(((Number) row.get("create_at")).longValue()).isZero();
        assertThat(((Number) row.get("update_user_id")).longValue()).isZero();
        assertThat(row.get("is_deleted")).isEqualTo(false);
    }

    @Test
    void adminSetStatus_fillsUpdateUserId() throws Exception {
        String adminToken = login("admin", "admin123");
        String username = "audit" + System.nanoTime();
        String userId = register(username, "secret1");

        mvc.perform(patch("/api/admin/users/" + userId + "/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"enabled\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        Long adminId = jdbc.queryForObject("SELECT id FROM t_user WHERE username = 'admin'", Long.class);
        Map<String, Object> row = jdbc.queryForMap(
                "SELECT update_user_id FROM t_user WHERE id = ?", Long.parseLong(userId));
        assertThat(((Number) row.get("update_user_id")).longValue()).isEqualTo(adminId);
    }

    @Test
    void logicDelete_marksDeletedAndFiltersQuery() throws Exception {
        String username = "audit" + System.nanoTime();
        String userId = register(username, "secret1");

        userMapper.deleteById(Long.parseLong(userId));

        Boolean deleted = jdbc.queryForObject(
                "SELECT is_deleted FROM t_user WHERE id = ?", Boolean.class, Long.parseLong(userId));
        assertThat(deleted).isTrue();
        assertThat(userMapper.selectByUsername(username)).isNull();
    }
}
```

- [ ] **Step 2: 运行测试确认失败/通过并据此校准**

Run: `D:/base_envir_soft/apache-maven-3.9.16-bin/bin/mvn.cmd -pl ai-code-agent-app -am test -Dtest=AuditFillIT -Dsurefire.failIfNoSpecifiedTests=false`
Expected: 若前置任务均正确，应直接 PASS；若 `@LogicDelete` 查询过滤默认关闭或插入未填 beforeValue，则 FAIL，按 systematic-debugging 根因排查后修正（本任务为兜底验收）。

- [ ] **Step 3: 运行 app 模块既有集成测试回归**

Run: `D:/base_envir_soft/apache-maven-3.9.16-bin/bin/mvn.cmd -pl ai-code-agent-app -am test -Dtest=UserAuthFlowIT,UserAdminFlowIT,AuditFillIT -Dsurefire.failIfNoSpecifiedTests=false`
Expected: 全部 PASS。

- [ ] **Step 4: 提交**

```bash
git add ai-code-agent-app/src/test/java/com/dp/ai_code_agent/app/AuditFillIT.java
git commit -m "test(app): 审计字段自动填充与逻辑删除集成测试"
```

---

## Self-Review

- **Spec 覆盖**：§4.1 UserContext→Task 1；§4.2 监听器→Task 2；§4.3 全局注册→Task 3；§5/§6 填充规则与改动点→Task 4/5；§7 @LogicDelete→Task 4；§8 测试→各 Task + Task 6 集成。无缺口。
- **占位符扫描**：无 TBD/TODO，每步含实际代码。
- **类型一致性**：`UserContext.get/getUserId/getUsername/scoped` 在 Task 1 定义、Task 2/5/6 引用一致；`AuditFieldFillListener` 在 Task 2 定义、Task 3 引用一致；`registerGlobalAuditListener` 在 Task 3 定义并被其测试调用。

## 执行交接

计划已保存。本项目按 AGENTS.md 约定**一律走 Inline Execution（executing-plans）**，不引入 subagent-driven-development。请确认后我即按 Task 1→6 顺序执行，每个任务 RED→GREEN→commit，并在关键节点（Task 4、Task 6 之后）执行全量 `mvn test` 验证。
