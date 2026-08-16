# AI 零代码应用生成平台（AI Code Agent）

基于 **Spring Boot 4 + Spring AI + Xbatis** 的零代码应用生成平台。用户通过自然语言描述需求，由 AI 自动生成可运行的应用。

## 技术栈

| 分类   | 技术                                        |
|------|-------------------------------------------|
| 语言   | Java 25                                   |
| 框架   | Spring Boot 4.1                           |
| AI   | Spring AI 2.0（OpenAI / ChatGPT）           |
| ORM  | Xbatis（MyBatis 风格，无 XML）               |
| 数据库  | PostgreSQL，Flyway 管理迁移                    |
| 会话   | Redis（Opaque Token）                        |
| 对象映射 | MapStruct                                 |
| 构建   | Maven                                     |

## 项目结构

```
ai-code-agent/
├── ai-code-agent-common/   # 通用工具、常量、异常、统一响应体
├── ai-code-agent-core/     # 核心业务逻辑：实体、Mapper、Service、DTO、Converter
├── ai-code-agent-user/     # 用户模块：spi 契约 + local RBAC 实现（可替换）
├── ai-code-agent-web/      # Web 层：REST 控制器、Security、全局异常处理、CORS
└── ai-code-agent-app/      # 启动模块：入口、配置（application.yml）、数据库迁移脚本
```

## 快速开始

### 1. 环境准备

- JDK 25+
- Maven 3.9+
- PostgreSQL（默认 `localhost:5432`，库名 `ai_code_agent`）
- OpenAI API Key（可通过环境变量 `OPENAI_API_KEY` 注入）

### 2. 启动

```bash
mvn clean package
java -jar ai-code-agent-app/target/ai-code-agent-*.jar
```

应用默认监听 `http://localhost:8080`。

### 3. 验证

用户模块接口（详见 `docs/superpowers/specs/2026-08-16-user-module-design.md`）：

```bash
# 注册
curl -X POST http://localhost:8080/api/auth/register \
  -H 'Content-Type: application/json' \
  -d '{"username":"alice","password":"123456","nickname":"Alice"}'

# 登录（返回 token）
curl -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"admin123"}'

# 带 token 获取当前用户
curl http://localhost:8080/api/auth/me \
  -H 'Authorization: Bearer <token>'
```

## 配置说明

核心配置位于 `ai-code-agent-app/src/main/resources/application.yml`：

```yaml
spring:
  datasource:          # 数据库连接
  data.redis:          # 会话存储（Redis）
  ai.openai:           # OpenAI 配置（api-key 支持环境变量注入）

app.user:             # 用户模块：provider=local 为自研实现，可替换
web.cors:             # 全局跨域配置
  # allowed-origin-patterns: ["*"]
  # allowed-methods: GET/POST/PUT/DELETE/PATCH/OPTIONS
  # allow-credentials: true
  # max-age: 3600
```

## 数据库迁移

表结构与初始化数据由 **Flyway** 管理，迁移脚本位于：

```
ai-code-agent-app/src/main/resources/db/migration/
```

命名规范：`V<版本号>__<说明>.sql`。**禁止手改线上库结构**，变更一律通过新增迁移脚本完成。

## API 规范

- 统一响应包装：`Result<T>`（`code`、`message`、`data`），成功 `code = 0`
- 全局异常处理：`GlobalExceptionHandler` 统一处理业务/校验/系统异常
- 上下文：`/api`

## 开发工作流

按 `AGENTS.md` 规范，遵循 superpowers 技能流程：

1. `brainstorming` — 新功能先澄清需求、产出设计
2. `writing-plans` / `executing-plans` — 拆解并执行实施计划
3. `test-driven-development` — 实现阶段 RED-GREEN-REFACTOR
4. `systematic-debugging` — 修复前先根因分析
5. `verification-before-completion` — 完成前先验证

接口变更后，需通过 `apifox-cli` 同步更新云端接口文档（项目：AI 零代码应用生成平台）。

## 文档

- 架构与技术选型：`docs/architecture/`
- 贡献与编码规范：`docs/contributing/`
- 功能模块文档：`docs/features/<feature-name>/`