-- V2: 用户 RBAC 表结构 + 预置数据

-- 用户表
CREATE TABLE t_user (
    id            BIGSERIAL PRIMARY KEY,
    username      VARCHAR(64)  NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    nickname      VARCHAR(64),
    status        SMALLINT     NOT NULL DEFAULT 1, -- 1 正常 0 禁用
    created_at    TIMESTAMP    NOT NULL DEFAULT now(),
    updated_at    TIMESTAMP    NOT NULL DEFAULT now()
);

-- 角色表
CREATE TABLE t_role (
    id          BIGSERIAL PRIMARY KEY,
    code        VARCHAR(32) NOT NULL UNIQUE,
    name        VARCHAR(64) NOT NULL,
    description VARCHAR(255)
);

-- 权限表
CREATE TABLE t_permission (
    id          BIGSERIAL PRIMARY KEY,
    code        VARCHAR(64) NOT NULL UNIQUE,
    name        VARCHAR(64) NOT NULL,
    description VARCHAR(255)
);

-- 用户-角色关联表
CREATE TABLE t_user_role (
    user_id BIGINT NOT NULL REFERENCES t_user(id),
    role_id BIGINT NOT NULL REFERENCES t_role(id),
    PRIMARY KEY (user_id, role_id)
);

-- 角色-权限关联表
CREATE TABLE t_role_permission (
    role_id       BIGINT NOT NULL REFERENCES t_role(id),
    permission_id BIGINT NOT NULL REFERENCES t_permission(id),
    PRIMARY KEY (role_id, permission_id)
);

-- 预置角色
INSERT INTO t_role (code, name, description) VALUES
    ('ADMIN', '管理员', '平台管理员，拥有全部权限'),
    ('USER',  '普通用户', '默认注册角色');

-- 预置权限码
INSERT INTO t_permission (code, name) VALUES
    ('user:view', '查看用户'),
    ('user:create', '创建用户'),
    ('user:update', '编辑用户'),
    ('user:delete', '删除用户'),
    ('user:reset-password', '重置密码'),
    ('user:assign-role', '分配角色'),
    ('role:view', '查看角色'),
    ('role:grant', '角色授权');

-- ADMIN 拥有全部权限
INSERT INTO t_role_permission (role_id, permission_id)
SELECT r.id, p.id FROM t_role r JOIN t_permission p ON TRUE
WHERE r.code = 'ADMIN';

-- USER 默认仅拥有 user:view
INSERT INTO t_role_permission (role_id, permission_id)
SELECT r.id, p.id FROM t_role r JOIN t_permission p ON TRUE
WHERE r.code = 'USER' AND p.code = 'user:view';

-- 初始管理员 admin / admin123（开发期默认值，上线前必须重置）
INSERT INTO t_user (username, password_hash, nickname, status) VALUES
    ('admin', '$2a$10$H90WllZLsy6yXDD5r7YuL.6209gXNMNkbDBAYsK//cPIfvQDoNs2a', '管理员', 1);

INSERT INTO t_user_role (user_id, role_id)
SELECT u.id, r.id FROM t_user u, t_role r
WHERE u.username = 'admin' AND r.code = 'ADMIN';
