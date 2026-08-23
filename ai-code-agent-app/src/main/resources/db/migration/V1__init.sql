-- 初始结构：用户表（含公共审计字段与最小角色字段）
--
-- 说明：
--   本地仅做最小权限验证，user_role 区分管理员(ADMIN)与普通用户(USER)；
--   复杂权限后续通过第三方权限管理以 SPI 方式接入，不在此展开。
--   规范：所有字段 NOT NULL；表与字段均带 COMMENT。

CREATE TABLE t_user (
    id             BIGSERIAL PRIMARY KEY,
    username       VARCHAR(64)  NOT NULL,
    password_hash  VARCHAR(100) NOT NULL,
    nickname       VARCHAR(64)  NOT NULL DEFAULT '',
    user_role      VARCHAR(16)  NOT NULL DEFAULT 'USER',
    status         SMALLINT     NOT NULL DEFAULT 1,
    created_time   TIMESTAMP    NOT NULL DEFAULT now(),
    update_time    TIMESTAMP    NOT NULL DEFAULT now(),
    create_at      BIGINT       NOT NULL DEFAULT 0,
    update_user_id BIGINT       NOT NULL DEFAULT 0,
    is_deleted     BOOLEAN      NOT NULL DEFAULT false,
    CONSTRAINT uk_t_user_username UNIQUE (username)
);

COMMENT ON TABLE  t_user                IS '用户表';
COMMENT ON COLUMN t_user.id             IS '主键 ID';
COMMENT ON COLUMN t_user.username       IS '用户名（唯一）';
COMMENT ON COLUMN t_user.password_hash  IS '密码哈希（BCrypt）';
COMMENT ON COLUMN t_user.nickname       IS '昵称';
COMMENT ON COLUMN t_user.user_role      IS '用户角色：ADMIN 管理员 / USER 普通用户';
COMMENT ON COLUMN t_user.status         IS '状态：1 正常 / 0 禁用';
COMMENT ON COLUMN t_user.created_time   IS '创建时间';
COMMENT ON COLUMN t_user.update_time    IS '更新时间';
COMMENT ON COLUMN t_user.create_at      IS '创建人 ID（0 表示系统）';
COMMENT ON COLUMN t_user.update_user_id IS '更新人 ID（0 表示系统）';
COMMENT ON COLUMN t_user.is_deleted     IS '逻辑删除标记';

-- 初始管理员 admin / admin123（开发期默认值，上线前必须重置）
INSERT INTO t_user (username, password_hash, nickname, user_role, status) VALUES
    ('admin', '$2a$10$H90WllZLsy6yXDD5r7YuL.6209gXNMNkbDBAYsK//cPIfvQDoNs2a', '管理员', 'ADMIN', 1);
