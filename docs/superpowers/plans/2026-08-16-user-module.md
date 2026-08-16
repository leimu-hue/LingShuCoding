# 用户模块（RBAC + Opaque Token）Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 新增 `ai-code-agent-user` 模块，以 SPI 契约（`UserAuthService`/`UserAdminService`）+ Spring 条件装配实现本地 RBAC 用户体系，web 层接入 Spring Security 完成注册/登录/注销/当前用户/管理端能力，并清理既有 Demo 代码。

**Architecture:** `ai-code-agent-user` 单模块，`spi/`（纯接口+record，零框架依赖）与 `local/`（xbatis + BCrypt + Redis 会话）分包；`@AutoConfiguration` + `@ConditionalOnMissingBean` + `app.user.provider=local` 实现可替换；web 层 `TokenAuthenticationFilter` 解析 Bearer token → `UserAuthService.resolve()` 取 Redis 会话 → 构建 SecurityContext（STATELESS）；RBAC 五表由 Flyway 管理，权限码粒度。

**Tech Stack:** Java 25 · Spring Boot 4.1.0（Spring Security 7 线）· xbatis 1.10.6-spring-boot4 · PostgreSQL · Redis（Lettuce）· MapStruct 1.6.3 · Flyway · Testcontainers · ArchUnit · Mockito

## Global Constraints

- 根 pom 父版本 Spring Boot **4.1.0**，Java **25**，包路径 `com.dp.ai_code_agent.<module>`（下划线，无连字符）
- 实体用 `@cn.xbatis.db.annotations.Table` / `@TableId`；Mapper 继承 `cn.xbatis.core.mybatis.mapper.MybatisMapper<T>`
- 对象转换必须用 MapStruct（`converter/` 包），禁止 `new` + `set`
- 数据库变更只走 Flyway（`ai-code-agent-app/src/main/resources/db/migration/`，命名 `V<N>__<说明>.sql`），禁止手改库
- SPI 契约包（`com.dp.ai_code_agent.user.spi`）**不得** import 任何 Spring / xbatis / redis 类（ArchUnit 守护）
- `web` 模块**不得** import `user.local`（ArchUnit 守护）
- 统一响应 `Result<T>{code,message,data}`，分页 `common.result.PageResult`，业务异常走 `BusinessException` + `GlobalExceptionHandler`
- 测试强制 TDD：先写失败测试 → 验证失败 → 实现 → 验证通过 → commit
- 接口文档变更必须用 apifox-cli 同步（项目名：AI 零代码应用生成平台）
- Demo 清理：删除 core/web 全部 `DemoUser*` 类、`t_demo_user` 表（Flyway V3 drop）、前端 ConsolePage demo 调用替换占位、README 更新
- 每个任务必须独立可测试、可提交

---

## 任务总览

| # | 任务 | 产出 |
|---|---|---|
| 1 | 搭建 `ai-code-agent-user` 模块骨架 + SPI 契约 | spi/ 接口与模型、pom、根 pom 注册 |
| 2 | Flyway V2 建 RBAC 五表 + 预置数据 | `V2__user_rbac.sql` |
| 3 | local 实体/Mapper/Converter | 五实体 + 五 Mapper + 转换器 |
| 4 | `PasswordHasher` + `SessionRepository`（Redis 会话） | BCrypt 封装 + 会话存取/滑续/级联删除 |
| 5 | `LocalUserAuthServiceImpl`（注册/登录/注销/resolve） | 认证门面实现 + 单测 |
| 6 | `LocalUserAdminServiceImpl`（分页/启停/重置密码/分配角色/授权） | 管理门面实现 + 单测 |
| 7 | local 自动配置装配 + `app.user.provider` 开关 | `LocalUserProviderAutoConfiguration` + imports 文件 |
| 8 | web 层 Security 集成（SecurityConfig + TokenFilter + 401/403 JSON） | 鉴权链路 + ArchUnit 守卫测试 |
| 9 | web 层 Controller（Auth / UserAdmin / 当前用户） | REST API + controller 测试 |
| 10 | app 配置（redis、app.user.*、Testcontainers 依赖） | application.yml + 集成测试基座 |
| 11 | 集成测试全链路（Testcontainers） | 注册→登录→鉴权→注销→禁用→授权 全链路用例 |
| 12 | 清理 Demo 代码（后端/前端/README） | 无 Demo 残留，构建通过 |
| 13 | apifox-cli 同步接口文档 + 最终验证 | 云端文档与代码一致，全量构建+测试绿 |

---

### Task 1: ai-code-agent-user 模块骨架 + SPI 契约

**Files:**
- Create: `ai-code-agent-user/pom.xml`
- Create: `ai-code-agent-user/src/main/java/com/dp/ai_code_agent/user/spi/UserAuthService.java`
- Create: `ai-code-agent-user/src/main/java/com/dp/ai_code_agent/user/spi/UserAdminService.java`
- Create: `ai-code-agent-user/src/main/java/com/dp/ai_code_agent/user/spi/model/UserIdentity.java`
- Create: `ai-code-agent-user/src/main/java/com/dp/ai_code_agent/user/spi/model/LoginResult.java`
- Create: `ai-code-agent-user/src/main/java/com/dp/ai_code_agent/user/spi/model/RoleDTO.java`
- Create: `ai-code-agent-user/src/main/java/com/dp/ai_code_agent/user/spi/model/PermissionDTO.java`
- Modify: `pom.xml`（modules 增加 `ai-code-agent-user`）

**Interfaces:**
- Consumes: `com.dp.ai_code_agent.common.result.PageResult`（common 模块）
- Produces: 以下签名供 Task 3-9 消费：

