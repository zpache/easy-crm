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

-- ---------------------------------------------------------------- 客户
-- 客户 = 已转化的企业实体，商机/订单/回款的挂载点
-- status：1 正常 / 2 公海 / 3 已合并（已合并不出现在默认列表，merged_to_id 指向合并目标）
CREATE TABLE IF NOT EXISTS crm_customer (
    id               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    name             VARCHAR(128)    NOT NULL COMMENT '客户名称',
    credit_code      VARCHAR(32)     NULL COMMENT '统一社会信用代码',
    industry         TINYINT         NULL COMMENT '行业字典值（CustomerDicts）',
    scale            TINYINT         NULL COMMENT '规模字典值',
    region           TINYINT         NULL COMMENT '区域字典值',
    level            TINYINT         NULL COMMENT '客户级别字典值',
    status           TINYINT         NOT NULL DEFAULT 1 COMMENT '1 正常 2 公海 3 已合并',
    owner_id         BIGINT UNSIGNED NULL COMMENT '负责人，NULL 表示公海',
    dept_id          BIGINT UNSIGNED NULL COMMENT '归属部门',
    merged_to_id     BIGINT UNSIGNED NULL COMMENT '合并目标客户 ID（status=3 时有效）',
    last_follow_time DATETIME        NULL COMMENT '最近跟进时间（公海回收依据）',
    remark           VARCHAR(500)    NULL COMMENT '备注',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    create_by   BIGINT       NULL,
    update_by   BIGINT       NULL,
    version     INT          NOT NULL DEFAULT 0,
    deleted     TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_name (name),
    KEY idx_credit_code (credit_code),
    KEY idx_owner_status (owner_id, status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT ='客户';

-- ---------------------------------------------------------------- 联系人
-- 主联系人唯一性：应用层 + 事务保证（先清后设）。
-- 理由：(customer_id, is_primary) 唯一索引会把同客户下多条 is_primary=0 的普通联系人判重；
-- MySQL 无部分索引，函数索引（CASE WHEN）可解但可读性/可移植性差；
-- CRM 并发量下「事务内先清后设 + 行锁」即可保证同一客户主联系人唯一。
CREATE TABLE IF NOT EXISTS crm_contact (
    id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    customer_id BIGINT UNSIGNED NOT NULL COMMENT '所属客户 ID',
    name        VARCHAR(64)     NOT NULL COMMENT '联系人姓名',
    phone       VARCHAR(32)     NULL COMMENT '手机号',
    email       VARCHAR(128)    NULL,
    position    VARCHAR(64)     NULL COMMENT '职位',
    is_primary  TINYINT         NOT NULL DEFAULT 0 COMMENT '1 主联系人（同客户唯一，应用层保证）',
    remark      VARCHAR(500)    NULL,
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    create_by   BIGINT       NULL,
    update_by   BIGINT       NULL,
    version     INT          NOT NULL DEFAULT 0,
    deleted     TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_customer_id (customer_id),
    KEY idx_phone (phone)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT ='联系人';

-- ---------------------------------------------------------------- 销售阶段配置
-- 初始化方式说明：采用 SQL 脚本种子（与项目其他初始数据一致，幂等 ON DUPLICATE KEY），
-- 不用 CommandLineRunner——避免应用启动时有隐式写库行为，阶段调整直接改库即可生效。
CREATE TABLE IF NOT EXISTS crm_opportunity_stage (
    id                BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    stage_code        VARCHAR(32)     NOT NULL COMMENT '阶段编码，全局唯一',
    stage_name        VARCHAR(64)     NOT NULL COMMENT '阶段名称',
    sort_no           INT             NOT NULL DEFAULT 0 COMMENT '排序，阶段流转按此前后',
    default_win_rate  INT             NOT NULL DEFAULT 0 COMMENT '默认赢单率（百分比）',
    is_won            TINYINT         NOT NULL DEFAULT 0 COMMENT '1 赢单终态',
    is_lost           TINYINT         NOT NULL DEFAULT 0 COMMENT '1 丢单终态',
    enabled           TINYINT         NOT NULL DEFAULT 1 COMMENT '1 启用',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    create_by   BIGINT       NULL,
    update_by   BIGINT       NULL,
    version     INT          NOT NULL DEFAULT 0,
    deleted     TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_stage_code (stage_code)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT ='商机销售阶段';

-- ---------------------------------------------------------------- 商机
-- win_rate 冗余当前阶段赢单率（阶段变更时同步），供预测汇总直接计算
CREATE TABLE IF NOT EXISTS crm_opportunity (
    id                  BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    name                VARCHAR(128)    NOT NULL COMMENT '商机名称',
    customer_id         BIGINT UNSIGNED NOT NULL COMMENT '关联客户（必须）',
    contact_id          BIGINT UNSIGNED NULL COMMENT '关联联系人（可选）',
    lead_id             BIGINT UNSIGNED NULL COMMENT '来源线索 ID（转化时回填）',
    stage_code          VARCHAR(32)     NOT NULL COMMENT '当前阶段编码（crm_opportunity_stage）',
    amount              DECIMAL(15, 2)  NULL COMMENT '预计金额',
    currency            VARCHAR(8)      NOT NULL DEFAULT 'CNY' COMMENT '币种，默认 CNY',
    win_rate            INT             NOT NULL DEFAULT 0 COMMENT '当前赢单率（冗余阶段配置）',
    expected_close_date DATE            NULL COMMENT '预计成交日期',
    actual_close_date   DATE            NULL COMMENT '实际成交日期（赢单时记录）',
    actual_amount       DECIMAL(15, 2)  NULL COMMENT '实际成交金额（赢单时记录）',
    lost_reason         VARCHAR(32)     NULL COMMENT '丢单原因字典值（LostReasons）',
    competitor          VARCHAR(128)    NULL COMMENT '竞争对手（丢单时可选）',
    owner_id            BIGINT UNSIGNED NULL COMMENT '负责人',
    dept_id             BIGINT UNSIGNED NULL COMMENT '归属部门',
    last_activity_at    DATETIME        NULL COMMENT '最近活动时间（停滞检测依据）',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    create_by   BIGINT       NULL,
    update_by   BIGINT       NULL,
    version     INT          NOT NULL DEFAULT 0,
    deleted     TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_customer_id (customer_id),
    KEY idx_owner_stage (owner_id, stage_code),
    KEY idx_expected_close_date (expected_close_date)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT ='商机';

-- ---------------------------------------------------------------- 提醒记录
CREATE TABLE IF NOT EXISTS crm_reminder (
    id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    biz_type    VARCHAR(32)     NOT NULL COMMENT '业务类型，如 opportunity_stale',
    biz_id      BIGINT UNSIGNED NOT NULL COMMENT '业务 ID',
    owner_id    BIGINT UNSIGNED NULL COMMENT '提醒接收人',
    remind_level TINYINT        NOT NULL DEFAULT 1 COMMENT '提醒等级：1 黄 2 橙 3 红',
    remind_time DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    read_flag   TINYINT         NOT NULL DEFAULT 0 COMMENT '0 未读 / 1 已读',
    content     VARCHAR(255)    NULL COMMENT '提醒内容',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    create_by   BIGINT       NULL,
    update_by   BIGINT       NULL,
    version     INT          NOT NULL DEFAULT 0,
    deleted     TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_owner_read (owner_id, read_flag)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT ='提醒记录';

-- ---------------------------------------------------------------- 跟进记录
-- 多态关联选型：biz_type + biz_id 字段方案（理由见 AGENTS/README）：
-- 跟进/待办跨线索、客户、商机、订单多类业务，字段方案索引友好（biz_type+biz_id），
-- 时间线聚合天然按此查询；项目全程无外键 + 逻辑删除体系也不适合 DB 外键约束。
CREATE TABLE IF NOT EXISTS crm_follow_record (
    id               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    biz_type         VARCHAR(32)     NOT NULL COMMENT '业务类型：lead / customer / opportunity / order（轮次5）',
    biz_id           BIGINT UNSIGNED NOT NULL COMMENT '业务 ID',
    follow_type      VARCHAR(32)     NOT NULL COMMENT '跟进类型字典：PHONE / VISIT / EMAIL / WECHAT / OTHER（后续迁字典表）',
    content          VARCHAR(1000)   NOT NULL COMMENT '跟进内容',
    next_follow_time DATETIME        NULL COMMENT '下次跟进时间（填写则自动生成待办任务）',
    owner_id         BIGINT UNSIGNED NULL COMMENT '跟进人',
    dept_id          BIGINT UNSIGNED NULL COMMENT '跟进人部门',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    create_by   BIGINT       NULL,
    update_by   BIGINT       NULL,
    version     INT          NOT NULL DEFAULT 0,
    deleted     TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_biz (biz_type, biz_id),
    KEY idx_owner (owner_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT ='跟进记录';

-- ---------------------------------------------------------------- 待办任务
-- status 落库仅 0 待办 / 1 已完成；「已过期」查询时动态计算（due_time < NOW 且未完成），
-- 避免每分钟定时任务扫表写状态（见 README 决策说明）。
CREATE TABLE IF NOT EXISTS crm_task (
    id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    title       VARCHAR(128)    NOT NULL COMMENT '任务标题',
    content     VARCHAR(1000)   NULL COMMENT '任务内容',
    due_time    DATETIME        NOT NULL COMMENT '截止时间',
    status      TINYINT         NOT NULL DEFAULT 0 COMMENT '0 待办 / 1 已完成（已过期动态计算）',
    biz_type    VARCHAR(32)     NULL COMMENT '关联业务类型（可空=独立待办）',
    biz_id      BIGINT UNSIGNED NULL COMMENT '关联业务 ID',
    owner_id    BIGINT UNSIGNED NULL COMMENT '负责人',
    source      VARCHAR(16)     NOT NULL DEFAULT 'MANUAL' COMMENT '来源：MANUAL 手动 / FOLLOW 跟进记录生成',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    create_by   BIGINT       NULL,
    update_by   BIGINT       NULL,
    version     INT          NOT NULL DEFAULT 0,
    deleted     TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_owner_status (owner_id, status),
    KEY idx_due_time (due_time)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT ='待办任务';

-- ---------------------------------------------------------------- 商机阶段变更日志
-- 与阶段变更同事务写入；时间线视图据此还原阶段历史
CREATE TABLE IF NOT EXISTS crm_opportunity_stage_log (
    id             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    opportunity_id BIGINT UNSIGNED NOT NULL COMMENT '商机 ID',
    from_stage     VARCHAR(32)     NULL COMMENT '原阶段编码（创建时为空）',
    to_stage       VARCHAR(32)     NOT NULL COMMENT '新阶段编码',
    operator_id    BIGINT UNSIGNED NULL COMMENT '操作人',
    change_time    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    remark         VARCHAR(255)    NULL,
    PRIMARY KEY (id),
    KEY idx_opportunity_id (opportunity_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT ='商机阶段变更日志';

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

-- 商机销售阶段默认配置（6 条：4 个进行中阶段 + 赢单/丢单终态；幂等）
INSERT INTO crm_opportunity_stage (stage_code, stage_name, sort_no, default_win_rate, is_won, is_lost, enabled)
VALUES
    ('CONTACT',    '初步接洽', 10, 10,  0, 0, 1),
    ('ANALYSIS',   '需求分析', 20, 30,  0, 0, 1),
    ('PROPOSAL',   '方案提报', 30, 60,  0, 0, 1),
    ('NEGOTIATION','商务谈判', 40, 80,  0, 0, 1),
    ('WON',        '赢单',     90, 100, 1, 0, 1),
    ('LOST',       '丢单',     99, 0,   0, 1, 1)
ON DUPLICATE KEY UPDATE stage_name = VALUES(stage_name), enabled = VALUES(enabled);

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
    (9, 3, 'SystemOperLog',    '操作日志', 'menu',    'system:operlog:list','/system/oper-log',   'system/oper-log/index',   'lucide:scroll-text',     60, 1),
    (10, 0, 'LeadManage',      '线索管理', 'catalog', NULL,                '/lead',               NULL,                      'lucide:contact',         50, 1),
    (11, 10, 'LeadList',       '线索列表', 'menu',    'crm:lead:list',     '/lead/list',          'lead/list',               'lucide:user-plus',       10, 1),
    (12, 0, 'CustomerManage',  '客户管理', 'catalog', NULL,                '/customer',           NULL,                      'lucide:building-2',      40, 1),
    (13, 12, 'CustomerList',   '客户列表', 'menu',    'crm:customer:list', '/customer/list',      'customer/list',           'lucide:building',        10, 1),
    (14, 12, 'ContactList',    '联系人列表', 'menu',  'crm:contact:list',  '/customer/contact',   'contact/list',            'lucide:users',           20, 1),
    (15, 0, 'OppManage',       '商机管理', 'catalog', NULL,                '/opportunity',        NULL,                      'lucide:target',          45, 1),
    (16, 15, 'OppList',        '商机列表', 'menu',    'crm:opportunity:list','/opportunity/list', 'opportunity/list',        'lucide:kanban',          10, 1),
    (17, 0, 'TaskManage',      '待办任务', 'catalog', NULL,                '/task',               NULL,                      'lucide:check-square',    70, 1),
    (18, 17, 'TaskList',       '任务列表', 'menu',    'crm:task:list',     '/task/list',          'task/list',               'lucide:list-checks',     10, 1)
ON DUPLICATE KEY UPDATE title = VALUES(title), status = VALUES(status);
