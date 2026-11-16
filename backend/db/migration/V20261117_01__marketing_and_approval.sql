-- =====================================================================
-- V20261117_01  营销域 + 审批中心入口 + 菜单登记
--
-- 内容：
--   1. crm_campaign            营销活动（活动、预算、周期、目标线索数）
--   2. crm_lead.campaign_id    线索归属活动（活动效果 / ROI 的归因口径）
--   3. crm_approval_instance   外部审批系统镜像（只读：待办 / 已办 / 已发起）
--   4. sys_permission 菜单：价目表 / 报价单 / 开票 / 退款 / 营销管理 / 营销活动 / 审批中心
--   5. 审批镜像示例数据（仅 3 条，admin 名下；接入外部系统后由同步任务覆盖）
--
-- 幂等约定（与其它迁移脚本一致）：
--   * 建表 CREATE TABLE IF NOT EXISTS；
--   * 补列走 information_schema + PREPARE 条件 ALTER；
--   * 种子 INSERT ... ON DUPLICATE KEY UPDATE（显式主键）；
--   * 末尾登记 sys_schema_migration。
--
-- 说明：审批流本身对接外部审批系统，本库只保存审批实例的只读镜像，
--       通过 / 驳回 / 撤销等动作在外部系统完成，CRM 侧仅提供入口与跳转。
-- =====================================================================

SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS sys_schema_migration (
    version     VARCHAR(64)  PRIMARY KEY,
    description VARCHAR(255) NULL,
    applied_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT ='数据库脚本版本登记';

-- =====================================================================
-- 一、营销活动
-- =====================================================================
CREATE TABLE IF NOT EXISTS crm_campaign (
    id           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    code         VARCHAR(64)     NOT NULL COMMENT '活动编码，业务唯一',
    name         VARCHAR(128)    NOT NULL COMMENT '活动名称',
    type         TINYINT         NOT NULL DEFAULT 1 COMMENT '活动类型：1 线上推广 / 2 线下活动 / 3 电话营销 / 4 渠道合作 / 5 内容营销',
    channel      VARCHAR(64)     NULL COMMENT '投放渠道（搜索引擎/信息流/展会/公众号…）',
    status       TINYINT         NOT NULL DEFAULT 0 COMMENT '0 草稿 / 1 进行中 / 2 已结束 / 3 已暂停',
    budget       DECIMAL(15, 2)  NOT NULL DEFAULT 0.00 COMMENT '活动预算',
    start_date   DATE            NULL COMMENT '开始日期',
    end_date     DATE            NULL COMMENT '结束日期',
    target_leads INT             NOT NULL DEFAULT 0 COMMENT '目标线索数',
    owner_id     BIGINT          NULL COMMENT '负责人（sys_user.id）',
    dept_id      BIGINT          NULL COMMENT '归属部门（sys_dept.id）',
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

-- 线索归属活动（可空：自然流量线索不挂活动）
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

-- =====================================================================
-- 二、审批实例镜像（只读）
-- =====================================================================
CREATE TABLE IF NOT EXISTS crm_approval_instance (
    id                    BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    biz_type              VARCHAR(32)     NOT NULL COMMENT '业务类型：QUOTE / CONTRACT / ORDER / INVOICE / REFUND / OTHER',
    biz_id                BIGINT          NULL COMMENT '业务单据 ID',
    biz_no                VARCHAR(64)     NULL COMMENT '业务单号（展示用）',
    title                 VARCHAR(128)    NOT NULL COMMENT '审批标题',
    apply_user_id         BIGINT          NULL COMMENT '发起人（sys_user.id）',
    apply_user_name       VARCHAR(64)     NULL COMMENT '发起人姓名（镜像外部系统）',
    current_approver_id   BIGINT          NULL COMMENT '当前审批人（sys_user.id，外部系统账号需先映射）',
    current_approver_name VARCHAR(64)     NULL COMMENT '当前审批人姓名',
    node_name             VARCHAR(64)     NULL COMMENT '当前节点名称',
    status                TINYINT         NOT NULL DEFAULT 0 COMMENT '0 审批中 / 1 已通过 / 2 已驳回 / 3 已撤销',
    apply_time            DATETIME        NULL COMMENT '发起时间',
    finish_time           DATETIME        NULL COMMENT '结束时间',
    source_system         VARCHAR(64)     NULL COMMENT '来源审批系统（如 企业微信审批 / 钉钉 OA）',
    external_url          VARCHAR(500)    NULL COMMENT '外部系统详情地址（点击跳转）',
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

-- 示例数据：便于登录后立即看到三个列表入口的效果；接入外部系统后由同步任务覆盖
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

-- =====================================================================
-- 三、菜单登记
-- =====================================================================
INSERT INTO sys_permission (id, pid, name, title, type, auth_code, path, component, icon, order_no, status)
VALUES
    -- 产品管理：价目表 / 报价单
    (102, 100, 'PriceListManage',  '价目表',   'menu', 'crm:price:list',      '/product/price',  'product/price/list',   'lucide:tags',        20, 1),
    (103, 100, 'QuoteList',        '报价单',   'menu', 'crm:quote:list',      '/product/quote',  'product/quote/list',   'lucide:file-text',   30, 1),
    -- 交易管理：开票 / 退款
    (114, 110, 'InvoiceList',      '开票管理', 'menu', 'crm:invoice:list',    '/trade/invoice',  'payment/invoice/list', 'lucide:receipt-text', 40, 1),
    (115, 110, 'RefundList',       '退款管理', 'menu', 'crm:refund:list',     '/trade/refund',   'payment/refund/list',  'lucide:rotate-ccw',  50, 1),
    -- 营销管理
    (140, 0,   'CrmMarketing',     '营销管理', 'catalog', NULL,               '/marketing',      NULL,                   'lucide:megaphone',   58, 1),
    (141, 140, 'CampaignList',     '营销活动', 'menu', 'crm:campaign:list',   '/marketing/campaign', 'marketing/campaign/list', 'lucide:target', 10, 1),
    -- 审批中心（外部审批入口）
    (150, 0,   'ApprovalCenter',   '审批中心', 'menu', 'crm:approval:list',   '/approval',       'approval/index',       'lucide:workflow',    85, 1)
ON DUPLICATE KEY UPDATE
    pid = VALUES(pid), title = VALUES(title), type = VALUES(type), auth_code = VALUES(auth_code),
    path = VALUES(path), component = VALUES(component), icon = VALUES(icon),
    order_no = VALUES(order_no), status = VALUES(status);

-- =====================================================================
-- 四、迁移台账
-- =====================================================================
INSERT INTO sys_schema_migration (version, description)
VALUES ('V20261117_01', '营销活动 + 线索活动归因 + 审批实例镜像 + 菜单登记（价目/报价/开票/退款/营销/审批）')
ON DUPLICATE KEY UPDATE description = VALUES(description);