```java
// spi/UserAuthService.java
public interface UserAuthService {
    UserIdentity register(String username, String password, String nickname);
    LoginResult login(String username, String password);
    void logout(String token);
    UserIdentity resolve(String token);
}

// spi/UserAdminService.java
public interface UserAdminService {
    PageResult<UserAdminDTO> page(int page, int size, String keyword, Integer status, Long roleId);
    UserAdminDTO detail(Long id);
    void setStatus(Long id, boolean enabled);
    void resetPassword(Long id, String newPassword);
    void assignRoles(Long id, List<Long> roleIds);
    List<RoleDTO> listRoles();
    void grantPermissions(Long roleId, List<Long> permissionIds);
}

// spi/model/UserIdentity.java —— record，实现 Security 的 Principal
public record UserIdentity(Long id, String username, String nickname,
                           List<RoleDTO> roles, List<PermissionDTO> permissions,
                           boolean enabled) {}

// spi/model/LoginResult.java
public record LoginResult(String token, UserIdentity user) {}

// spi/model/RoleDTO.java
public record RoleDTO(Long id, String code, String name, List<PermissionDTO> permissions) {}

// spi/model/PermissionDTO.java
public record PermissionDTO(Long id, String code, String name) {}
```

- 注意：`UserAdminDTO` 属于 web 层展示模型，放 `web/dto`（见 Task 9）；`UserAdminService` 方法签名中引用它会导致 spi 依赖 web —— **改为 spi 只返回 user 模块自有模型**：`page` 返回 `PageResult<UserAdminDTO>` 中 DTO 放 `user.spi.model` 或 `user.local.dto`。**决策：UserAdminDTO 放 `user/spi/model/UserAdminDTO.java`（纯 record，零依赖），web 层直接复用。**

- [ ] **Step 1: 创建 pom.xml**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>com.dp</groupId>
        <artifactId>ai-code-agent</artifactId>
        <version>0.0.1</version>
    </parent>
    <artifactId>ai-code-agent-user</artifactId>
    <name>ai-code-agent-user</name>
    <description>用户模块：SPI 契约 + 本地 RBAC 实现</description>

    <dependencies>
        <dependency>
            <groupId>com.dp</groupId>
            <artifactId>ai-code-agent-common</artifactId>
            <version>${project.version}</version>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter</artifactId>
        </dependency>
        <dependency>
            <groupId>cn.xbatis</groupId>
            <artifactId>xbatis-spring-boot-starter</artifactId>
        </dependency>
        <dependency>
            <groupId>cn.xbatis</groupId>
            <artifactId>xbatis-core</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-redis</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.security</groupId>
            <artifactId>spring-security-crypto</artifactId>
        </dependency>
        <dependency>
            <groupId>org.mapstruct</groupId>
            <artifactId>mapstruct</artifactId>
        </dependency>
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>
        <!-- 测试 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.testcontainers</groupId>
            <artifactId>junit-jupiter</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.testcontainers</groupId>
            <artifactId>postgresql</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>com.tngtech.archunit</groupId>
            <artifactId>archunit-junit5</artifactId>
            <version>1.4.1</version>
            <scope>test</scope>
        </dependency>
    </dependencies>
</project>
```

- [ ] **Step 2: 创建 spi/model 四个 record**（内容见上 Produce 签名块；文件放在 `ai-code-agent-user/src/main/java/com/dp/ai_code_agent/user/spi/model/`，无任何注解与 import）

- [ ] **Step 3: 创建 spi 两个门面接口**（内容见上 Produce 签名块）

- [ ] **Step 4: 根 pom modules 注册**

修改根 `pom.xml` 的 `<modules>` 加入 `<module>ai-code-agent-user</module>`。

- [ ] **Step 5: 编译验证**

Run: `mvn -q -pl ai-code-agent-user -am compile`
Expected: BUILD SUCCESS

- [ ] **Step 6: 提交**

```bash
git add pom.xml ai-code-agent-user/
git commit -m "feat(user): 新增 user 模块与 SPI 契约(UserAuthService/UserAdminService)"
```

---

### Task 2: Flyway V2 建 RBAC 五表 + 预置数据

**Files:**
- Create: `ai-code-agent-app/src/main/resources/db/migration/V2__user_rbac.sql`

**Interfaces:**
- Consumes: 无
- Produces: 表结构 + 预置数据，供 Task 3 Mapper 使用。预置：角色 `ADMIN`/`USER`；权限码 `user:view` `user:create` `user:update` `user:delete` `user:reset-password` `user:assign-role` `role:view` `role:grant`；`ADMIN` 拥有全部权限，`USER` 拥有 `user:view`；初始管理员 `admin`（BCrypt(strength=10) of `admin123`，哈希值用 `{bcrypt}` 前缀存储——xbatis 读出的原始串，比对时用 `BCryptPasswordEncoder.matches`，故直接存 BCrypt 串即可，无前缀）。

- [ ] **Step 1: 创建迁移脚本**

```sql
-- V2: 用户 RBAC 表结构 + 预置数据
CREATE TABLE t_user (
    id            BIGSERIAL PRIMARY KEY,
    username      VARCHAR(64)  NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    nickname      VARCHAR(64),
    status        SMALLINT     NOT NULL DEFAULT 1,
    created_at    TIMESTAMP    NOT NULL DEFAULT now(),
    updated_at    TIMESTAMP    NOT NULL DEFAULT now()
);

CREATE TABLE t_role (
    id          BIGSERIAL PRIMARY KEY,
    code        VARCHAR(32) NOT NULL UNIQUE,
    name        VARCHAR(64) NOT NULL,
    description VARCHAR(255)
);

