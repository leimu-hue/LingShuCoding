# 审计字段自动填充设计文档

> 日期：2026-08-23 · 状态：已确认 · 对应仓库：ai-code-agent

## 1. 背景与目标

`BaseEntity` 已预留审计字段（`createdTime` / `updateTime` / `createAt` / `updateUserId` / `isDeleted`），但当前各 Service 手写赋值，且**不一致**：

| 位置 | 手动填了什么 | 问题 |
|---|---|---|
| `LocalUserAuthServiceImpl.register()` | `createdTime`、`updateTime`、`createAt=0`、`updateUserId=0`、`isDeleted=false` | 全手写 |
| `LocalUserAdminServiceImpl.setStatus()` | 仅 `updateTime` | 漏 `updateUserId` |
| `LocalUserAdminServiceImpl.resetPassword()` | 仅 `updateTime` | 漏 `updateUserId` |

**目标**：为上下文建立统一用户信息（`UserContext`），并借助 xbatis 原生机制在落库时自动填充审计字段，业务代码不再手写时间与操作人。

**非目标（YAGNI）**

- 不做数据库 `updated_at` 触发器 / `ON UPDATE` 之类 DB 层方案（与应用逻辑解耦、便于测试与换库）
- 不做审计日志表 / 操作留痕（本次仅字段填充）
- 不做异步线程的上下文传播（当前无 `@Async`/`AsyncContext` 落库场景）

## 2. 关键技术决策

| 决策点 | 结论 | 理由 |
|---|---|---|
| 上下文载体 | `ScopedValue<UserIdentity>`（非 ThreadLocal） | Java 25 已转正；作用域退出自动解绑，根除线程池复用串号；值只读单向，语义安全 |
| 上下文位置 | `user/spi/context/UserContext` | `UserIdentity` 位于 `user-spi`；`ScopedValue` 为 JDK API，不破坏 spi 纯净性 |
| 上下文填充时机 | `TokenAuthenticationFilter` 内 `UserContext.scoped(user, () -> doFilter)` | 请求边界绑定，覆盖整个 filterChain（含 Service→Mapper 落库） |
| 填充接入点 | xbatis 全局监听器 `XbatisGlobalConfig.setGlobalOnInsertListener/setGlobalOnUpdateListener` | 优于裸 MyBatis Interceptor：能拿到原始实体、零动态 SQL 包装穿透；全局一次注册，逐实体零注解 |
| 逻辑删除 | `BaseEntity.isDeleted` 标注 `@LogicDelete(beforeValue="false", afterValue="true")`（common 引入 `xbatis-annotation`） | xbatis 原生能力，查询自动过滤、删除转 update，业务不手写 |
| 无登录态取值 | `0` | 与现有 `DEFAULT 0` 及注释「0 表示系统」一致，不改表结构 |
| 覆盖策略 | 无条件覆盖 | 审计语义要求 `updateUserId` 恒等于「最后操作者」 |

**换认证框架的改动半径**：`UserContext` 绑定由 `user-spi` 抽象、web 层喂数据，与既有 `UserAuthService` SPI 模式一致；换 Sa-Token / Keycloak 时仅需在对应 filter 中调用 `UserContext.scoped(...)`，填充监听器零改动。

## 3. 现状诊断

`TokenAuthenticationFilter` 已将 `UserIdentity` 作为 principal 写入 `SecurityContextHolder`。缺失的是：

1. 框架无关、业务/ORM 层可直接调用的 `UserContext`；
2. 审计字段的自动填充。

`SecurityContextHolder` 继续服务 Spring Security（`@PreAuthorize` 等授权），`UserContext` 服务业务/ORM 层，二者并存、各司其职。

## 4. 组件设计

### 4.1 `UserContext`（user-spi）

```java
package com.dp.ai_code_agent.user.spi.context;

public final class UserContext {
    private static final ScopedValue<UserIdentity> CURRENT = ScopedValue.newInstance();

    public static UserIdentity get() { return CURRENT.isBound() ? CURRENT.get() : null; }
    public static Long getUserId() { UserIdentity u = get(); return u == null ? null : u.id(); }
    public static String getUsername() { UserIdentity u = get(); return u == null ? null : u.username(); }
    public static <R, X extends Throwable> R scoped(UserIdentity identity,
                                                    ScopedValue.CallableOp<R, X> op) throws X {
        return ScopedValue.where(CURRENT, identity).call(op);
    }
}
```

- 纯 JDK 依赖（`ScopedValue`、`ScopedValue.CallableOp`），不引入 Spring。
- `scoped(...)` 封装绑定细节，filter 与测试调用方不直接接触 `ScopedValue`。
- `CallableOp<R, X>` 可抛出指定受检异常 `X`；`doFilter` 抛出的 `IOException/ServletException` 会推导为 `X = Exception`，由 filter 侧 try/catch 转换（见 6.2）。

### 4.2 `AuditFieldFillListener`（user 模块）

位于 `com.dp.ai_code_agent.user.audit`，实现 xbatis 原生监听器：

