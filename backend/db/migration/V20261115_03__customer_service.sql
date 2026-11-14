-- =====================================================================
-- easy-crm 客户服务域（工单 / 流转日志 / SLA 策略 / 知识库 / 满意度）
-- 版本：V20261115_03   MySQL 8.4 / utf8mb4 / InnoDB
--
-- 幂等约定：
--   1. 建表统一 CREATE TABLE IF NOT EXISTS；
--   2. 「补列」（脚本被中断后重跑、或旧库缺列）统一走 information_schema
--      判定 + PREPARE/EXECUTE 动态 DDL，避免重复加列报错；
--   3. 种子数据统一 INSERT ... ON DUPLICATE KEY UPDATE；
--   4. 脚本开头建 sys_schema_migration，末尾登记本版本执行记录。
-- 所有新表均携带 BaseEntity 审计列：id / create_time / update_time /
-- create_by / update_by / version / deleted。
-- =====================================================================

SET NAMES utf8mb4;

-- ---------------------------------------------------------------- 迁移台账
CREATE TABLE IF NOT EXISTS sys_schema_migration (
    version     VARCHAR(64)  PRIMARY KEY,
    description VARCHAR(255) NULL,
    applied_at  DATETIME     DEFAULT CURRENT_TIMESTAMP
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '数据库迁移执行记录';

-- =====================================================================
-- 1. 服务工单 crm_service_ticket
--    状态机：PENDING 待受理 → ASSIGNED 已派单 → PROCESSING 处理中 →
--            WAITING_CONFIRM 待客户确认 → COMPLETED 已完成 / CLOSED 已关闭
--    SLA 时间由 crm_sla_policy(ticket_type + priority) 在创建时计算。
-- =====================================================================
CREATE TABLE IF NOT EXISTS crm_service_ticket (
    id                       BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    ticket_no                VARCHAR(32)     NOT NULL COMMENT '工单编号，业务唯一',
    title                    VARCHAR(128)    NOT NULL COMMENT '工单标题',
    customer_id              BIGINT UNSIGNED NOT NULL COMMENT '客户 ID',
    contact_id               BIGINT UNSIGNED NULL COMMENT '联系人 ID（可空）',
    order_id                 BIGINT UNSIGNED NULL COMMENT '订单 ID（可空）',
    ticket_type              VARCHAR(32)     NOT NULL COMMENT '工单类型字典值（TicketDicts.TYPE）',
    priority                 VARCHAR(16)     NOT NULL COMMENT '优先级字典值（TicketDicts.PRIORITY）',
    status                   VARCHAR(24)     NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING/ASSIGNED/PROCESSING/WAITING_CONFIRM/COMPLETED/CLOSED',
    owner_id                 BIGINT UNSIGNED NULL COMMENT '受理人（sys_user.id）',
    dept_id                  BIGINT UNSIGNED NULL COMMENT '归属部门（sys_dept.id）',
    sla_due_response_time    DATETIME        NULL COMMENT 'SLA 应响应时间（创建时按策略计算）',
    sla_due_resolve_time     DATETIME        NULL COMMENT 'SLA 应解决时间（创建时按策略计算）',
    actual_response_time     DATETIME        NULL COMMENT '实际首次响应时间',
    actual_resolve_time      DATETIME        NULL COMMENT '实际解决时间',
    overdue_flag             TINYINT         NOT NULL DEFAULT 0 COMMENT '超时标记：0 正常 / 1 已超时（ServiceSlaTask 扫描写入）',
    source                   VARCHAR(32)     NULL COMMENT '来源字典值（TicketDicts.SOURCE）',
    description              VARCHAR(2000)   NULL COMMENT '问题描述',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    create_by   BIGINT       NULL,
    update_by   BIGINT       NULL,
    version     INT          NOT NULL DEFAULT 0,
    deleted     TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_ticket_no (ticket_no),
    KEY idx_customer_id (customer_id),
    KEY idx_owner_status (owner_id, status),
    KEY idx_status (status),
    KEY idx_sla_due_response (sla_due_response_time),
    KEY idx_sla_due_resolve (sla_due_resolve_time),
    KEY idx_overdue (overdue_flag, status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT ='服务工单';

-- =====================================================================
-- 2. 工单流转日志 crm_service_ticket_log
--    每次状态变更（含派单/关闭/改派）与工单变更同事务写入。
-- =====================================================================
CREATE TABLE IF NOT EXISTS crm_service_ticket_log (
    id           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    ticket_id    BIGINT UNSIGNED NOT NULL COMMENT '工单 ID',
    from_status  VARCHAR(24)     NULL COMMENT '原状态（创建时为空）',
    to_status    VARCHAR(24)     NOT NULL COMMENT '新状态',
    operator_id  BIGINT UNSIGNED NULL COMMENT '操作人（sys_user.id）',
    operate_time DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    remark       VARCHAR(500)    NULL COMMENT '备注',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    create_by   BIGINT       NULL,
    update_by   BIGINT       NULL,
    version     INT          NOT NULL DEFAULT 0,
    deleted     TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_ticket_id (ticket_id),
    KEY idx_operate_time (operate_time)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT ='工单流转日志';

-- =====================================================================
-- 3. SLA 策略 crm_sla_policy
--    工单类型 + 优先级 → 响应小时数 / 解决小时数；enabled 控制启停。
-- =====================================================================
CREATE TABLE IF NOT EXISTS crm_sla_policy (
    id             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    ticket_type    VARCHAR(32)     NOT NULL COMMENT '工单类型字典值',
    priority       VARCHAR(16)     NOT NULL COMMENT '优先级字典值',
    response_hours INT             NOT NULL COMMENT '响应时限（小时）',
    resolve_hours  INT             NOT NULL COMMENT '解决时限（小时）',
    enabled        TINYINT         NOT NULL DEFAULT 1 COMMENT '1 启用 / 0 停用',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    create_by   BIGINT       NULL,
    update_by   BIGINT       NULL,
    version     INT          NOT NULL DEFAULT 0,
    deleted     TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_type_priority (ticket_type, priority)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT ='SLA 策略';

-- =====================================================================
-- 4. 知识库 crm_knowledge
-- =====================================================================
CREATE TABLE IF NOT EXISTS crm_knowledge (
    id             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    category       VARCHAR(64)     NOT NULL COMMENT '分类',
    title          VARCHAR(128)    NOT NULL COMMENT '标题',
    content        TEXT            NULL COMMENT '内容',
    tags           VARCHAR(255)    NULL COMMENT '标签（逗号分隔）',
    publish_status VARCHAR(16)     NOT NULL DEFAULT 'DRAFT' COMMENT '发布状态：DRAFT/PUBLISHED/OFFLINE',
    view_count     BIGINT          NOT NULL DEFAULT 0 COMMENT '浏览量',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    create_by   BIGINT       NULL,
    update_by   BIGINT       NULL,
    version     INT          NOT NULL DEFAULT 0,
    deleted     TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_category (category),
    KEY idx_publish_status (publish_status),
    KEY idx_title (title)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT ='知识库';

-- =====================================================================
-- 5. 满意度评价 crm_satisfaction
--    一单一评：uk_ticket_id 唯一约束 + Service 层事务内二次校验。
--    owner_id / dept_id 冗余自工单，用于行级数据权限。
-- =====================================================================
CREATE TABLE IF NOT EXISTS crm_satisfaction (
    id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    ticket_id     BIGINT UNSIGNED NOT NULL COMMENT '工单 ID',
    score         TINYINT         NOT NULL COMMENT '评分 1-5',
    content       VARCHAR(1000)   NULL COMMENT '评价内容',
    evaluate_time DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '评价时间',
    owner_id      BIGINT UNSIGNED NULL COMMENT '冗余工单受理人，供行级数据权限',
    dept_id       BIGINT UNSIGNED NULL COMMENT '冗余工单归属部门，供行级数据权限',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    create_by   BIGINT       NULL,
    update_by   BIGINT       NULL,
    version     INT          NOT NULL DEFAULT 0,
    deleted     TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_ticket_id (ticket_id),
    KEY idx_owner_id (owner_id),
    KEY idx_dept_id (dept_id),
    KEY idx_evaluate_time (evaluate_time)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT ='满意度评价';

-- =====================================================================
-- 幂等补列：information_schema + PREPARE 动态 DDL
-- 用途：脚本曾在建表后中断、或旧库已有同名表但缺列时，重跑可安全补齐。
-- 首次执行时列已存在于 CREATE TABLE 中，下面各块均为 no-op（SELECT 1）。
-- =====================================================================
SET @db_name = DATABASE();

-- crm_service_ticket.order_id
SET @ddl = (
    SELECT IF(COUNT(*) = 0,
        'ALTER TABLE `crm_service_ticket` ADD COLUMN `order_id` BIGINT UNSIGNED NULL COMMENT ''订单 ID（可空）'' AFTER `contact_id`',
        'SELECT 1')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'crm_service_ticket' AND COLUMN_NAME = 'order_id'
);
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- crm_service_ticket.overdue_flag
SET @ddl = (
    SELECT IF(COUNT(*) = 0,
        'ALTER TABLE `crm_service_ticket` ADD COLUMN `overdue_flag` TINYINT NOT NULL DEFAULT 0 COMMENT ''超时标记'' AFTER `actual_resolve_time`',
        'SELECT 1')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'crm_service_ticket' AND COLUMN_NAME = 'overdue_flag'
);
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- crm_service_ticket.dept_id
SET @ddl = (
    SELECT IF(COUNT(*) = 0,
        'ALTER TABLE `crm_service_ticket` ADD COLUMN `dept_id` BIGINT UNSIGNED NULL COMMENT ''归属部门'' AFTER `owner_id`',
        'SELECT 1')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'crm_service_ticket' AND COLUMN_NAME = 'dept_id'
);
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- crm_sla_policy.enabled
SET @ddl = (
    SELECT IF(COUNT(*) = 0,
        'ALTER TABLE `crm_sla_policy` ADD COLUMN `enabled` TINYINT NOT NULL DEFAULT 1 COMMENT ''启用'' AFTER `resolve_hours`',
        'SELECT 1')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'crm_sla_policy' AND COLUMN_NAME = 'enabled'
);
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- crm_knowledge.view_count
SET @ddl = (
    SELECT IF(COUNT(*) = 0,
        'ALTER TABLE `crm_knowledge` ADD COLUMN `view_count` BIGINT NOT NULL DEFAULT 0 COMMENT ''浏览量'' AFTER `publish_status`',
        'SELECT 1')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'crm_knowledge' AND COLUMN_NAME = 'view_count'
);
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- crm_satisfaction.dept_id
SET @ddl = (
    SELECT IF(COUNT(*) = 0,
        'ALTER TABLE `crm_satisfaction` ADD COLUMN `dept_id` BIGINT UNSIGNED NULL COMMENT ''归属部门'' AFTER `owner_id`',
        'SELECT 1')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'crm_satisfaction' AND COLUMN_NAME = 'dept_id'
);
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 幂等补索引（同样基于 information_schema + PREPARE，避免重复建索引报错）
SET @ddl = (
    SELECT IF(COUNT(*) = 0,
        'ALTER TABLE `crm_service_ticket` ADD INDEX `idx_overdue` (`overdue_flag`, `status`)',
        'SELECT 1')
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'crm_service_ticket' AND INDEX_NAME = 'idx_overdue'
);
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- =====================================================================
-- 种子数据：SLA 策略（工单类型 × 优先级），幂等 ON DUPLICATE KEY UPDATE
-- 说明：命中唯一键 (ticket_type, priority) 时更新时限并「复活」逻辑删除行，
--       避免已删行占用唯一键导致种子看似成功却不可用。
-- =====================================================================
INSERT INTO crm_sla_policy (ticket_type, priority, response_hours, resolve_hours, enabled)
VALUES
    ('CONSULT',   'LOW',    24, 72, 1),
    ('CONSULT',   'MEDIUM', 12, 48, 1),
    ('CONSULT',   'HIGH',    4, 24, 1),
    ('CONSULT',   'URGENT',  1,  8, 1),
    ('COMPLAINT', 'LOW',    12, 48, 1),
    ('COMPLAINT', 'MEDIUM',  6, 24, 1),
    ('COMPLAINT', 'HIGH',    2, 12, 1),
    ('COMPLAINT', 'URGENT',  1,  4, 1),
    ('REPAIR',    'LOW',    24, 72, 1),
    ('REPAIR',    'MEDIUM', 12, 48, 1),
    ('REPAIR',    'HIGH',    4, 24, 1),
    ('REPAIR',    'URGENT',  2,  8, 1),
    ('RETURN',    'LOW',    24, 72, 1),
    ('RETURN',    'MEDIUM', 12, 48, 1),
    ('RETURN',    'HIGH',    4, 24, 1),
    ('RETURN',    'URGENT',  2,  8, 1),
    ('OTHER',     'LOW',    24, 72, 1),
    ('OTHER',     'MEDIUM', 12, 48, 1),
    ('OTHER',     'HIGH',    4, 24, 1),
    ('OTHER',     'URGENT',  2,  8, 1)
ON DUPLICATE KEY UPDATE
    response_hours = VALUES(response_hours),
    resolve_hours  = VALUES(resolve_hours),
    enabled        = VALUES(enabled),
    deleted        = 0;

-- =====================================================================
-- 迁移台账登记（幂等）
-- =====================================================================
INSERT INTO sys_schema_migration (version, description)
VALUES ('V20261115_03', '客户服务域：工单/流转日志/SLA 策略/知识库/满意度')
ON DUPLICATE KEY UPDATE applied_at = CURRENT_TIMESTAMP;