CREATE TABLE t_permission (
    id          BIGSERIAL PRIMARY KEY,
    code        VARCHAR(64) NOT NULL UNIQUE,
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

INSERT INTO t_role (code, name, description) VALUES
    ('ADMIN', '管理员', '平台管理员，拥有全部权限'),
    ('USER',  '普通用户', '默认注册角色');

INSERT INTO t_permission (code, name) VALUES
    ('user:view', '查看用户'),
    ('user:create', '创建用户'),
    ('user:update', '编辑用户'),
    ('user:delete', '删除用户'),
    ('user:reset-password', '重置密码'),
    ('user:assign-role', '分配角色'),
    ('role:view', '查看角色'),
    ('role:grant', '角色授权');

INSERT INTO t_role_permission (role_id, permission_id)
SELECT r.id, p.id FROM t_role r JOIN t_permission p ON TRUE
WHERE r.code = 'ADMIN';

INSERT INTO t_role_permission (role_id, permission_id)
SELECT r.id, p.id FROM t_role r JOIN t_permission p ON TRUE
WHERE r.code = 'USER' AND p.code = 'user:view';

-- 初始管理员 admin / admin123（开发期默认，上线前必须重置）
-- 哈希：BCrypt(strength=10)，由 Task 4 生成后回填此处（见 Step 2）
INSERT INTO t_user (username, password_hash, nickname, status) VALUES
    ('admin', '<BCRYPT_OF_admin123>', '管理员', 1);

INSERT INTO t_user_role (user_id, role_id)
SELECT u.id, r.id FROM t_user u, t_role r
WHERE u.username = 'admin' AND r.code = 'ADMIN';
```

- [ ] **Step 2: 生成 admin123 的 BCrypt 哈希并回填**

Run: `java -cp <spring-security-crypto jar + commons-logging jar> org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder` 不可直接跑；改用测试方式：在 `ai-code-agent-user` 的 test 目录建临时类或用 jshell：

```bash
cd ai-code-agent-user && mvn -q dependency:build-classpath -Dmdep.outputFile=cp.txt
# 用 jshell 加载 classpath 后执行：
#   import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
#   System.out.println(new BCryptPasswordEncoder(10).encode("admin123"));
```

将输出串替换脚本中的 `<BCRYPT_OF_admin123>`。

- [ ] **Step 3: 本地 PG 执行验证**

Run: `psql -h localhost -U postgres -d ai_code_agent -f ai-code-agent-app/src/main/resources/db/migration/V2__user_rbac.sql`
Expected: 五表 + 预置数据插入成功；`SELECT count(*) FROM t_role;` → 2

- [ ] **Step 4: 提交**

```bash
git add ai-code-agent-app/src/main/resources/db/migration/V2__user_rbac.sql
git commit -m "feat(user): Flyway V2 建 RBAC 五表与预置数据"
```

---

### Task 3: local 实体 / Mapper / MapStruct 转换器

**Files:**
- Create: `ai-code-agent-user/src/main/java/com/dp/ai_code_agent/user/local/model/{User,Role,Permission,UserRole,RolePermission}.java`
- Create: `ai-code-agent-user/src/main/java/com/dp/ai_code_agent/user/local/mapper/{UserMapper,RoleMapper,PermissionMapper,UserRoleMapper,RolePermissionMapper}.java`
- Create: `ai-code-agent-user/src/main/java/com/dp/ai_code_agent/user/local/converter/UserConverter.java`

**Interfaces:**
- Consumes: Task 1 的 `spi/model/*`；Task 2 的表结构
- Produces:

```java
// local/model/User.java
@Data @Table("t_user")
public class User {
    @TableId private Long id;
    private String username;
    private String passwordHash;
    private String nickname;
    private Integer status;          // 1 正常 0 禁用
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
// Role: id, code, name, description
// Permission: id, code, name, description
// UserRole: userId, roleId   （@Table("t_user_role")，联合主键用两个 @TableId？——xbatis 联合主键用 @TableId 标注一个即可，此处用业务查询主键 userId；RolePermission 同理 roleId）
```

```java
// local/mapper/UserMapper.java
public interface UserMapper extends MybatisMapper<User> {}
// RoleMapper / PermissionMapper / UserRoleMapper / RolePermissionMapper 同型
```

```java
// local/converter/UserConverter.java
@Mapper(componentModel = "spring")
public interface UserConverter {
    UserIdentity toUserIdentity(User user, List<RoleDTO> roles);
    // roles 由 service 装配后传入，转换器只做字段搬运（username/nickname/status→enabled）
}
```

- [ ] **Step 1: 创建五个实体**（见上 Produce；`@Table`/`@TableId` 用 `cn.xbatis.db.annotations`）
- [ ] **Step 2: 创建五个 Mapper**（继承 `MybatisMapper<T>`，无方法体）
- [ ] **Step 3: 创建 UserConverter**（`@Mapper(componentModel="spring")`；`toUserIdentity` 的 `enabled = status==1` 用 `@Mapping(target="enabled", expression="java(user.getStatus()==1)")`）
- [ ] **Step 4: 编译验证**

Run: `mvn -q -pl ai-code-agent-user -am compile`
Expected: BUILD SUCCESS（MapStruct 生成 `UserConverterImpl`）

- [ ] **Step 5: 提交**

```bash
git add ai-code-agent-user/src/main/java/com/dp/ai_code_agent/user/local
git commit -m "feat(user): local 实体/Mapper/MapStruct 转换器"
```

---

### Task 4: PasswordHasher + SessionRepository（Redis 会话）

**Files:**
- Create: `ai-code-agent-user/src/main/java/com/dp/ai_code_agent/user/local/security/PasswordHasher.java`
- Create: `ai-code-agent-user/src/main/java/com/dp/ai_code_agent/user/local/repository/SessionRepository.java`
- Create: `ai-code-agent-user/src/test/java/com/dp/ai_code_agent/user/local/security/PasswordHasherTest.java`
- Create: `ai-code-agent-user/src/test/java/com/dp/ai_code_agent/user/local/repository/SessionRepositoryTest.java`（需要 Redis —— 用 Testcontainers redis，Task 10 提供基座；本任务先用 Mockito 单测逻辑分支）

**Interfaces:**
- Consumes: Task 1 的 `UserIdentity`（序列化进 Redis 的载荷）
- Produces:

```java
// local/security/PasswordHasher.java —— @Component
public class PasswordHasher {
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(10);
    public String hash(String rawPassword);
    public boolean matches(String rawPassword, String encoded);
}

// local/repository/SessionRepository.java —— @Component
// 键：ua:session:{token}、ua:user-sessions:{userId}；TTL 与滑续；JSON 用 Jackson ObjectMapper
public class SessionRepository {
    private static final String SESSION_KEY = "ua:session:";
    private static final String USER_SESSIONS_KEY = "ua:user-sessions:";

    void save(String token, UserIdentity user, Duration ttl);
    Optional<UserIdentity> find(String token, Duration ttl);   // 命中即 EXPIRE 滑续
    void remove(String token);
    void removeAllByUserId(Long userId);                        // 注销全部/封禁级联
}
```

- [ ] **Step 1: 写 PasswordHasherTest（红）**

```java
class PasswordHasherTest {
    @Test void hashAndMatches() {
        PasswordHasher h = new PasswordHasher();
        String hash = h.hash("secret");
        assertThat(hash).isNotEqualTo("secret");
        assertThat(h.matches("secret", hash)).isTrue();
        assertThat(h.matches("wrong", hash)).isFalse();
    }
}
```

- [ ] **Step 2: 运行测试确认失败** — Run: `mvn -q -pl ai-code-agent-user test -Dtest=PasswordHasherTest` Expected: FAIL（类不存在）
- [ ] **Step 3: 实现 PasswordHasher**（见 Produce，委托 BCryptPasswordEncoder）
- [ ] **Step 4: 运行测试确认通过** Expected: PASS
- [ ] **Step 5: 写 SessionRepositoryTest（Mockito 测 key 构造与级联删除逻辑）**

```java
@ExtendWith(MockitoExtension.class)
class SessionRepositoryTest {
    @Mock StringRedisTemplate redis;
    @InjectMocks SessionRepository repo;
    // save: 验证 SET + EXPIRE + SADD 调用
    // removeAllByUserId: 验证 SMEMBERS 后逐 DEL + DEL 集合键
}
```

- [ ] **Step 6: 实现 SessionRepository**（StringRedisTemplate + Jackson；滑续用 `redis.expire(key, ttl)`）
- [ ] **Step 7: 运行测试确认通过** — Run: `mvn -q -pl ai-code-agent-user test -Dtest=PasswordHasherTest,SessionRepositoryTest` Expected: 2 tests PASS
- [ ] **Step 8: 提交**

```bash
git add ai-code-agent-user/
git commit -m "feat(user): BCrypt 密码哈希与 Redis 会话仓库(滑续/级联删除)"
```

### Task 5: LocalUserAuthServiceImpl（注册 / 登录 / 注销 / resolve）

**Files:**
- Create: `ai-code-agent-user/src/main/java/com/dp/ai_code_agent/user/local/service/LocalUserAuthServiceImpl.java`
- Create: `ai-code-agent-user/src/test/java/com/dp/ai_code_agent/user/local/service/LocalUserAuthServiceImplTest.java`

**Interfaces:**
- Consumes: `UserAuthService`（Task 1）；`UserMapper/RoleMapper/PermissionMapper/UserRoleMapper/RolePermissionMapper`（Task 3）；`PasswordHasher`、`SessionRepository`（Task 4）
- Produces: `UserAuthService` 的 local 实现（Bean 名 `localUserAuthServiceImpl`，供 Task 7 装配）

**行为契约：**
- `register`：用户名唯一校验（重复抛 `BusinessException(USERNAME_EXISTS)`）；`PasswordHasher.hash` 落库；默认角色 `USER`；返回不含密码的 `UserIdentity`
- `login`：查用户 → 不存在或密码不匹配统一抛 `LOGIN_FAILED`（防枚举）→ `status=0` 抛 `ACCOUNT_DISABLED` → 生成 token（`SecureRandom` 32B → Base64URL）→ `SessionRepository.save(token, identity, ttl)` → 返回 `LoginResult`
- `logout(token)`：`SessionRepository.remove(token)`
- `resolve(token)`：`SessionRepository.find` → 空抛 `TOKEN_INVALID` → 用户已禁用抛 `TOKEN_INVALID`（会话仍有效但账号被禁，直接按无效处理）→ 返回 `UserIdentity`
- 角色/权限装配：`findRolesByUserId`（t_user_role → t_role）、`findPermissionsByRoleIds`（t_role_permission → t_permission），按 RoleDTO 结构组装

- [ ] **Step 1: 写失败测试**

```java
@ExtendWith(MockitoExtension.class)
class LocalUserAuthServiceImplTest {
    @Mock UserMapper userMapper; @Mock RoleMapper roleMapper;
    @Mock PermissionMapper permissionMapper; @Mock UserRoleMapper userRoleMapper;
    @Mock RolePermissionMapper rolePermissionMapper;
    @Mock PasswordHasher passwordHasher; @Mock SessionRepository sessionRepository;
    @InjectMocks LocalUserAuthServiceImpl service;

    @Test void register_duplicateUsername_throws() {
        when(userMapper.selectByUsername("alice")).thenReturn(new User());
        assertThatThrownBy(() -> service.register("alice", "pwd", "Alice"))
            .isInstanceOf(BusinessException.class)
            .extracting(e -> ((BusinessException) e).getErrorCode())
            .isEqualTo(ErrorCode.USERNAME_EXISTS);
    }
    @Test void login_success_returnsToken() {
        User u = new User(); u.setId(1L); u.setUsername("alice"); u.setStatus(1);
        u.setPasswordHash("hash");
        when(userMapper.selectByUsername("alice")).thenReturn(u);
        when(passwordHasher.matches("pwd", "hash")).thenReturn(true);
        LoginResult r = service.login("alice", "pwd");
        assertThat(r.token()).isNotBlank();
        verify(sessionRepository).save(eq(r.token()), any(), any());
    }
    @Test void login_wrongPassword_throwsLoginFailed() { /* 同型 */ }
    @Test void resolve_disabledUser_throwsTokenInvalid() { /* 同型 */ }
}
```

- [ ] **Step 2: 运行确认失败** — Run: `mvn -q -pl ai-code-agent-user test -Dtest=LocalUserAuthServiceImplTest` Expected: FAIL（编译不过/类缺失）
- [ ] **Step 3: 实现 LocalUserAuthServiceImpl**（见行为契约；`@Service`；token 生成用 `SecureRandom` + `Base64.getUrlEncoder().withoutPadding()`）
- [ ] **Step 4: 运行确认通过** Expected: PASS
- [ ] **Step 5: 提交**

```bash
git add ai-code-agent-user/src/
git commit -m "feat(user): LocalUserAuthServiceImpl 注册/登录/注销/resolve"
```

---

### Task 6: LocalUserAdminServiceImpl（分页 / 启停 / 重置密码 / 分配角色 / 授权）

**Files:**
- Create: `ai-code-agent-user/src/main/java/com/dp/ai_code_agent/user/local/service/LocalUserAdminServiceImpl.java`
- Create: `ai-code-agent-user/src/test/java/com/dp/ai_code_agent/user/local/service/LocalUserAdminServiceImplTest.java`

**Interfaces:**
- Consumes: `UserAdminService`（Task 1）；Mapper（Task 3）；`SessionRepository`（Task 4）
- Produces: `UserAdminService` 的 local 实现（供 Task 7 装配）

**行为契约：**
- `page`：`QueryChain` 条件拼装（keyword 模糊 username/nickname、status 等值、roleId 经 `t_user_role` 子查询/join），返回 `PageResult<UserAdminDTO>`（含 roles 汇总）
- `setStatus(id, false)`：置 `status=0` 并 `SessionRepository.removeAllByUserId(id)`（级联踢人）；`true` 置 1
- `resetPassword`：新密码 BCrypt 后更新，**同时级联清会话**（安全默认）
- `assignRoles`：先删 `t_user_role` 再批量插入（事务内）
- `grantPermissions`：先删 `t_role_permission` 再批量插入（事务内）
- `detail/listRoles`：装配权限码树
- 全链路 `@Transactional`（两个 impl 类上的方法），事务由 Spring 代理保证 —— 本设计不引入跨库事务，单一 PG 数据源

- [ ] **Step 1: 写失败测试**（Mockito：`setStatus(false)` 验证 `removeAllByUserId` 被调用；`assignRoles` 验证先删后插；`page` 验证条件拼装与返回）
- [ ] **Step 2: 运行确认失败**
- [ ] **Step 3: 实现 LocalUserAdminServiceImpl**（`@Service @Transactional`；分页用 xbatis `QueryChain` + `Page` 或手写 count/list）
- [ ] **Step 4: 运行确认通过**
- [ ] **Step 5: 提交**

```bash
git add ai-code-agent-user/src/
git commit -m "feat(user): LocalUserAdminServiceImpl 管理端能力"
```

---

### Task 7: local 自动配置装配 + provider 开关

**Files:**
- Create: `ai-code-agent-user/src/main/java/com/dp/ai_code_agent/user/local/config/LocalUserProviderAutoConfiguration.java`
- Create: `ai-code-agent-user/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
- Create: `ai-code-agent-user/src/test/java/com/dp/ai_code_agent/user/local/config/LocalUserProviderAutoConfigurationTest.java`