```java
public class AuditFieldFillListener
        implements OnInsertListener<Object>, OnUpdateListener<Object> {

    @Override
    public void onInsert(Object entity) {
        if (entity instanceof BaseEntity base) {
            LocalDateTime now = LocalDateTime.now();
            long uid = currentUserId();       // 无登录 → 0
            base.setCreatedTime(now);
            base.setUpdateTime(now);
            base.setCreateAt(uid);
            base.setUpdateUserId(uid);
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
        Long id = UserContext.getUserId();
        return id == null ? 0L : id;
    }
}
```

- 基于 `Object` 入参 + `instanceof BaseEntity` 守卫，对所有继承 `BaseEntity` 的实体生效；非 `BaseEntity` 实体静默跳过（避免桥接方法强转报 `ClassCastException`）。
- 不触碰 `isDeleted`（交由 `@LogicDelete`）。

### 4.3 全局注册（user 模块 `@AutoConfiguration`）

在 `user` 模块新增 `AuditFillAutoConfiguration`，于启动时注册全局监听器：

```java
@AutoConfiguration
public class AuditFillAutoConfiguration {
    @PostConstruct
    void registerGlobalAuditListener() {
        XbatisGlobalConfig.setGlobalOnInsertListener(new AuditFieldFillListener());
        XbatisGlobalConfig.setGlobalOnUpdateListener(new AuditFieldFillListener());
    }
}
```

- 需在 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` 登记。
- 全局监听器 set-once，必须在首个实体操作前注册（实现阶段以 TDD 验证时序）。

## 5. 填充规则（定稿）

| 时机 | 字段 | 值 |
|---|---|---|
| INSERT | `createdTime`、`updateTime` | `now` |
| INSERT | `createAt`、`updateUserId` | 当前用户 ID（无登录 → `0`） |
| UPDATE | `updateTime` | `now` |
| UPDATE | `updateUserId` | 当前用户 ID（无登录 → `0`） |
| `isDeleted` | — | 交由 `@LogicDelete` |

## 6. 改动点清单

### 6.1 新增

| 文件 | 说明 |
|---|---|
| `ai-code-agent-user/.../user/spi/context/UserContext.java` | ScopedValue 用户上下文 |
| `ai-code-agent-user/.../user/audit/AuditFieldFillListener.java` | 审计字段填充监听器 |
| `ai-code-agent-user/.../user/audit/AuditFillAutoConfiguration.java` | 全局监听器注册 |

### 6.2 修改

| 文件 | 动作 |
|---|---|
| `ai-code-agent-common/pom.xml` | 新增 `cn.xbatis:xbatis-annotation:1.10.6` 依赖 |
| `ai-code-agent-common/.../common/model/BaseEntity.java` | `isDeleted` 加 `@LogicDelete(beforeValue="false", afterValue="true")` |
| `ai-code-agent-web/.../web/security/TokenAuthenticationFilter.java` | `resolve` 后 `UserContext.scoped(user, () -> filterChain.doFilter(...))` |
| `ai-code-agent-user/.../user/local/service/LocalUserAuthServiceImpl.java` | `register` 删除手写审计字段赋值 |
| `ai-code-agent-user/.../user/local/service/LocalUserAdminServiceImpl.java` | `setStatus`/`resetPassword` 删除手写 `updateTime` |
| `ai-code-agent-user/.../META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` | 登记 `AuditFillAutoConfiguration` |

## 7. 不变式与约束

- `user-spi` 保持纯净：`UserContext` 仅依赖 JDK（`ScopedValue`/`ScopedValue.CallableOp`），ArchUnit 继续守护 `spi → Spring/xbatis` 禁止。
- 依赖方向：`common` 新增 `xbatis-annotation`（`@LogicDelete` 注解所在，会传递 `mybatis`），`common` 定位由「纯 JDK」调整为「零 Spring 依赖」；`user-spi` 仍零框架依赖。
- 数据库结构不变：无登录填 `0` 与 `DEFAULT 0` 一致，不新增 Flyway 迁移。
- 填充为无条件覆盖：`updateUserId` 恒等于最后操作者。

## 8. 测试策略（TDD，先红后绿）

- **`UserContext` 单测**：绑定后 `get/getUserId` 可读；作用域外 `get()` 返回 `null`。
- **`AuditFieldFillListener` 单测**：`onInsert` 填 4 字段；`onUpdate` 填 2 字段；无登录态 `createAt/updateUserId = 0`。
- **集成测试**：
  - 注册 → 审计字段自动落库（`createdTime/updateTime/createAt/updateUserId` 非空、`createAt=0`）；
  - 管理员 `setStatus`/`resetPassword` → `updateTime` 刷新、`updateUserId` = 管理员 ID；
  - 逻辑删除：`delete` 转 `update is_deleted=true`，查询自动过滤已删数据。
- **既有回归**：`LocalUserAuthServiceImplTest` / `LocalUserAdminServiceImplTest` / `UserAuthFlowIT` / `UserAdminFlowIT` 全绿。

## 9. 风险与备注

- **全局监听器时序**：`setGlobalOnUpdateListener` set-once，若注册晚于首个实体操作则失效；以集成测试兜底，失败即根因排查（systematic-debugging）。
- **逻辑删除与 OnUpdate 监听器的交互**：逻辑删除本质是 update，需在实现阶段验证 `onUpdate` 是否会被触发、`updateUserId` 是否正确记录（当前无 `delete()` 调用，非阻塞）。
- **异步传播**：ScopedValue 不跨线程，当前无异步落库场景，暂不处理（YAGNI）。
