# 用户模块（RBAC + Opaque Token）设计文档

> 日期：2026-08-16 · 状态：已确认 · 对应仓库：ai-code-agent

## 1. 背景与目标

平台需要一个用户体系支撑登录、鉴权与管理功能。考虑未来可能替换为 Sa-Token / Keycloak 等成熟用户框架，当前实现必须与使用方解耦，**通过 SPI 契约下放实现**，保证换框架时业务代码与 web 层零改动。

**目标功能（用户提出）**

1. 用户注册、登录
2. 获取当前登录用户
3. 用户注销（含注销全部设备）
4. 用户权限控制（RBAC）
5. 【管理员】管理用户

**非目标（YAGNI）**

- 不做邮箱/手机号注册（首版仅用户名+密码）
- 不做第三方 OAuth/验证码登录
- 不做角色/权限的自建（角色权限由 Flyway 预置 + 管理员分配，不做在线创建）
- 不做前端页面（本次仅后端，前端另行规划）
- 不做多租户

## 2. 关键技术决策

| 决策点 | 结论 | 理由 |
|---|---|---|
| 模块形态 | 新模块 `ai-code-agent-user`，内部 `spi/` + `local/` 分包 | 单一模块内做契约/实现隔离，避免过度拆分（用户已确认） |
| SPI 加载机制 | Spring 条件装配：`@AutoConfiguration` + `@ConditionalOnMissingBean` + `app.user.provider=local` 开关 | 实现是容器内 Bean，可注入 Mapper、吃 `@Transactional`；外部实现存在时 local 自动退位（用户已确认） |
| 会话方案 | 自研 Opaque Token + Redis，Security `STATELESS` + Bearer Header | 满足注销/封禁级联踢人；不引入 Cookie 依赖（用户已确认） |
| 接口粒度 | 两个门面：`UserAuthService`、`UserAdminService` | 契约面最小，换框架只需实现 2 个接口（用户已确认） |
| 权限模型 | 完整 RBAC 三实体五表（user/role/permission + 两张关联表），权限码粒度 | 管理员可在线给角色分配权限（用户已确认） |
| 账号体系 | 用户名 + 密码（BCrypt），用户名唯一 | 最克制的主链路（用户已确认） |
| 管理端 | 分页/详情/启停（级联清会话）/重置密码/分配角色 | 常规管理档位（用户已确认） |
| 交付范围 | 仅后端；单测 + Testcontainers 集成测试 + apifox-cli 同步接口文档 | 用户已确认 |

**换框架的改动半径**：新增一个实现 `UserAuthService`/`UserAdminService` 的 `@AutoConfiguration`，改 `app.user.provider` 开关（或删除 local 配置），web 层与业务代码零改动。

## 3. 模块结构

```
ai-code-agent-user/                      # 新模块（核心交付）
├── pom.xml                              # common + spring-boot-starter + xbatis + data-redis + mapstruct
└── src/main/java/com/dp/ai_code_agent/user/
    ├── spi/                             # 契约层：零框架依赖
    │   ├── UserAuthService.java         # register / login / logout / resolve(token)
    │   ├── UserAdminService.java        # page / detail / setStatus / resetPassword / assignRoles / grantPermissions
    │   └── model/                       # UserIdentity / LoginResult / RoleDTO / PermissionDTO（不可变 record）
    ├── local/                           # 当前实现（容器内）
    │   ├── config/LocalUserProviderAutoConfiguration.java
    │   ├── service/                     # LocalUserAuthServiceImpl / LocalUserAdminServiceImpl
    │   ├── mapper/                      # UserMapper / RoleMapper / PermissionMapper / UserRoleMapper / RolePermissionMapper
    │   ├── model/                       # User / Role / Permission / UserRole / RolePermission（xbatis 实体）
    │   ├── repository/SessionRepository.java   # Redis 会话存取、滑续、级联删除
    │   └── security/PasswordHasher.java         # BCrypt 封装
    └── src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
```

**既有模块改动**