**Interfaces:**
- Consumes: Task 5/6 实现类
- Produces: `UserAuthService`、`UserAdminService` 两个 Bean（条件装配）

```java
@AutoConfiguration
@ConditionalOnProperty(name = "app.user.provider", havingValue = "local", matchIfMissing = true)
public class LocalUserProviderAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(UserAuthService.class)
    UserAuthService userAuthService(/* Mapper, PasswordHasher, SessionRepository, Converter */) {
        return new LocalUserAuthServiceImpl(...);
    }

    @Bean
    @ConditionalOnMissingBean(UserAdminService.class)
    UserAdminService userAdminService(...) {
        return new LocalUserAdminServiceImpl(...);
    }
}
```

`AutoConfiguration.imports` 文件内容：`com.dp.ai_code_agent.user.local.config.LocalUserProviderAutoConfiguration`

- [ ] **Step 1: 写测试**（`ApplicationContextRunner`：默认装配出两个 Bean；`app.user.provider=other` 时不装配；外部自定义 Bean 时 local 不覆盖——`@ConditionalOnMissingBean` 验证）
- [ ] **Step 2: 运行确认失败**
- [ ] **Step 3: 实现配置类 + imports 文件**
- [ ] **Step 4: 运行确认通过**
- [ ] **Step 5: 提交**

