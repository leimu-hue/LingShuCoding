-- 初始结构：示例用户表
CREATE TABLE t_demo_user (
    id   BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    age  INT          NOT NULL DEFAULT 0
);

INSERT INTO t_demo_user (name, age) VALUES ('zhangsan', 18);
INSERT INTO t_demo_user (name, age) VALUES ('lisi', 19);