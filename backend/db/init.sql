-- =====================================================================
-- easy-crm 系统表初始化（MySQL 8.4 / utf8mb4 / InnoDB）
-- 所有表统一携带 BaseEntity 审计列：create_time / update_time / create_by /
-- update_by / version（乐观锁）/ deleted（逻辑删除），主键自增。
-- -- =====================================================================

SET NAMES utf8mb4;

-- ---------------------------------------------------------------- 部门
CREATE TABLE IF NOT EXISTS sys_dept (
    id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    pid         BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '上级部门 ID，0 为根',
    name        VARCHAR(64)     NOT NULL COMMENT '部门名称',
    sort        INT             NOT NULL DEFAULT 0 COMMENT '排序',
    status      TINYINT         NOT NULL DEFAULT 1 COMMENT '1 正常，0 禁用',
    remark      VARCHAR(255)    NULL COMMENT '备注',
    create_time DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    create_by   BIGINT          NULL,
    update_by   BIGINT          NULL,
    version     INT             NOT NULL DEFAULT 0,
    deleted     TINYINT         NOT NULL DEFAULT 0 COMMENT '0 未删除，1 已删除',
    PRIMARY KEY (id),
    KEY idx_pid (pid)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT ='部门';

-- ---------------------------------------------------------------- 用户
CREATE TABLE IF NOT EXISTS sys_user (
    id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    username      VARCHAR(64)     NOT NULL COMMENT '登录账号',
    password      VARCHAR(100)    NOT NULL COMMENT 'BCrypt 密文',
    nickname      VARCHAR(64)     NULL COMMENT '昵称',
    avatar        VARCHAR(255)    NULL COMMENT '头像地址',
    phone         VARCHAR(32)     NULL,
    email         VARCHAR(128)    NULL,
    dept_id       BIGINT UNSIGNED NULL COMMENT '所属部门 ID',
    role          VARCHAR(64)     NULL COMMENT '主角色标识，兜底用；优先取 sys_user_role',
    status        TINYINT         NOT NULL DEFAULT 1 COMMENT '1 正常，0 禁用',
    last_login_at DATETIME        NULL COMMENT '最后登录时间',
    create_time   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    create_by     BIGINT          NULL,
    update_by     BIGINT          NULL,
    version       INT             NOT NULL DEFAULT 0,
    deleted       TINYINT         NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username),
    KEY idx_dept_id (dept_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT ='用户';

-- ---------------------------------------------------------------- 角色
CREATE TABLE IF NOT EXISTS sys_role (
    id         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    name       VARCHAR(64)     NOT NULL COMMENT '角色名称',
    code       VARCHAR(64)     NOT NULL COMMENT '角色编码，如 admin',
    data_scope TINYINT         NOT NULL DEFAULT 1 COMMENT '数据范围：1 本人 / 2 本部门 / 3 本部门及下级 / 4 全部',
    sort       INT             NOT NULL DEFAULT 0,
    status     TINYINT         NOT NULL DEFAULT 1 COMMENT '1 正常，0 禁用',
    remark     VARCHAR(255)    NULL,
    create_time DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    create_by  BIGINT          NULL,
    update_by  BIGINT          NULL,
    version    INT             NOT NULL DEFAULT 0,
    deleted    TINYINT         NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_code (code)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT ='角色';

-- ---------------------------------------------------------------- 用户-角色
CREATE TABLE IF NOT EXISTS sys_user_role (
    id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    user_id     BIGINT UNSIGNED NOT NULL,
    role_id     BIGINT UNSIGNED NOT NULL,
    create_time DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    create_by   BIGINT          NULL,
    update_by   BIGINT          NULL,
    version     INT             NOT NULL DEFAULT 0,
    deleted     TINYINT         NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_role (user_id, role_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT ='用户-角色关联';

-- ---------------------------------------------------------------- 权限（菜单+按钮）
-- 字段与 vben 路由 + meta 对齐，type：catalog 目录 / menu 菜单 / button 按钮 / link 外链 / embedded 内嵌
CREATE TABLE IF NOT EXISTS sys_permission (
    id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    pid         BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '上级权限 ID，0 为根',
    name        VARCHAR(64)     NOT NULL COMMENT '路由名称，全局唯一',
    title       VARCHAR(64)     NULL COMMENT '菜单标题',
    type        VARCHAR(16)     NOT NULL DEFAULT 'menu' COMMENT 'catalog/menu/button/link/embedded',
    auth_code   VARCHAR(128)    NULL COMMENT '权限标识，如 system:user:create',
    path        VARCHAR(128)    NULL COMMENT '路由路径',
    component   VARCHAR(128)    NULL COMMENT '组件路径，相对前端 src/views',
    icon        VARCHAR(64)     NULL,
    order_no    INT             NOT NULL DEFAULT 0 COMMENT '排序值，越小越前',
    status      TINYINT         NOT NULL DEFAULT 1 COMMENT '1 正常，0 禁用',
    badge       VARCHAR(32)     NULL,
    badge_type  VARCHAR(16)     NULL,
    badge_variants VARCHAR(16)  NULL,
    keep_alive  TINYINT         NOT NULL DEFAULT 0 COMMENT '1 缓存页面',
    affix_tab   TINYINT         NOT NULL DEFAULT 0 COMMENT '1 固定标签页',
    hide_in_menu         TINYINT NOT NULL DEFAULT 0,
    hide_children_in_menu TINYINT NOT NULL DEFAULT 0,
    hide_in_breadcrumb   TINYINT NOT NULL DEFAULT 0,
    hide_in_tab          TINYINT NOT NULL DEFAULT 0,
    link        VARCHAR(255)    NULL COMMENT '外链地址',
    iframe_src  VARCHAR(255)    NULL COMMENT '内嵌 iframe 地址',
    create_time DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    create_by   BIGINT          NULL,
    update_by   BIGINT          NULL,
    version     INT             NOT NULL DEFAULT 0,
    deleted     TINYINT         NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_pid (pid),
    KEY idx_auth_code (auth_code)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT ='权限（菜单+按钮）';

-- ---------------------------------------------------------------- 角色-权限
CREATE TABLE IF NOT EXISTS sys_role_permission (
    id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    role_id       BIGINT UNSIGNED NOT NULL,
    permission_id BIGINT UNSIGNED NOT NULL,
    create_time   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    create_by     BIGINT          NULL,
    update_by     BIGINT          NULL,
    version       INT             NOT NULL DEFAULT 0,
    deleted       TINYINT         NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_role_permission (role_id, permission_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT ='角色-权限关联';

-- ---------------------------------------------------------------- 登录日志
CREATE TABLE IF NOT EXISTS sys_login_log (
    id         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    username   VARCHAR(64)     NOT NULL COMMENT '登录账号',
    user_id    BIGINT UNSIGNED NULL,
    ip         VARCHAR(64)     NULL,
    user_agent VARCHAR(255)    NULL,
    success    TINYINT         NOT NULL DEFAULT 0 COMMENT '1 成功 0 失败',
    message    VARCHAR(255)    NULL COMMENT '失败原因',
    created_at DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_username (username),
    KEY idx_created_at (created_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT ='登录日志';

-- ---------------------------------------------------------------- 操作日志
CREATE TABLE IF NOT EXISTS sys_oper_log (
    id         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    user_id    BIGINT UNSIGNED NULL,
    username   VARCHAR(64)     NULL,
    module     VARCHAR(64)     NULL COMMENT '业务模块（控制器 @Tag name）',
    action     VARCHAR(128)    NULL COMMENT '操作（接口 @Operation summary）',
    method     VARCHAR(10)     NULL,
    uri        VARCHAR(255)    NULL,
    params     TEXT            NULL COMMENT '请求参数（序列化截断）',
    result_code INT            NULL COMMENT '0 成功',
    error_msg  VARCHAR(500)    NULL,
    ip         VARCHAR(64)     NULL,
    cost_ms    INT             NULL COMMENT '耗时（毫秒）',
    created_at DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_username (username),
    KEY idx_created_at (created_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT ='操作日志';

-- =====================================================================
-- CRM 业务表
-- =====================================================================

-- ---------------------------------------------------------------- 线索
-- 线索 = 尚未验证的潜在客户，独立于客户体系；owner_id 为空表示在公海
CREATE TABLE IF NOT EXISTS crm_lead (
    id                       BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    name                     VARCHAR(128)    NOT NULL COMMENT '线索名称/公司名',
    contact_name             VARCHAR(64)     NULL COMMENT '联系人姓名',
    phone                    VARCHAR(32)     NULL COMMENT '手机号（业务层全局唯一）',
    email                    VARCHAR(128)    NULL COMMENT '邮箱（业务层全局唯一）',
    source                   TINYINT         NULL COMMENT '来源字典值，见 LeadSource 常量类（后续接字典表）',
    status                   TINYINT         NOT NULL DEFAULT 0 COMMENT '状态：0 新建 1 跟进中 2 已验证 3 不合格',
    owner_id                 BIGINT UNSIGNED NULL COMMENT '负责人（sys_user.id），NULL 表示公海',
    dept_id                  BIGINT UNSIGNED NULL COMMENT '归属部门（sys_dept.id）',
    remark                   VARCHAR(500)    NULL COMMENT '备注',
    converted_customer_id    BIGINT UNSIGNED NULL COMMENT '转化生成的客户 ID',
    converted_contact_id     BIGINT UNSIGNED NULL COMMENT '转化生成的联系人 ID',
    converted_opportunity_id BIGINT UNSIGNED NULL COMMENT '转化生成的商机 ID',
    last_follow_time         DATETIME        NULL COMMENT '最近跟进时间（公海回收依据）',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    create_by   BIGINT       NULL,
    update_by   BIGINT       NULL,
    version     INT          NOT NULL DEFAULT 0,
    deleted     TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    -- phone / email 唯一性在业务层校验（逻辑删除表不宜用 DB 唯一索引，否则已删记录会占用号码）
    KEY idx_phone (phone),
    KEY idx_email (email),
    KEY idx_owner_status (owner_id, status),
    KEY idx_name (name),
    KEY idx_status (status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT ='线索';

-- =====================================================================
-- 初始数据：admin / admin123（仅开发环境，上线前改密）
-- 密码为 BCrypt(cost=10)，与 SecurityConfig 的 BCryptPasswordEncoder 匹配
-- =====================================================================
INSERT INTO sys_dept (id, pid, name, sort, status, remark)
VALUES (1, 0, '总部', 0, 1, '根部门')
ON DUPLICATE KEY UPDATE name = VALUES(name);

INSERT INTO sys_user (username, password, nickname, dept_id, role, status)
VALUES ('admin', '$2y$10$mOxg6OagMn734O9oAia2Ie3OveTlHyXW/rSvjtO./wJMuPyZlq6Hq', '系统管理员', 1, 'admin', 1)
ON DUPLICATE KEY UPDATE nickname = VALUES(nickname);

INSERT INTO sys_role (id, name, code, data_scope, sort, status, remark)
VALUES (1, '超级管理员', 'admin', 4, 0, 1, '数据范围：全部')
ON DUPLICATE KEY UPDATE name = VALUES(name);

INSERT INTO sys_user_role (user_id, role_id)
SELECT u.id, r.id FROM sys_user u, sys_role r
WHERE u.username = 'admin' AND r.code = 'admin'
ON DUPLICATE KEY UPDATE role_id = VALUES(role_id);

-- 预置菜单（component 相对前端 src/views；admin 角色通过 ADMIN 兜底获得全部权限码）
INSERT INTO sys_permission (id, pid, name, title, type, auth_code, path, component, icon, order_no, status)
VALUES
    (1, 0, 'Dashboard',        '工作台',   'catalog', NULL,                '/dashboard',          NULL,                      'lucide:layout-dashboard', 0,  1),
    (2, 1, 'DashboardAnalytics','分析页',  'menu',    'dashboard:analytics','/dashboard/analytics','dashboard/analytics/index','lucide:chart-pie',       0,  1),
    (3, 0, 'System',           '系统管理', 'catalog', NULL,                '/system',             NULL,                      'lucide:settings',        100, 1),
    (4, 3, 'SystemDept',       '部门管理', 'menu',    'system:dept:list',  '/system/dept',        'system/dept/list',        'lucide:building-2',      10, 1),
    (5, 3, 'SystemUser',       '用户管理', 'menu',    'system:user:list',  '/system/user',        'system/user/list',        'lucide:users',           20, 1),
    (6, 3, 'SystemRole',       '角色管理', 'menu',    'system:role:list',  '/system/role',        'system/role/list',        'lucide:user-check',      30, 1),
    (7, 3, 'SystemMenu',       '菜单管理', 'menu',    'system:menu:list',  '/system/menu',        'system/menu/list',        'lucide:menu',            40, 1),
    (8, 3, 'SystemLoginLog',   '登录日志', 'menu',    'system:loginlog:list','/system/login-log', 'system/login-log/index',  'lucide:key-round',       50, 1),
    (9, 3, 'SystemOperLog',    '操作日志', 'menu',    'system:operlog:list','/system/oper-log',   'system/oper-log/index',   'lucide:scroll-text',     60, 1)
ON DUPLICATE KEY UPDATE title = VALUES(title), status = VALUES(status);