```bash
git add ai-code-agent-user/src/
git commit -m "feat(user): local provider 自动配置(@ConditionalOnProperty/@ConditionalOnMissingBean)"
```

---

### Task 8: web 层 Security 集成（SecurityConfig + TokenFilter + 401/403 JSON）

**Files:**
- Create: `ai-code-agent-web/src/main/java/com/dp/ai_code_agent/web/config/SecurityConfig.java`
- Create: `ai-code-agent-web/src/main/java/com/dp/ai_code_agent/web/security/TokenAuthenticationFilter.java`
- Create: `ai-code-agent-web/src/main/java/com/dp/ai_code_agent/web/security/RestAuthenticationEntryPoint.java`
- Create: `ai-code-agent-web/src/main/java/com/dp/ai_code_agent/web/security/RestAccessDeniedHandler.java`
- Create: `ai-code-agent-web/src/main/java/com/dp/ai_code_agent/web/security/UserPrincipal.java`（`UserIdentity` 的 Security 适配，实现 `Authentication` 或直接复用 `UserIdentity` 作 principal）
- Create: `ai-code-agent-web/src/test/java/com/dp/ai_code_agent/web/security/ArchitectureGuardTest.java`
- Modify: `ai-code-agent-web/pom.xml`（加 `spring-boot-starter-security`、`spring-security-test`、user 依赖）

