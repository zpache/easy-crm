-- =====================================================================
-- V20261116_01  数据字典表（P2-03）
--  - sys_dict      字典类型（dict_type 唯一，built_in=1 表示内置字典）
--  - sys_dict_item 字典项（dict_type + value 业务唯一，unique 由 Service 层校验，
--                   避免 DB 唯一键与逻辑删除冲突）
--  - 预置 9 个内置字典（来源/丢单原因/客户行业/规模/区域/级别/状态/跟进类型/业务类型）
--  - 内置字典以 Java 常量类为 fallback（DictDefaults），字典表删除全部字典项后
--    读接口回退到常量取值，行为与迁移前一致
-- 幂等：可重复执行；重复执行时种子按 dict_type / value 覆盖更新
-- =====================================================================

CREATE TABLE IF NOT EXISTS sys_dict (
    id         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    dict_type  VARCHAR(64)     NOT NULL COMMENT '字典类型编码（业务唯一）',
    name       VARCHAR(64)     NOT NULL COMMENT '字典名称',
    remark     VARCHAR(255)    NULL,
    built_in   TINYINT         NOT NULL DEFAULT 0 COMMENT '1 内置字典（不允许删除，允许停用）',
    status     TINYINT         NOT NULL DEFAULT 1 COMMENT '1 启用，0 停用',
    create_time DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    create_by  BIGINT          NULL,
    update_by  BIGINT          NULL,
    version    INT             NOT NULL DEFAULT 0,
    deleted    TINYINT         NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_dict_type (dict_type)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT ='数据字典类型';

CREATE TABLE IF NOT EXISTS sys_dict_item (
    id         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    dict_id    BIGINT UNSIGNED NULL COMMENT '所属字典 ID（冗余列，归属以 dict_type 为准）',
    dict_type  VARCHAR(64)     NOT NULL COMMENT '字典类型编码（冗余，免联查）',
    label      VARCHAR(64)     NOT NULL COMMENT '显示名称（Excel 导入按此匹配）',
    value      VARCHAR(64)     NOT NULL COMMENT '存储值（数字码或英文码）',
    sort_no    INT             NOT NULL DEFAULT 0 COMMENT '排序，越小越前',
    status     TINYINT         NOT NULL DEFAULT 1 COMMENT '1 启用，0 停用',
    remark     VARCHAR(255)    NULL,
    create_time DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    create_by  BIGINT          NULL,
    update_by  BIGINT          NULL,
    version    INT             NOT NULL DEFAULT 0,
    deleted    TINYINT         NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_dict_type_value (dict_type, value),
    KEY idx_dict_id (dict_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT ='数据字典项';

-- 兼容旧版建表（dict_id 曾为 NOT NULL）：条件改为可空
SET @dict_id_not_null = (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'sys_dict_item'
      AND column_name = 'dict_id' AND is_nullable = 'NO');
SET @alter_sql = IF(@dict_id_not_null > 0,
    'ALTER TABLE sys_dict_item MODIFY dict_id BIGINT UNSIGNED NULL COMMENT ''所属字典 ID（冗余列，归属以 dict_type 为准）''',
    'SELECT 1');
PREPARE dict_stmt FROM @alter_sql;
EXECUTE dict_stmt;
DEALLOCATE PREPARE dict_stmt;

-- ---------------------------------------------------------------- 内置字典种子（显式主键，保证幂等）
-- 先清理历史重复（无唯一键时代的重复种子行，物理删除仅针对种子）
DELETE d FROM sys_dict d
JOIN (SELECT dict_type, MIN(id) AS keep_id FROM sys_dict GROUP BY dict_type HAVING COUNT(*) > 1) t
  ON d.dict_type = t.dict_type AND d.id <> t.keep_id;

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
    -- 线索来源（值 = 数字码，与 LeadSource 常量一致）
    (1,  1, 'lead_source', '官网',       '1',        10, 1),
    (2,  1, 'lead_source', '电话',       '2',        20, 1),
    (3,  1, 'lead_source', '广告投放',   '3',        30, 1),
    (4,  1, 'lead_source', '客户转介绍', '4',        40, 1),
    (5,  1, 'lead_source', '展会活动',   '5',        50, 1),
    (6,  1, 'lead_source', '其他',       '6',        60, 1),
    -- 丢单原因（值 = 英文码，与 LostReasons 常量一致）
    (7,  2, 'lost_reason', '价格因素',       'PRICE',      10, 1),
    (8,  2, 'lost_reason', '功能不满足',     'FEATURE',    20, 1),
    (9,  2, 'lost_reason', '选择了竞争对手', 'COMPETITOR', 30, 1),
    (10, 2, 'lost_reason', '预算取消',       'BUDGET',     40, 1),
    (11, 2, 'lost_reason', '失去联系',       'NO_CONTACT', 50, 1),
    (12, 2, 'lost_reason', '其他',           'OTHER',      60, 1),
    -- 客户行业
    (13, 3, 'customer_industry', '制造业',   '1', 10, 1),
    (14, 3, 'customer_industry', '互联网',   '2', 20, 1),
    (15, 3, 'customer_industry', '金融',     '3', 30, 1),
    (16, 3, 'customer_industry', '零售消费', '4', 40, 1),
    (17, 3, 'customer_industry', '医疗健康', '5', 50, 1),
    (18, 3, 'customer_industry', '教育',     '6', 60, 1),
    (19, 3, 'customer_industry', '其他',     '9', 70, 1),
    -- 客户规模
    (20, 4, 'customer_scale', '1-50 人',     '1', 10, 1),
    (21, 4, 'customer_scale', '51-200 人',   '2', 20, 1),
    (22, 4, 'customer_scale', '201-1000 人', '3', 30, 1),
    (23, 4, 'customer_scale', '1000 人以上', '4', 40, 1),
    -- 客户区域
    (24, 5, 'customer_region', '华北', '1', 10, 1),
    (25, 5, 'customer_region', '华东', '2', 20, 1),
    (26, 5, 'customer_region', '华南', '3', 30, 1),
    (27, 5, 'customer_region', '西南', '4', 40, 1),
    (28, 5, 'customer_region', '海外', '5', 50, 1),
    -- 客户级别
    (29, 6, 'customer_level', 'A（战略）', '1', 10, 1),
    (30, 6, 'customer_level', 'B（重点）', '2', 20, 1),
    (31, 6, 'customer_level', 'C（普通）', '3', 30, 1),
    -- 客户状态
    (32, 7, 'customer_status', '正常',     '1', 10, 1),
    (33, 7, 'customer_status', '公海',     '2', 20, 1),
    (34, 7, 'customer_status', '已合并',   '3', 30, 1),
    (35, 7, 'customer_status', '成交客户', '4', 40, 1),
    -- 跟进类型
    (36, 8, 'follow_type', '电话', 'PHONE',  10, 1),
    (37, 8, 'follow_type', '拜访', 'VISIT',  20, 1),
    (38, 8, 'follow_type', '邮件', 'EMAIL',  30, 1),
    (39, 8, 'follow_type', '微信', 'WECHAT', 40, 1),
    (40, 8, 'follow_type', '其他', 'OTHER',  50, 1),
    -- 关联业务类型
    (41, 9, 'follow_biz_type', '线索', 'lead',        10, 1),
    (42, 9, 'follow_biz_type', '客户', 'customer',    20, 1),
    (43, 9, 'follow_biz_type', '商机', 'opportunity', 30, 1),
    (44, 9, 'follow_biz_type', '订单', 'order',       40, 1)
ON DUPLICATE KEY UPDATE dict_id = VALUES(dict_id), label = VALUES(label), status = VALUES(status), sort_no = VALUES(sort_no);

-- ---------------------------------------------------------------- 字典管理菜单（挂系统管理下）
INSERT INTO sys_permission (id, pid, name, title, type, auth_code, path, component, icon, order_no, status)
VALUES (130, 3, 'DictList', '数据字典', 'menu', 'system:dict:list', '/system/dict', 'system/dict/list', 'lucide:book-text', 70, 1)
ON DUPLICATE KEY UPDATE title = VALUES(title), status = VALUES(status);

-- ---------------------------------------------------------------- 迁移台账
CREATE TABLE IF NOT EXISTS sys_schema_migration (
    version     VARCHAR(64) PRIMARY KEY,
    description VARCHAR(255),
    applied_at  DATETIME DEFAULT CURRENT_TIMESTAMP
);
INSERT INTO sys_schema_migration (version, description)
VALUES ('V20261116_01', '数据字典：sys_dict / sys_dict_item 建表与内置字典种子')
ON DUPLICATE KEY UPDATE description = VALUES(description);
