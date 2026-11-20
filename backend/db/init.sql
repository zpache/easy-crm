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
    token_version BIGINT          NOT NULL DEFAULT 0 COMMENT '令牌版本：改密/登出/停用时递增，旧 JWT 立即失效',
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
    KEY idx_status (status),
    KEY idx_converted_customer (converted_customer_id)
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
    follow_record_id BIGINT UNSIGNED NULL COMMENT '生成来源跟进记录 ID（source=FOLLOW 时有效，删除记录级联取消）',
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

-- ---------------------------------------------------------------- 数据字典（P2-03：sys_dict + sys_dict_item）
-- dict_type 业务唯一由 Service 层校验（避免 DB 唯一键与逻辑删除冲突）；
-- 内置字典以 Java 常量类为 fallback（DictDefaults）
CREATE TABLE IF NOT EXISTS sys_dict (
    id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    dict_type   VARCHAR(64)     NOT NULL COMMENT '字典类型编码（业务唯一）',
    name        VARCHAR(64)     NOT NULL COMMENT '字典名称',
    remark      VARCHAR(255)    NULL,
    built_in    TINYINT         NOT NULL DEFAULT 0 COMMENT '1 内置字典（不允许删除，允许停用）',
    status      TINYINT         NOT NULL DEFAULT 1 COMMENT '1 启用，0 停用',
    create_time DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    create_by   BIGINT          NULL,
    update_by   BIGINT          NULL,
    version     INT             NOT NULL DEFAULT 0,
    deleted     TINYINT         NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_dict_type (dict_type)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT ='数据字典类型';

CREATE TABLE IF NOT EXISTS sys_dict_item (
    id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    dict_id     BIGINT UNSIGNED NULL COMMENT '所属字典 ID（冗余列，归属以 dict_type 为准）',
    dict_type   VARCHAR(64)     NOT NULL COMMENT '字典类型编码（冗余，免联查）',
    label       VARCHAR(64)     NOT NULL COMMENT '显示名称（Excel 导入按此匹配）',
    value       VARCHAR(64)     NOT NULL COMMENT '存储值（数字码或英文码）',
    sort_no     INT             NOT NULL DEFAULT 0 COMMENT '排序，越小越前',
    status      TINYINT         NOT NULL DEFAULT 1 COMMENT '1 启用，0 停用',
    remark      VARCHAR(255)    NULL,
    create_time DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    create_by   BIGINT          NULL,
    update_by   BIGINT          NULL,
    version     INT             NOT NULL DEFAULT 0,
    deleted     TINYINT         NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_dict_type_value (dict_type, value),
    KEY idx_dict_id (dict_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT ='数据字典项';

-- =====================================================================
-- 初始数据：admin（初始口令仅用于引导，首次启动时自动更换）
-- 下面这行 BCrypt 是「未加固」标记值：应用启动检测到它即生成随机口令并打印一次，
-- 也可通过环境变量 ADMIN_INITIAL_PASSWORD 预设。任何环境都不会保留公开的默认口令。
-- 详见 backend/db/migration/README.md 与 security/AdminBootstrapRunner.java
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
    (18, 17, 'TaskList',       '任务列表', 'menu',    'crm:task:list',     '/task/list',          'task/list',               'lucide:list-checks',     10, 1),
    -- 跟进记录 / 商机看板 / 消息提醒
    (20, 0,  'FollowManage',   '跟进管理', 'catalog', NULL,                  '/follow',             NULL,                       'lucide:history',         48,  1),
    (21, 20, 'FollowList',     '跟进记录', 'menu',    'crm:follow:list',    '/follow/list',        'follow/list',              'lucide:message-square',  10,  1),
    (22, 15, 'OpportunityBoard','商机看板','menu',    'crm:opportunity:list','/opportunity/board',  'opportunity/board',        'lucide:kanban',          20,  1),
    (23, 0,  'MessageCenter',  '消息提醒', 'menu',    'crm:reminder:list',  '/message',            'message/index',            'lucide:bell',            80,  1),
    (130, 3, 'DictList',       '数据字典', 'menu',    'system:dict:list',   '/system/dict',        'system/dict/list',         'lucide:book-text',       70,  1),
    -- 客户服务
    (60, 0,  'Support',            '客户服务',   'catalog', NULL,                     '/support',            NULL,                              'lucide:headset',    60,  1),
    (61, 60, 'SupportTicketList',  '工单列表',   'menu',    'crm:ticket:list',       '/support/ticket',     'support/ticket/list',             'lucide:ticket',     10,  1),
    (62, 60, 'SupportSlaPolicy',   'SLA 策略',   'menu',    'crm:sla:list',          '/support/sla',        'support/sla/list',                'lucide:timer',      20,  1),
    (63, 60, 'SupportKnowledge',   '知识库',     'menu',    'crm:knowledge:list',    '/support/knowledge',  'support/knowledge/list',          'lucide:book-open',  30,  1),
    (64, 60, 'SupportSatisfaction','满意度统计', 'menu',    'crm:satisfaction:list', '/support/satisfaction','support/satisfaction/list',      'lucide:star',       40,  1),
    -- 产品 / 交易 / 报表中心
    (100, 0,   'CrmProduct',       '产品管理',   'catalog', NULL,                  '/product',            NULL,                     'lucide:package',     55,  1),
    (101, 100, 'ProductList',      '产品列表',   'menu',    'crm:product:list',    '/product/list',       'product/list',           'lucide:boxes',       10,  1),
    (110, 0,   'CrmTrade',         '交易管理',   'catalog', NULL,                  '/trade',              NULL,                     'lucide:shopping-cart',56, 1),
    (111, 110, 'ContractList',     '合同列表',   'menu',    'crm:contract:list',   '/trade/contract',     'contract/list',          'lucide:file-signature',10, 1),
    (112, 110, 'CrmOrderList',     '订单列表',   'menu',    'crm:order:list',      '/trade/order',        'order/list',             'lucide:clipboard-list',20, 1),
    (113, 110, 'PaymentList',      '回款管理',   'menu',    'crm:payment-plan:list','/trade/payment',     'payment/list',           'lucide:wallet',      30,  1),
    (120, 0,   'CrmReport',        '报表中心',   'catalog', NULL,                  '/report',             NULL,                     'lucide:chart-column',60,  1),
    (121, 120, 'ReportAchievement','销售达成分析','menu',   'crm:report:achievement','/report/analysis',  'report/index',           'lucide:chart-no-axes-combined',10,1)
ON DUPLICATE KEY UPDATE
    pid = VALUES(pid), name = VALUES(name), title = VALUES(title), type = VALUES(type),
    auth_code = VALUES(auth_code), path = VALUES(path), component = VALUES(component),
    icon = VALUES(icon), order_no = VALUES(order_no), status = VALUES(status);

-- ---------------------------------------------------------------- 内置字典种子（与迁移 V20261116_01 保持一致；管理入口：系统管理/数据字典）
INSERT INTO sys_dict (id, dict_type, name, remark, built_in, status)
VALUES
    (1, 'lead_source',      '线索来源',   '线索/Excel 导入来源取值', 1, 1),
    (2, 'lost_reason',      '丢单原因',   '商机丢单归因',           1, 1),
    (3, 'customer_industry','客户行业',   '客户所属行业',           1, 1),
    (4, 'customer_scale',   '客户规模',   '客户人员规模',           1, 1),
    (5, 'customer_region',  '客户区域',   '客户所在区域',           1, 1),
    (6, 'customer_level',   '客户级别',   '客户分级',               1, 1),
    (7, 'customer_status',  '客户状态',   '客户生命周期状态',       1, 1),
    (8, 'follow_type',      '跟进类型',   '跟进记录方式',           1, 1),
    (9, 'follow_biz_type',  '关联业务类型','跟进/待办多态关联取值',  1, 1)
ON DUPLICATE KEY UPDATE name = VALUES(name), built_in = 1;

INSERT INTO sys_dict_item (id, dict_id, dict_type, label, value, sort_no, status)
VALUES
    (1,  1, 'lead_source', '官网', '1', 10, 1),
    (2,  1, 'lead_source', '电话', '2', 20, 1),
    (3,  1, 'lead_source', '广告投放', '3', 30, 1),
    (4,  1, 'lead_source', '客户转介绍', '4', 40, 1),
    (5,  1, 'lead_source', '展会活动', '5', 50, 1),
    (6,  1, 'lead_source', '其他', '6', 60, 1),
    (7,  2, 'lost_reason', '价格因素', 'PRICE', 10, 1),
    (8,  2, 'lost_reason', '功能不满足', 'FEATURE', 20, 1),
    (9,  2, 'lost_reason', '选择了竞争对手', 'COMPETITOR', 30, 1),
    (10, 2, 'lost_reason', '预算取消', 'BUDGET', 40, 1),
    (11, 2, 'lost_reason', '失去联系', 'NO_CONTACT', 50, 1),
    (12, 2, 'lost_reason', '其他', 'OTHER', 60, 1),
    (13, 3, 'customer_industry', '制造业', '1', 10, 1),
    (14, 3, 'customer_industry', '互联网', '2', 20, 1),
    (15, 3, 'customer_industry', '金融', '3', 30, 1),
    (16, 3, 'customer_industry', '零售消费', '4', 40, 1),
    (17, 3, 'customer_industry', '医疗健康', '5', 50, 1),
    (18, 3, 'customer_industry', '教育', '6', 60, 1),
    (19, 3, 'customer_industry', '其他', '9', 70, 1),
    (20, 4, 'customer_scale', '1-50 人', '1', 10, 1),
    (21, 4, 'customer_scale', '51-200 人', '2', 20, 1),
    (22, 4, 'customer_scale', '201-1000 人', '3', 30, 1),
    (23, 4, 'customer_scale', '1000 人以上', '4', 40, 1),
    (24, 5, 'customer_region', '华北', '1', 10, 1),
    (25, 5, 'customer_region', '华东', '2', 20, 1),
    (26, 5, 'customer_region', '华南', '3', 30, 1),
    (27, 5, 'customer_region', '西南', '4', 40, 1),
    (28, 5, 'customer_region', '海外', '5', 50, 1),
    (29, 6, 'customer_level', 'A（战略）', '1', 10, 1),
    (30, 6, 'customer_level', 'B（重点）', '2', 20, 1),
    (31, 6, 'customer_level', 'C（普通）', '3', 30, 1),
    (32, 7, 'customer_status', '正常', '1', 10, 1),
    (33, 7, 'customer_status', '公海', '2', 20, 1),
    (34, 7, 'customer_status', '已合并', '3', 30, 1),
    (35, 7, 'customer_status', '成交客户', '4', 40, 1),
    (36, 8, 'follow_type', '电话', 'PHONE', 10, 1),
    (37, 8, 'follow_type', '拜访', 'VISIT', 20, 1),
    (38, 8, 'follow_type', '邮件', 'EMAIL', 30, 1),
    (39, 8, 'follow_type', '微信', 'WECHAT', 40, 1),
    (40, 8, 'follow_type', '其他', 'OTHER', 50, 1),
    (41, 9, 'follow_biz_type', '线索', 'lead', 10, 1),
    (42, 9, 'follow_biz_type', '客户', 'customer', 20, 1),
    (43, 9, 'follow_biz_type', '商机', 'opportunity', 30, 1),
    (44, 9, 'follow_biz_type', '订单', 'order', 40, 1)
ON DUPLICATE KEY UPDATE dict_id = VALUES(dict_id), label = VALUES(label), status = VALUES(status), sort_no = VALUES(sort_no);

-- =====================================================================
-- 批次 5：营销域 + 审批中心入口（与 migration/V20261117_01 保持一致）
--   营销活动 crm_campaign / 线索活动归因 crm_lead.campaign_id /
--   外部审批镜像 crm_approval_instance / 新增菜单
-- =====================================================================

CREATE TABLE IF NOT EXISTS crm_campaign (
    id           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    code         VARCHAR(64)     NOT NULL COMMENT '活动编码，业务唯一',
    name         VARCHAR(128)    NOT NULL COMMENT '活动名称',
    type         TINYINT         NOT NULL DEFAULT 1 COMMENT '活动类型：1 线上推广 / 2 线下活动 / 3 电话营销 / 4 渠道合作 / 5 内容营销',
    channel      VARCHAR(64)     NULL COMMENT '投放渠道',
    status       TINYINT         NOT NULL DEFAULT 0 COMMENT '0 草稿 / 1 进行中 / 2 已结束 / 3 已暂停',
    budget       DECIMAL(15, 2)  NOT NULL DEFAULT 0.00 COMMENT '活动预算',
    start_date   DATE            NULL,
    end_date     DATE            NULL,
    target_leads INT             NOT NULL DEFAULT 0 COMMENT '目标线索数',
    owner_id     BIGINT          NULL,
    dept_id      BIGINT          NULL,
    remark       VARCHAR(500)    NULL,
    create_time  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    create_by    BIGINT          NULL,
    update_by    BIGINT          NULL,
    version      INT             NOT NULL DEFAULT 0,
    deleted      TINYINT         NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_code (code),
    KEY idx_status (status),
    KEY idx_owner (owner_id),
    KEY idx_date (start_date, end_date)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT ='营销活动';

SET @campaign_col = (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'crm_lead' AND column_name = 'campaign_id');
SET @alter_sql = IF(@campaign_col = 0,
    'ALTER TABLE crm_lead ADD COLUMN campaign_id BIGINT UNSIGNED NULL COMMENT ''来源营销活动 ID'' AFTER source',
    'SELECT 1');
PREPARE campaign_stmt FROM @alter_sql;
EXECUTE campaign_stmt;
DEALLOCATE PREPARE campaign_stmt;

SET @campaign_idx = (SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE() AND table_name = 'crm_lead' AND index_name = 'idx_campaign');
SET @alter_sql = IF(@campaign_idx = 0,
    'ALTER TABLE crm_lead ADD KEY idx_campaign (campaign_id)',
    'SELECT 1');
PREPARE campaign_idx_stmt FROM @alter_sql;
EXECUTE campaign_idx_stmt;
DEALLOCATE PREPARE campaign_idx_stmt;

CREATE TABLE IF NOT EXISTS crm_approval_instance (
    id                    BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    biz_type              VARCHAR(32)     NOT NULL COMMENT 'QUOTE / CONTRACT / ORDER / INVOICE / REFUND / OTHER',
    biz_id                BIGINT          NULL,
    biz_no                VARCHAR(64)     NULL,
    title                 VARCHAR(128)    NOT NULL COMMENT '审批标题',
    apply_user_id         BIGINT          NULL,
    apply_user_name       VARCHAR(64)     NULL,
    current_approver_id   BIGINT          NULL,
    current_approver_name VARCHAR(64)     NULL,
    node_name             VARCHAR(64)     NULL,
    status                TINYINT         NOT NULL DEFAULT 0 COMMENT '0 审批中 / 1 已通过 / 2 已驳回 / 3 已撤销',
    apply_time            DATETIME        NULL,
    finish_time           DATETIME        NULL,
    source_system         VARCHAR(64)     NULL,
    external_url          VARCHAR(500)    NULL,
    remark                VARCHAR(500)    NULL,
    create_time           DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time           DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    create_by             BIGINT          NULL,
    update_by             BIGINT          NULL,
    version               INT             NOT NULL DEFAULT 0,
    deleted               TINYINT         NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_approver_status (current_approver_id, status),
    KEY idx_apply_user (apply_user_id),
    KEY idx_biz (biz_type, biz_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT ='审批实例镜像（外部审批系统同步，只读）';

INSERT INTO crm_approval_instance
    (id, biz_type, biz_id, biz_no, title, apply_user_id, apply_user_name,
     current_approver_id, current_approver_name, node_name, status,
     apply_time, finish_time, source_system, external_url, remark)
VALUES
    (1, 'QUOTE', NULL, 'QT-示例-001', '报价单 8 折审批（示例）', 10, '销售小王',
     1, '系统管理员', '销售总监审批', 0,
     NOW(), NULL, '企业微信审批', 'https://example.com/approval/1', '示例数据：外部审批镜像'),
    (2, 'CONTRACT', NULL, 'HT-示例-002', '合同用印审批（示例）', 10, '销售小王',
     1, '系统管理员', '法务审核', 1,
     DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY), '企业微信审批',
     'https://example.com/approval/2', '示例数据：外部审批镜像'),
    (3, 'REFUND', NULL, 'RF-示例-003', '客户退款审批（示例）', 1, '系统管理员',
     1, '系统管理员', '财务复核', 0,
     DATE_SUB(NOW(), INTERVAL 1 DAY), NULL, '钉钉 OA', 'https://example.com/approval/3', '示例数据：外部审批镜像')
ON DUPLICATE KEY UPDATE title = VALUES(title), status = VALUES(status), node_name = VALUES(node_name);

INSERT INTO sys_permission (id, pid, name, title, type, auth_code, path, component, icon, order_no, status)
VALUES
    (102, 100, 'PriceListManage',  '价目表',   'menu', 'crm:price:list',      '/product/price',  'product/price/list',   'lucide:tags',        20, 1),
    (103, 100, 'QuoteList',        '报价单',   'menu', 'crm:quote:list',      '/product/quote',  'product/quote/list',   'lucide:file-text',   30, 1),
    (114, 110, 'InvoiceList',      '开票管理', 'menu', 'crm:invoice:list',    '/trade/invoice',  'payment/invoice/list', 'lucide:receipt-text', 40, 1),
    (115, 110, 'RefundList',       '退款管理', 'menu', 'crm:refund:list',     '/trade/refund',   'payment/refund/list',  'lucide:rotate-ccw',  50, 1),
    (140, 0,   'CrmMarketing',     '营销管理', 'catalog', NULL,               '/marketing',      NULL,                   'lucide:megaphone',   58, 1),
    (141, 140, 'CampaignList',     '营销活动', 'menu', 'crm:campaign:list',   '/marketing/campaign', 'marketing/campaign/list', 'lucide:target', 10, 1),
    (150, 0,   'ApprovalCenter',   '审批中心', 'menu', 'crm:approval:list',   '/approval',       'approval/index',       'lucide:workflow',    85, 1)
ON DUPLICATE KEY UPDATE
    pid = VALUES(pid), title = VALUES(title), type = VALUES(type), auth_code = VALUES(auth_code),
    path = VALUES(path), component = VALUES(component), icon = VALUES(icon),
    order_no = VALUES(order_no), status = VALUES(status);