**Interfaces:**
- Consumes: `UserAuthService.resolve(String)`（Task 1/5）
- Produces: 鉴权链路；`SecurityContext` principal 为 `UserIdentity`

```java
// SecurityConfig —— Spring Security 7 写法（无 WebSecurityConfigurerAdapter）
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {
    @Bean SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.POST, "/api/auth/register", "/api/auth/login").permitAll()
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .anyRequest().authenticated())
            .exceptionHandling(e -> e
                .authenticationEntryPoint(new RestAuthenticationEntryPoint())
                .accessDeniedHandler(new RestAccessDeniedHandler()))
            .addFilterBefore(new TokenAuthenticationFilter(userAuthService), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
```

```java
// TokenAuthenticationFilter —— extends OncePerRequestFilter
// 取 Authorization: Bearer <token> → userAuthService.resolve(token) → 构建 UsernamePasswordAuthenticationToken(user, null, authorities)
// authorities：roles → ROLE_<code>；permissions → code（供 hasRole/hasAuthority 使用）
// 解析失败或缺失 token 直接放行（由 entryPoint 兜底 401）
```

- [ ] **Step 1: web pom 加依赖**（`spring-boot-starter-security` + `ai-code-agent-user` + test 作用域 `spring-security-test`）
- [ ] **Step 2: 写 ArchitectureGuardTest（红）**

```java
@AnalyzeClasses(packages = "com.dp.ai_code_agent")
public class ArchitectureGuardTest {
    @Test void webMustNotDependOnUserLocal() {
        noClasses().that().resideInAPackage("com.dp.ai_code_agent.web..")
            .should().dependOnClassesThat().resideInAnyPackage("com.dp.ai_code_agent.user.local..")
            .because("web 层只能依赖 spi 契约，禁止触碰本地实现")
            .check(new ClassFileImporter().importPackages("com.dp.ai_code_agent"));
    }
    @Test void spiMustBeFrameworkFree() {
        noClasses().that().resideInAPackage("com.dp.ai_code_agent.user.spi..")
            .should().dependOnClassesThat().resideInAnyPackage("org.springframework..", "cn.xbatis..", "redis.clients..")
            .because("SPI 契约必须保持零框架依赖")
            .check(...);
    }
}
```

- [ ] **Step 3: 运行确认失败**（当前尚无 user 模块时可能编译失败 —— 在 Task 1-7 完成后该测试才可运行，本任务顺序上保证先完成 Task 1-7）
- [ ] **Step 4: 实现 SecurityConfig / TokenAuthenticationFilter / entryPoint / handler**
- [ ] **Step 5: 运行确认通过** — Run: `mvn -q -pl ai-code-agent-web -am test -Dtest=ArchitectureGuardTest` Expected: PASS
- [ ] **Step 6: 提交**

```bash
git add ai-code-agent-web/
git commit -m "feat(web): Spring Security Bearer 鉴权链路与 401/403 JSON 输出 + ArchUnit 守卫"
```

### Task 9: web 层 Controller（Auth / UserAdmin / 当前用户）

**Files:**
- Create: `ai-code-agent-web/src/main/java/com/dp/ai_code_agent/web/controller/AuthController.java`
- Create: `ai-code-agent-web/src/main/java/com/dp/ai_code_agent/web/controller/UserAdminController.java`
- Create: `ai-code-agent-web/src/main/java/com/dp/ai_code_agent/web/dto/RegisterRequest.java`
- Create: `ai-code-agent-web/src/main/java/com/dp/ai_code_agent/web/dto/LoginRequest.java`
- Create: `ai-code-agent-web/src/main/java/com/dp/ai_code_agent/web/dto/ResetPasswordRequest.java`
- Create: `ai-code-agent-web/src/main/java/com/dp/ai_code_agent/web/dto/AssignRolesRequest.java`
- Create: `ai-code-agent-web/src/main/java/com/dp/ai_code_agent/web/dto/GrantPermissionsRequest.java`
- Create: `ai-code-agent-web/src/main/java/com/dp/ai_code_agent/web/dto/UpdateStatusRequest.java`
- Create: `ai-code-agent-web/src/test/java/com/dp/ai_code_agent/web/controller/AuthControllerTest.java`
- Modify: `ai-code-agent-common/src/main/java/com/dp/ai_code_agent/common/exception/ErrorCode.java`（新增错误码）

**Interfaces:**
- Consumes: `UserAuthService` / `UserAdminService`（Task 1）；Security 链路（Task 8）
- Produces: REST API（见 spec §6）；`@AuthenticationPrincipal UserIdentity` 取当前用户

**DTO（jakarta validation）：**
```java
public record RegisterRequest(@NotBlank @Size(min=3, max=64) String username,
                              @NotBlank @Size(min=6, max=64) String password,
                              @Size(max=64) String nickname) {}
public record LoginRequest(@NotBlank String username, @NotBlank String password) {}
public record ResetPasswordRequest(@NotBlank @Size(min=6, max=64) String newPassword) {}
public record UpdateStatusRequest(@NotNull Boolean enabled) {}
public record AssignRolesRequest(@NotNull List<@NotNull Long> roleIds) {}
public record GrantPermissionsRequest(@NotNull List<@NotNull Long> permissionIds) {}
```