| 模块 | 改动 |
|---|---|
| 根 `pom.xml` | `<modules>` 增加 `ai-code-agent-user` |
| `ai-code-agent-web` | 依赖 user 模块 + `spring-boot-starter-security`；新增 `SecurityConfig`、`TokenAuthenticationFilter`、`AuthController`、`UserAdminController` |
| `ai-code-agent-app` | 依赖 `spring-boot-starter-data-redis`；`application.yml` 增加 redis 与 `app.user.*` 配置；Flyway 新增迁移脚本 |
| `ai-code-agent-common` | `ErrorCode` 补充业务错误码 |

**依赖方向（强制，ArchUnit 守护）**

- `web → user:spi` ✅
- `web → user:local` ❌（禁止）
- `user:local → user:spi` ✅
- `user:spi → Spring / xbatis / redis 类` ❌（契约保持纯净）

## 4. 数据模型（Flyway）

新增迁移 `V2__user_rbac.sql`：

```sql
CREATE TABLE t_user (
    id            BIGSERIAL PRIMARY KEY,
    username      VARCHAR(64)  NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    nickname      VARCHAR(64),
    status        SMALLINT     NOT NULL DEFAULT 1,  -- 1 正常 0 禁用
    created_at    TIMESTAMP    NOT NULL DEFAULT now(),
    updated_at    TIMESTAMP    NOT NULL DEFAULT now()
);

CREATE TABLE t_role (
    id          BIGSERIAL PRIMARY KEY,
    code        VARCHAR(32) NOT NULL UNIQUE,   -- ADMIN / USER
    name        VARCHAR(64) NOT NULL,
    description VARCHAR(255)
);

CREATE TABLE t_permission (
    id          BIGSERIAL PRIMARY KEY,
    code        VARCHAR(64) NOT NULL UNIQUE,   -- 如 user:create
    name        VARCHAR(64) NOT NULL,
    description VARCHAR(255)
);

CREATE TABLE t_user_role (
    user_id BIGINT NOT NULL REFERENCES t_user(id),
    role_id BIGINT NOT NULL REFERENCES t_role(id),
    PRIMARY KEY (user_id, role_id)
);

CREATE TABLE t_role_permission (
    role_id       BIGINT NOT NULL REFERENCES t_role(id),
    permission_id BIGINT NOT NULL REFERENCES t_permission(id),
    PRIMARY KEY (role_id, permission_id)
);
```

**预置数据**：`ADMIN`、`USER` 两个角色；初始权限码集合；一个初始管理员（密码 BCrypt 哈希，用户名 `admin`，开发期默认密码，文档注明首登改密建议）。

**Demo 清理**：`V3__drop_demo_user.sql` 删除 `t_demo_user` 表；删除 core/web 模块中全部 `DemoUser*` 类；前端 `ConsolePage.tsx` 中 `/demo-users` 调用替换为占位（本次仅后端，占位即可）；README 中 demo 示例更新。

## 5. 会话与安全设计

### 5.1 Token 与会话

- token：`SecureRandom` 32 字节 → Base64URL（约 43 字符），**不含任何用户信息**（opaque）
- Redis 键设计：

| Key | Value | TTL | 用途 |
|---|---|---|---|
| `ua:session:{token}` | 会话快照 JSON（userId/username/nickname/roles/permissionCodes） | 默认 30 分钟，访问滑续 | 鉴权主键 |
| `ua:user-sessions:{userId}` | SET（该用户全部有效 token） | 随成员清理 | 注销全部 / 封禁级联踢人 |

- Redis 中**绝不存储密码哈希**。
- 登录成功：`SET ua:session:{token}` + `SADD ua:user-sessions:{userId}`。
- 注销单设备：`DEL ua:session:{token}` + `SREM ua:user-sessions:{userId}`。
- 注销全部设备：遍历 `ua:user-sessions:{userId}` 逐个 `DEL` 会话键，再删集合。
- 管理员禁用用户：同上"注销全部设备"，并置 `status=0`。
- `resolve(token)`：查会话 → 校验用户未被禁用 → 滑续 TTL → 返回 `UserIdentity`。

### 5.2 安全策略

