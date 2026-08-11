# Repository Guidelines

## 模块划分

- `common` – 通用工具、常量、异常等
- `web` – Web 层（REST API、过滤器、拦截器）
- `core` – 核心业务逻辑

## 子包约定

每个模块内部按职责分层，Java 包路径统一为 `com.dp.ai_code_agent.<module>`（注意：包名不允许连字符，使用下划线）：

```
<module>/
├── controller/      # REST 控制器（仅路由与参数绑定，仅 web 模块存在）
├── service/         # 业务逻辑
├── mapper/          # Xbatis Mapper（继承 cn.xbatis.core.mybatis.mapper.MybatisMapper<T>）
├── model/           # 实体类（@cn.xbatis.db.annotations.Table、@TableId）
├── dto/             # 请求/响应数据传输对象
├── converter/       # MapStruct 转换器
├── config/          # 模块级配置
└── repository/      # 数据访问层（如 Redis）
```

## 编码规范

### Java 代码

**必须**遵循 `skill://java-coding-standards-en` 技能。

### 对象映射

使用 **MapStruct** 进行对象转换，禁止使用 `new` + `set` 手动赋值：

```java
// ✅ 正确 — MapStruct
@Mapper(componentModel = "spring")
public interface UserConverter {
    UserDTO toDto(User user);
    User toEntity(CreateUserRequest request);
    List<UserDTO> toDtoList(List<User> users);
}

// ❌ 错误 — 手动 new + set
UserDTO dto = new UserDTO();
dto.setId(user.getId());
dto.setName(user.getName());
```

MapStruct 转换器统一放在各模块的 `converter/` 包下。

## 开发工作流（superpowers 技能）

按流程使用以下 `skill://` 技能:

1. `skill://brainstorming` — 任何新功能/修改行为前，先澄清需求并产出设计
2. `skill://writing-plans` — 设计确认后，将任务拆分为细粒度实施计划
3. `skill://executing-plans` — 按计划批量执行（Inline Execution，带检查点）。本项目未引入 `subagent-driven-development`，一律走此路径
4. `skill://test-driven-development` — 实现阶段强制 RED-GREEN-REFACTOR
5. `skill://systematic-debugging` — 遇到 bug/测试失败先根因分析，禁止直接猜改
6. `skill://verification-before-completion` — 声称"完成/修复/通过"前先运行验证命令，证据优先
7. `skill://finishing-a-development-branch` — 所有任务完成且验证通过后，决定合并/PR/保留

注意：未引入 `using-git-worktrees`（Windows 上体验不佳），`executing-plans` 第 1 步的隔离工作区创建可跳过。

## 文档管理

所有项目文档统一存放于 `docs/` 目录，按类型和功能模块分层：

- **通用文档**（根级）
    - `docs/architecture/` – 系统整体架构、技术选型
    - `docs/contributing/` – 贡献指南、开发流程、代码规范
    - `docs/ops/` – 部署、监控、运维手册

- **功能模块文档**
    - `docs/features/<feature-name>/` – 各功能的设计、实现、接口说明
    - 示例：`docs/features/user-auth/`、`docs/features/payment/`
    - 可采用 superpowers 技能进行增强

- **数据库迁移**：表结构与初始化数据由 **Flyway** 管理，迁移脚本统一存放于 `ai-code-agent-app/src/main/resources/db/migration/`（命名 `V<版本>__<说明>.sql`），禁止手改线上库结构。

## API 文档更新

当 **接口发生变更**（新增、修改、删除）时，**必须**使用 `skill://apifox-cli` 同步更新云端接口文档。

- **项目名称**：AI 零代码应用生成平台（不存在即创建项目）
- **操作时机**：在接口代码提交前，执行 apifox-cli 命令，确保云端文档与代码一致。
- **内容覆盖**：包括请求/响应结构、参数说明、错误码、示例等。
- **验证**：更新后需在 Apifox 平台确认文档生效，方可提交代码。

## 设计前端
当你需要设计前端界面的时候，**必须**使用 `skill://antd`

**你需要从 .agents/skills 加载上面所说的技能**