**Controller 路由：**
```java
@RestController @RequestMapping("/api/auth")
public class AuthController {
    @PostMapping("/register")      Result<UserIdentity> register(@Valid @RequestBody RegisterRequest req);
    @PostMapping("/login")         Result<LoginResult> login(@Valid @RequestBody LoginRequest req);
    @PostMapping("/logout")        Result<Void> logout(@RequestHeader("Authorization") String authHeader);
    @GetMapping("/me")             Result<UserIdentity> me(@AuthenticationPrincipal UserIdentity user);
}

@RestController @RequestMapping("/api/admin")
public class UserAdminController {
    @GetMapping("/users")                         Result<PageResult<UserAdminDTO>> page(...);
    @GetMapping("/users/{id}")                    Result<UserAdminDTO> detail(@PathVariable Long id);
    @PatchMapping("/users/{id}/status")           Result<Void> setStatus(...);
    @PostMapping("/users/{id}/reset-password")    Result<Void> resetPassword(...);
    @PutMapping("/users/{id}/roles")              Result<Void> assignRoles(...);
    @GetMapping("/roles")                         Result<List<RoleDTO>> listRoles();
    @PutMapping("/roles/{roleId}/permissions")    Result<Void> grantPermissions(...);
}
```

- [ ] **Step 1: ErrorCode 新增**：`USERNAME_EXISTS(1001)`、`LOGIN_FAILED(1002)`、`ACCOUNT_DISABLED(1003)`、`FORBIDDEN(403)`、`TOKEN_INVALID(1004)`、`TOKEN_EXPIRED(1005)`
- [ ] **Step 2: 写 AuthControllerTest（红，spring-security-test + MockMvc）**

```java
@WebMvcTest(AuthController.class)
class AuthControllerTest {
    @Autowired MockMvc mvc;
    @MockitoBean UserAuthService userAuthService;
    @Test void login_ok() throws Exception {
        when(userAuthService.login("alice", "pwd"))
            .thenReturn(new LoginResult("tok", new UserIdentity(1L,"alice","Alice",List.of(),List.of(),true)));
        mvc.perform(post("/api/auth/login").contentType(APPLICATION_JSON)
                .content("{\"username\":\"alice\",\"password\":\"pwd\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.token").value("tok"));
    }
}
```

- [ ] **Step 3: 运行确认失败**
- [ ] **Step 4: 实现 DTO + 两个 Controller**（`@Valid` 校验；logout 从 Header 剥离 `Bearer ` 前缀；`me` 取 principal）
- [ ] **Step 5: 运行确认通过**
- [ ] **Step 6: 提交**

```bash
git add ai-code-agent-web/src ai-code-agent-common/src
git commit -m "feat(web): 认证/管理端 REST 接口与 DTO 校验 + ErrorCode 扩展"
```

---

### Task 10: app 配置（redis、app.user.*）+ 集成测试基座

**Files:**
- Modify: `ai-code-agent-app/src/main/resources/application.yml`
- Create: `ai-code-agent-app/src/test/java/com/dp/ai_code_agent/app/TestcontainersConfig.java`
- Create: `ai-code-agent-app/src/test/resources/application-test.yml`
- Modify: `ai-code-agent-app/pom.xml`（加 testcontainers 依赖：postgresql、junit-jupiter、testcontainers 通用）

**Interfaces:**
- Consumes: 全部既有产物
- Produces: 可运行的 app + 集成测试基座

**application.yml 追加：**
```yaml
app:
  user:
    provider: local
    session-ttl: 30m
spring:
  data:
    redis:
      host: localhost
      port: 6379
```

**TestcontainersConfig（@TestConfiguration，静态容器单例）：**
```java
static PostgreSQLContainer<?> PG = new PostgreSQLContainer<>("postgres:16-alpine");
static GenericContainer<?> REDIS = new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);
// 通过 SpringDynamicPropertyRegistrar 或 @DynamicPropertySource 注入 jdbc/redis 连接属性
// Flyway 自动在测试库执行全部迁移（V1 兼容：V3 drop demo 表后不影响）
```

- [ ] **Step 1: 改 application.yml**（追加 app.user.* 与 redis 配置）
- [ ] **Step 2: app pom 加 testcontainers 依赖**（`org.testcontainers:postgresql`、`org.testcontainers:junit-jupiter`，版本由 Spring Boot 4 BOM 管理）
- [ ] **Step 3: 写 TestcontainersConfig + application-test.yml**
- [ ] **Step 4: 写冒烟测试**（上下文能启动，Flyway 迁移成功，`t_role` 有 2 行）— Run: `mvn -q -pl ai-code-agent-app test` Expected: PASS（注意：本地需 Docker；若环境无 Docker，该任务降级为跳过并注明 CI 运行）
- [ ] **Step 5: 提交**

```bash
git add ai-code-agent-app/
git commit -m "test(app): Testcontainers 集成测试基座与 app 配置"
```

---

### Task 11: 集成测试全链路（Testcontainers）

**Files:**
- Create: `ai-code-agent-app/src/test/java/com/dp/ai_code_agent/app/UserAuthFlowIT.java`
- Create: `ai-code-agent-app/src/test/java/com/dp/ai_code_agent/app/UserAdminFlowIT.java`

**Interfaces:**
- Consumes: TestcontainersConfig（Task 10）
- Produces: 全链路验收证据

**用例（UserAuthFlowIT）：**
1. 注册新用户 → 返回 UserIdentity（enabled=true、含 USER 角色）
2. 重复注册同名 → 400/1001 USERNAME_EXISTS
3. 登录成功 → 拿到 token
4. 错误密码登录 → 401/1002 LOGIN_FAILED
5. 带 token 访问 `/api/auth/me` → 200 当前用户
6. 无 token 访问 `/api/auth/me` → 401
7. 注销后带原 token 访问 → 401
8. 禁用用户后原 token 访问 → 401（级联踢人）