- `PasswordHasher`：BCrypt（strength 10），`spring-security-crypto` 的 `BCryptPasswordEncoder`。
- 登录失败统一返回「用户名或密码错误」（防枚举）；账号禁用单独错误码。
- `SecurityConfig`：
  - 放行：`POST /api/auth/register`、`POST /api/auth/login`、`GET /api/auth/me` 之前的 OPTIONS、错误页
  - 其余请求需认证；`/api/admin/**` 需 `ROLE_ADMIN`
  - `SessionCreationPolicy.STATELESS`；`TokenAuthenticationFilter` 在 `UsernamePasswordAuthenticationFilter` 之前
  - 401/403 输出统一 JSON（`Result` 结构）
- 方法级安全：`@PreAuthorize("hasRole('ADMIN')")` / `@PreAuthorize("hasAuthority('user:create')")`，`@EnableMethodSecurity`。
- 接口取当前用户：`UserIdentity` 作为 principal 注入 `SecurityContext`，Controller 用 `@AuthenticationPrincipal` 获取。

## 6. API 设计（web 层）

### 6.1 认证接口（公开）

| 方法 | 路径 | 说明 | 请求体/响应 |
|---|---|---|---|
| POST | `/api/auth/register` | 注册（用户名+密码+可选昵称） | `RegisterRequest{username,password,nickname}` → `UserIdentity` |
| POST | `/api/auth/login` | 登录 | `LoginRequest{username,password}` → `LoginResult{token,user}` |
| POST | `/api/auth/logout` | 注销当前 token | 无请求体，`Authorization` 头取 token |
| GET | `/api/auth/me` | 当前登录用户 | → `UserIdentity` |

### 6.2 管理接口（ROLE_ADMIN）

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/admin/users` | 分页查询（keyword / status / roleId 筛选）→ `PageResult<UserAdminDTO>` |
| GET | `/api/admin/users/{id}` | 详情 |
| PATCH | `/api/admin/users/{id}/status` | 启用/禁用（禁用级联清会话） |
| POST | `/api/admin/users/{id}/reset-password` | 重置密码 |
| PUT | `/api/admin/users/{id}/roles` | 分配角色 |
| GET | `/api/admin/roles` | 角色列表（含权限码） |
| PUT | `/api/admin/roles/{roleId}/permissions` | 给角色分配权限 |

统一响应：`Result<T>{code,message,data}`；分页复用 `common.result.PageResult`。
接口变更时按 AGENTS.md 约定用 apifox-cli 同步云端文档。

## 7. 错误处理

- `ErrorCode` 新增：`USERNAME_EXISTS`、`LOGIN_FAILED`、`ACCOUNT_DISABLED`、`FORBIDDEN`、`TOKEN_INVALID`、`TOKEN_EXPIRED`。
- `BusinessException` + 现有 `GlobalExceptionHandler` 统一兜底；
- Security 的 401/403 由 `AuthenticationEntryPoint` / `AccessDeniedHandler` 输出 `Result` JSON；
- 校验失败（jakarta validation）由现有 handler 兜底。

## 8. 测试策略

- **单元测试**（service 层，Mockito）：密码校验、注册重名、登录失败、会话滑续、禁用校验、角色权限合并。
- **集成测试**（Testcontainers：postgres + redis 镜像）：注册 → 登录 → 带 token 访问受保护接口 → 注销 → 再访问 401；管理员禁用用户 → 该用户会话立即失效；分配角色后权限码生效。
- **ArchUnit 测试**：守护依赖方向（web 不依赖 local；spi 不依赖 Spring/xbatis）。
- **spring-security-test**：`@WithMockUser` 辅助 controller 测试。
- 所有测试用例先红后绿（TDD），覆盖即验收标准。

## 9. 配置项

```yaml
app:
  user:
    provider: local                 # 实现开关
    session-ttl: 30m                # 会话过期时间（滑续）
    token-header: Authorization     # 预留
spring:
  data:
    redis:
      host: localhost
      port: 6379
```

## 10. 风险与备注

- **SPI 纯净度靠 ArchUnit 兜底**：若守卫测试被破坏（如 web 直接 import local），CI 立即失败。
- Redis 故障 → 认证/鉴权失败（快速失败，不降级为放行），启动期不强制检查连接。
- 初始管理员密码为开发期默认值，spec 中注明上线前必须修改（后续可通过重置密码接口变更）。