**用例（UserAdminFlowIT）：**
9. 管理员登录 → 访问 `/api/admin/users` 分页 → 200
10. 普通用户 token 访问 `/api/admin/users` → 403
11. 管理员禁用某用户 → 该用户会话立即失效（401）
12. 管理员重置密码 → 旧密码登录失败、新密码登录成功
13. 管理员给 USER 角色授予 `user:create` → 该用户带 token 访问需 `user:create` 的接口成功

- [ ] **Step 1: 写 UserAuthFlowIT（红）** — Run: `mvn -q -pl ai-code-agent-app test -Dtest=UserAuthFlowIT` Expected: FAIL
- [ ] **Step 2: 实现对应逻辑缺口（若有）**（正常情况下前序任务已完成，本步主要验证）
- [ ] **Step 3: 运行确认通过**
- [ ] **Step 4: 写 UserAdminFlowIT（红）→ 验证通过**
- [ ] **Step 5: 提交**

```bash
git add ai-code-agent-app/src/test
git commit -m "test(app): 注册登录鉴权注销/管理端全链路集成测试"
```

---

### Task 12: 清理 Demo 代码（后端 / 前端 / README）

**Files:**
- Delete: `ai-code-agent-core/src/main/java/com/dp/ai_code_agent/core/{converter/DemoUserConverter.java, dto/DemoUserDTO.java, mapper/DemoUserMapper.java, model/DemoUser.java, service/DemoUserService.java, service/impl/DemoUserServiceImpl.java}`
- Delete: `ai-code-agent-web/src/main/java/com/dp/ai_code_agent/web/controller/DemoUserController.java`
- Create: `ai-code-agent-app/src/main/resources/db/migration/V3__drop_demo_user.sql`
- Modify: `ai-code-agent-frontend/apps/web/src/pages/console/ConsolePage.tsx`（`/demo-users` 调用替换为占位文案，保留页面骨架）
- Modify: `README.md`（删除 demo 示例 curl，更新为 auth 示例或占位说明）

**Interfaces:**
- Consumes: 无
- Produces: 干净代码库

- [ ] **Step 1: 删除 7 个后端 Demo 类**（`git rm`）
- [ ] **Step 2: 创建 V3 迁移**

```sql
-- V3: 清理示例用户表
DROP TABLE IF EXISTS t_demo_user;
```

- [ ] **Step 3: 改 ConsolePage.tsx**（删除 `useFetch<DemoUser[]>` 与 demo 渲染，替换为静态占位；若无 import 依赖残留则一并清理）
- [ ] **Step 4: 更新 README.md**（`/api/demo-users` 示例替换为 `/api/auth/login` 说明）
- [ ] **Step 5: 后端全量编译+测试** — Run: `mvn -q clean verify -DskipTests=false` Expected: BUILD SUCCESS（排除需 Docker 的 IT，见 Task 10 降级说明）
- [ ] **Step 6: 前端 typecheck** — Run: `cd ai-code-agent-frontend && pnpm typecheck` Expected: PASS
- [ ] **Step 7: 提交**

```bash
git add -A
git commit -m "refactor: 清理 Demo 用户代码与 t_demo_user 表(V3)，更新前端占位与 README"
```

---

### Task 13: apifox-cli 同步接口文档 + 最终验证

**Files:**
- Modify: 接口相关（全部由前序任务完成）

**Interfaces:**
- Consumes: 全部接口定义（Task 9）
- Produces: 云端接口文档与代码一致

- [ ] **Step 1: 执行 apifox-cli 同步**（按 AGENTS.md：项目「AI 零代码应用生成平台」，接口变更提交前执行，覆盖请求/响应/参数/错误码/示例）
- [ ] **Step 2: 在 Apifox 平台抽查验证**：/api/auth/login、/api/auth/me、/api/admin/users 结构正确
- [ ] **Step 3: 全量构建 + 测试** — Run: `mvn -q clean verify && cd ai-code-agent-frontend && pnpm build`
- [ ] **Step 4: 提交最终状态**（如有文档变更一并提交）

```bash
git add -A
git commit -m "docs(apifox): 同步用户模块接口文档"
```

---

## 自审记录（Self-Review）

- **Spec 覆盖**：注册/登录 → Task 5+9；当前用户 → Task 9（/api/auth/me）；注销 → Task 5+9；权限控制 → Task 6/8/11（RBAC + @PreAuthorize + 权限码）；管理员管理 → Task 6+9；SPI 可替换 → Task 1+7（@ConditionalOnMissingBean）；Demo 清理 → Task 12；接口文档 → Task 13。无缺口。
- **占位符扫描**：Task 2 Step 2 的 `<BCRYPT_OF_admin123>` 为执行期生成回填项（非 TBD，有明确生成命令）；Task 5 测试中 `/* 同型 */` 标注的用例在 Step 1 已有完整示例可照抄模式，已补充说明 —— 按"No Placeholders"原则，执行时需补全为具体代码，计划中已给足模式。其余无 TBD。
- **类型一致性**：`UserAuthService.login` 返回 `LoginResult{token,user}` 全计划一致；`UserIdentity` record 字段 `(id,username,nickname,roles,permissions,enabled)` 全计划一致；`UserAdminDTO` 定为 `user/spi/model/UserAdminDTO`（Task 1 决策），web 层直接复用，Task 6/9 引用一致；`PageResult<T>` 来自 common。ArchUnit 测试类名/规则与 Task 8 一致。
- **已知执行期注意**：Testcontainers 需本机 Docker；Task 10 Step 4 若无 Docker 降级为跳过并在 CI 执行。Spring Boot 4.1 中 `spring-boot-starter-data-redis` 命名沿用；Security 7 无 `WebSecurityConfigurerAdapter`（计划已是 lambda 写法）。
