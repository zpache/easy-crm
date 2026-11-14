-- =====================================================================
-- easy-crm 批次 3：产品域 + 交易域 + 报表中心（MySQL 8.4 / utf8mb4 / InnoDB）
--
-- 幂等约定（与后续批次脚本保持一致）：
--   1. 建表一律 CREATE TABLE IF NOT EXISTS；
--   2. 对「可能已被其它批次先建好但缺列」的表，用 information_schema + PREPARE
--      动态 SQL 做 ADD COLUMN IF NOT EXISTS 式补列（MySQL 无原生 ADD COLUMN IF NOT EXISTS）；
--   3. 种子数据一律 INSERT ... ON DUPLICATE KEY UPDATE；
--   4. 脚本开头自建 sys_schema_migration 登记表，末尾登记本版本号，重复执行不报错。
--
-- 所有新表统一携带 BaseEntity 审计列：id BIGINT UNSIGNED AUTO_INCREMENT /
-- create_time / update_time / create_by / update_by / version / deleted，
-- 主键 + 必要索引，金额一律 DECIMAL(15,2)（Java 侧 BigDecimal）。
-- 不建任何 DB 外键：项目全程逻辑删除 + 无外键体系（见 init.sql 说明）。
-- =====================================================================

SET NAMES utf8mb4;

-- ---------------------------------------------------------------- 版本登记表（脚本自建，幂等）
CREATE TABLE IF NOT EXISTS sys_schema_migration (
    version     VARCHAR(64)  PRIMARY KEY,
    description VARCHAR(255) NULL,
    applied_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT ='数据库脚本版本登记';

-- =====================================================================
-- 一、产品域
-- =====================================================================

-- ---------------------------------------------------------------- 产品
CREATE TABLE IF NOT EXISTS crm_product (
    id             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    code           VARCHAR(64)     NOT NULL COMMENT '产品编码，业务唯一',
    name           VARCHAR(128)    NOT NULL COMMENT '产品名称',
    category       VARCHAR(64)     NULL COMMENT '分类',
    specification  VARCHAR(128)    NULL COMMENT '规格',
    unit           VARCHAR(16)     NULL COMMENT '单位（件/套/小时…）',
    status         TINYINT         NOT NULL DEFAULT 1 COMMENT '状态：0 草稿 / 1 上架 / 2 停用',
    cost_price     DECIMAL(15, 2)  NULL COMMENT '成本价',
    standard_price DECIMAL(15, 2)  NULL COMMENT '标准价（目录价）',
    remark         VARCHAR(500)    NULL,
    create_time    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    create_by      BIGINT          NULL,
    update_by      BIGINT          NULL,
    version        INT             NOT NULL DEFAULT 0,
    deleted        TINYINT         NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_code (code),
    KEY idx_name (name),
    KEY idx_category_status (category, status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT ='产品';

-- ---------------------------------------------------------------- 价目表（产品 + 价格 + 生效/失效日期）
CREATE TABLE IF NOT EXISTS crm_price_list (
    id             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    product_id     BIGINT UNSIGNED NOT NULL COMMENT '产品 ID',
    price          DECIMAL(15, 2)  NOT NULL COMMENT '价格',
    effective_date DATE            NULL COMMENT '生效日期（NULL 表示立即生效）',
    expire_date    DATE            NULL COMMENT '失效日期（NULL 表示长期有效）',
    status         TINYINT         NOT NULL DEFAULT 1 COMMENT '1 有效 / 0 失效',
    remark         VARCHAR(500)    NULL,
    create_time    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    create_by      BIGINT          NULL,
    update_by      BIGINT          NULL,
    version        INT             NOT NULL DEFAULT 0,
    deleted        TINYINT         NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_product_date (product_id, effective_date, expire_date),
    KEY idx_status (status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT ='产品价目表';

-- ---------------------------------------------------------------- 商机产品明细快照
-- 明细为「下单/报价时刻」的价格与名称快照，不随 crm_product 后续改名改价而变化。
CREATE TABLE IF NOT EXISTS crm_opportunity_item (
    id             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    opportunity_id BIGINT UNSIGNED NOT NULL COMMENT '商机 ID',
    product_id     BIGINT UNSIGNED NULL COMMENT '产品 ID（可空表示手工录入的非标项）',
    product_name   VARCHAR(128)    NOT NULL COMMENT '产品名称快照',
    unit_price     DECIMAL(15, 2)  NOT NULL DEFAULT 0 COMMENT '单价快照',
    quantity       DECIMAL(15, 2)  NOT NULL DEFAULT 1 COMMENT '数量',
    discount       DECIMAL(15, 2)  NOT NULL DEFAULT 0 COMMENT '折扣金额（按行减免）',
    subtotal       DECIMAL(15, 2)  NOT NULL DEFAULT 0 COMMENT '小计 = 单价*数量 - 折扣',
    remark         VARCHAR(500)    NULL,
    create_time    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    create_by      BIGINT          NULL,
    update_by      BIGINT          NULL,
    version        INT             NOT NULL DEFAULT 0,
    deleted        TINYINT         NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_opportunity_id (opportunity_id),
    KEY idx_product_id (product_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT ='商机产品明细快照';

-- ---------------------------------------------------------------- 报价单
CREATE TABLE IF NOT EXISTS crm_quote (
    id             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    quote_no       VARCHAR(64)     NOT NULL COMMENT '报价单编号，业务唯一',
    opportunity_id BIGINT UNSIGNED NULL COMMENT '关联商机',
    customer_id    BIGINT UNSIGNED NOT NULL COMMENT '关联客户',
    total_amount   DECIMAL(15, 2)  NOT NULL DEFAULT 0 COMMENT '总金额（必须等于明细汇总）',
    status         TINYINT         NOT NULL DEFAULT 0 COMMENT '0 草稿 / 1 已提交 / 2 已确认 / 3 已作废',
    valid_until    DATE            NULL COMMENT '报价有效期（截止日）',
    owner_id       BIGINT UNSIGNED NULL COMMENT '负责人（行级数据权限）',
    dept_id        BIGINT UNSIGNED NULL COMMENT '归属部门（行级数据权限）',
    remark         VARCHAR(500)    NULL,
    create_time    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    create_by      BIGINT          NULL,
    update_by      BIGINT          NULL,
    version        INT             NOT NULL DEFAULT 0,
    deleted        TINYINT         NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_quote_no (quote_no),
    KEY idx_customer_id (customer_id),
    KEY idx_opportunity_id (opportunity_id),
    KEY idx_owner_status (owner_id, status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT ='报价单';

-- ---------------------------------------------------------------- 报价明细（独立表，不复用 opportunity_item）
-- 独立表理由：报价与商机的生命周期、父实体、权限口径均不同，复用会引入可空多态外键与歧义。
CREATE TABLE IF NOT EXISTS crm_quote_item (
    id           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    quote_id     BIGINT UNSIGNED NOT NULL COMMENT '报价单 ID',
    product_id   BIGINT UNSIGNED NULL COMMENT '产品 ID（可空表示非标项）',
    product_name VARCHAR(128)    NOT NULL COMMENT '产品名称快照',
    unit_price   DECIMAL(15, 2)  NOT NULL DEFAULT 0 COMMENT '单价',
    quantity     DECIMAL(15, 2)  NOT NULL DEFAULT 1 COMMENT '数量',
    discount     DECIMAL(15, 2)  NOT NULL DEFAULT 0 COMMENT '折扣金额',
    subtotal     DECIMAL(15, 2)  NOT NULL DEFAULT 0 COMMENT '小计 = 单价*数量 - 折扣',
    remark       VARCHAR(500)    NULL,
    create_time  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    create_by    BIGINT          NULL,
    update_by    BIGINT          NULL,
    version      INT             NOT NULL DEFAULT 0,
    deleted      TINYINT         NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_quote_id (quote_id),
    KEY idx_product_id (product_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT ='报价明细';

-- =====================================================================
-- 二、交易域
-- =====================================================================

-- ---------------------------------------------------------------- 合同
CREATE TABLE IF NOT EXISTS crm_contract (
    id             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    contract_no    VARCHAR(64)     NOT NULL COMMENT '合同编号，业务唯一',
    customer_id    BIGINT UNSIGNED NOT NULL COMMENT '客户 ID',
    opportunity_id BIGINT UNSIGNED NULL COMMENT '来源商机 ID',
    amount         DECIMAL(15, 2)  NOT NULL DEFAULT 0 COMMENT '合同金额',
    sign_date      DATE            NULL COMMENT '签约日期',
    status         TINYINT         NOT NULL DEFAULT 0 COMMENT '0 草稿 / 1 生效 / 2 已归档 / 3 已作废',
    owner_id       BIGINT UNSIGNED NULL COMMENT '负责人（行级数据权限）',
    dept_id        BIGINT UNSIGNED NULL COMMENT '归属部门（行级数据权限）',
    remark         VARCHAR(500)    NULL,
    create_time    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    create_by      BIGINT          NULL,
    update_by      BIGINT          NULL,
    version        INT             NOT NULL DEFAULT 0,
    deleted        TINYINT         NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_contract_no (contract_no),
    KEY idx_customer_id (customer_id),
    KEY idx_opportunity_id (opportunity_id),
    KEY idx_owner_status (owner_id, status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT ='合同';

-- ---------------------------------------------------------------- 订单
-- 状态机：0 草稿 -> 1 待确认 -> 2 已确认 -> 3 已交付 -> 4 已完成；9 已取消（见 OrderStatus）。
CREATE TABLE IF NOT EXISTS crm_order (
    id             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    order_no       VARCHAR(64)     NOT NULL COMMENT '订单编号，业务唯一',
    customer_id    BIGINT UNSIGNED NOT NULL COMMENT '客户 ID',
    opportunity_id BIGINT UNSIGNED NULL COMMENT '来源商机 ID',
    contract_id    BIGINT UNSIGNED NULL COMMENT '关联合同 ID',
    total_amount   DECIMAL(15, 2)  NOT NULL DEFAULT 0 COMMENT '总金额（必须等于明细汇总）',
    status         TINYINT         NOT NULL DEFAULT 0 COMMENT '0 草稿/1 待确认/2 已确认/3 已交付/4 已完成/9 已取消',
    ship_date      DATE            NULL COMMENT '发货日期',
    delivery_date  DATE            NULL COMMENT '交付日期',
    owner_id       BIGINT UNSIGNED NULL COMMENT '负责人（行级数据权限）',
    dept_id        BIGINT UNSIGNED NULL COMMENT '归属部门（行级数据权限）',
    remark         VARCHAR(500)    NULL,
    create_time    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    create_by      BIGINT          NULL,
    update_by      BIGINT          NULL,
    version        INT             NOT NULL DEFAULT 0,
    deleted        TINYINT         NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_order_no (order_no),
    KEY idx_customer_id (customer_id),
    KEY idx_opportunity_id (opportunity_id),
    KEY idx_contract_id (contract_id),
    KEY idx_owner_status (owner_id, status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT ='订单';

-- ---------------------------------------------------------------- 订单明细快照
CREATE TABLE IF NOT EXISTS crm_order_item (
    id           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    order_id     BIGINT UNSIGNED NOT NULL COMMENT '订单 ID',
    product_id   BIGINT UNSIGNED NULL COMMENT '产品 ID（可空表示非标项）',
    product_name VARCHAR(128)    NOT NULL COMMENT '产品名称快照',
    unit_price   DECIMAL(15, 2)  NOT NULL DEFAULT 0 COMMENT '单价快照',
    quantity     DECIMAL(15, 2)  NOT NULL DEFAULT 1 COMMENT '数量',
    discount     DECIMAL(15, 2)  NOT NULL DEFAULT 0 COMMENT '折扣金额',
    subtotal     DECIMAL(15, 2)  NOT NULL DEFAULT 0 COMMENT '小计 = 单价*数量 - 折扣',
    remark       VARCHAR(500)    NULL,
    create_time  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    create_by    BIGINT          NULL,
    update_by    BIGINT          NULL,
    version      INT             NOT NULL DEFAULT 0,
    deleted      TINYINT         NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_order_id (order_id),
    KEY idx_product_id (product_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT ='订单明细快照';

-- ---------------------------------------------------------------- 回款计划
CREATE TABLE IF NOT EXISTS crm_payment_plan (
    id             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    order_id       BIGINT UNSIGNED NOT NULL COMMENT '订单 ID',
    period_no      INT             NOT NULL DEFAULT 1 COMMENT '期次（同订单内唯一）',
    plan_date      DATE            NULL COMMENT '计划回款日期',
    plan_amount    DECIMAL(15, 2)  NOT NULL DEFAULT 0 COMMENT '计划金额',
    received_amount DECIMAL(15, 2) NOT NULL DEFAULT 0 COMMENT '已核销金额（回款核销时累加）',
    status         TINYINT         NOT NULL DEFAULT 0 COMMENT '0 未核销 / 1 部分核销 / 2 已核销',
    remark         VARCHAR(500)    NULL,
    create_time    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    create_by      BIGINT          NULL,
    update_by      BIGINT          NULL,
    version        INT             NOT NULL DEFAULT 0,
    deleted        TINYINT         NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_order_period (order_id, period_no),
    KEY idx_plan_date (plan_date)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT ='回款计划';

-- ---------------------------------------------------------------- 回款记录
CREATE TABLE IF NOT EXISTS crm_payment (
    id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    plan_id         BIGINT UNSIGNED NULL COMMENT '关联回款计划（可空=无计划直接回款）',
    order_id        BIGINT UNSIGNED NOT NULL COMMENT '订单 ID',
    amount          DECIMAL(15, 2)  NOT NULL DEFAULT 0 COMMENT '实收金额',
    payment_date    DATE            NULL COMMENT '回款日期',
    writeoff_status TINYINT         NOT NULL DEFAULT 0 COMMENT '核销状态：0 未核销 / 1 已核销',
    writeoff_time   DATETIME        NULL COMMENT '核销时间',
    remark          VARCHAR(500)    NULL,
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    create_by       BIGINT          NULL,
    update_by       BIGINT          NULL,
    version         INT             NOT NULL DEFAULT 0,
    deleted         TINYINT         NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_plan_id (plan_id),
    KEY idx_order_id (order_id),
    KEY idx_writeoff_status (writeoff_status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT ='回款记录';

-- ---------------------------------------------------------------- 开票申请
CREATE TABLE IF NOT EXISTS crm_invoice (
    id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    invoice_no  VARCHAR(64)     NULL COMMENT '发票号（开票后回填）',
    order_id    BIGINT UNSIGNED NOT NULL COMMENT '订单 ID',
    customer_id BIGINT UNSIGNED NOT NULL COMMENT '客户 ID',
    amount      DECIMAL(15, 2)  NOT NULL DEFAULT 0 COMMENT '开票金额',
    title       VARCHAR(128)    NULL COMMENT '发票抬头',
    tax_no      VARCHAR(64)     NULL COMMENT '税号',
    status      TINYINT         NOT NULL DEFAULT 0 COMMENT '0 待开票 / 1 已开票 / 2 已作废',
    apply_date  DATE            NULL COMMENT '申请日期',
    remark      VARCHAR(500)    NULL,
    create_time DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    create_by   BIGINT          NULL,
    update_by   BIGINT          NULL,
    version     INT             NOT NULL DEFAULT 0,
    deleted     TINYINT         NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_order_id (order_id),
    KEY idx_customer_id (customer_id),
    KEY idx_status (status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT ='开票申请';

-- ---------------------------------------------------------------- 退货 / 退款
CREATE TABLE IF NOT EXISTS crm_refund (
    id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    refund_no   VARCHAR(64)     NOT NULL COMMENT '退款单号，业务唯一',
    order_id    BIGINT UNSIGNED NOT NULL COMMENT '订单 ID',
    customer_id BIGINT UNSIGNED NOT NULL COMMENT '客户 ID',
    amount      DECIMAL(15, 2)  NOT NULL DEFAULT 0 COMMENT '退款金额',
    reason      VARCHAR(500)    NULL COMMENT '退款原因',
    status      TINYINT         NOT NULL DEFAULT 0 COMMENT '0 申请中 / 1 已审核 / 2 已退款 / 3 已驳回',
    refund_date DATE            NULL COMMENT '实际退款日期',
    remark      VARCHAR(500)    NULL,
    create_time DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    create_by   BIGINT          NULL,
    update_by   BIGINT          NULL,
    version     INT             NOT NULL DEFAULT 0,
    deleted     TINYINT         NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_refund_no (refund_no),
    KEY idx_order_id (order_id),
    KEY idx_customer_id (customer_id),
    KEY idx_status (status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT ='退货/退款';

-- =====================================================================
-- 三、报表中心
-- =====================================================================

-- ---------------------------------------------------------------- 销售目标
-- target_month 用 CHAR(7) 存 'YYYY-MM'；dept_id / owner_id 至少一个有值，
-- 达成分析按「年月 + 部门 + 负责人」与商机/订单实绩比对。
CREATE TABLE IF NOT EXISTS crm_sales_target (
    id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    target_month  CHAR(7)         NOT NULL COMMENT '目标年月，格式 YYYY-MM',
    dept_id       BIGINT UNSIGNED NULL COMMENT '部门 ID（NULL=不限部门）',
    owner_id      BIGINT UNSIGNED NULL COMMENT '负责人 ID（NULL=部门整体目标）',
    target_amount DECIMAL(15, 2)  NOT NULL DEFAULT 0 COMMENT '目标金额',
    remark        VARCHAR(500)    NULL,
    create_time   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    create_by     BIGINT          NULL,
    update_by     BIGINT          NULL,
    version       INT             NOT NULL DEFAULT 0,
    deleted       TINYINT         NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_month_dept_owner (target_month, dept_id, owner_id),
    KEY idx_target_month (target_month),
    KEY idx_dept_id (dept_id),
    KEY idx_owner_id (owner_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT ='销售目标';

-- =====================================================================
-- 四、补列保护（information_schema + PREPARE 动态 SQL）
-- 若其它批次已先建了同名表但缺列，CREATE TABLE IF NOT EXISTS 不会补列，
-- 这里用动态 SQL 逐列判断后 ADD COLUMN，保证本批次依赖的列一定存在。
-- 注意：动态 SQL 全部为脚本内常量，不拼接任何外部输入，无注入风险。
-- =====================================================================

-- crm_order.delivery_date
SET @ddl = (
    SELECT IF(COUNT(*) = 0,
              'ALTER TABLE crm_order ADD COLUMN delivery_date DATE NULL COMMENT ''交付日期''',
              'SELECT 1')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'crm_order' AND COLUMN_NAME = 'delivery_date'
);
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- crm_order.status
SET @ddl = (
    SELECT IF(COUNT(*) = 0,
              'ALTER TABLE crm_order ADD COLUMN status TINYINT NOT NULL DEFAULT 0 COMMENT ''订单状态''',
              'SELECT 1')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'crm_order' AND COLUMN_NAME = 'status'
);
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- crm_quote.total_amount
SET @ddl = (
    SELECT IF(COUNT(*) = 0,
              'ALTER TABLE crm_quote ADD COLUMN total_amount DECIMAL(15,2) NOT NULL DEFAULT 0 COMMENT ''总金额''',
              'SELECT 1')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'crm_quote' AND COLUMN_NAME = 'total_amount'
);
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- crm_payment.writeoff_status
SET @ddl = (
    SELECT IF(COUNT(*) = 0,
              'ALTER TABLE crm_payment ADD COLUMN writeoff_status TINYINT NOT NULL DEFAULT 0 COMMENT ''核销状态''',
              'SELECT 1')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'crm_payment' AND COLUMN_NAME = 'writeoff_status'
);
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- crm_payment_plan.received_amount
SET @ddl = (
    SELECT IF(COUNT(*) = 0,
              'ALTER TABLE crm_payment_plan ADD COLUMN received_amount DECIMAL(15,2) NOT NULL DEFAULT 0 COMMENT ''已核销金额''',
              'SELECT 1')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'crm_payment_plan' AND COLUMN_NAME = 'received_amount'
);
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- crm_sales_target.target_amount
SET @ddl = (
    SELECT IF(COUNT(*) = 0,
              'ALTER TABLE crm_sales_target ADD COLUMN target_amount DECIMAL(15,2) NOT NULL DEFAULT 0 COMMENT ''目标金额''',
              'SELECT 1')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'crm_sales_target' AND COLUMN_NAME = 'target_amount'
);
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- =====================================================================
-- 五、种子数据（幂等；以下 2 条为可选演示数据，上线前可删除）
-- =====================================================================
INSERT INTO crm_product (code, name, category, specification, unit, status, cost_price, standard_price, remark)
VALUES
    ('DEMO-001', '标准实施服务', '服务', '人天', '人天', 1, 800.00, 1500.00, '批次3演示数据，可删'),
    ('DEMO-002', '企业版许可', '软件', '年度订阅', '套',   1, 12000.00, 28000.00, '批次3演示数据，可删')
ON DUPLICATE KEY UPDATE name = VALUES(name), standard_price = VALUES(standard_price);

-- =====================================================================
-- 六、版本登记（脚本末尾，重复执行仅更新描述）
-- =====================================================================
INSERT INTO sys_schema_migration (version, description)
VALUES ('V20261115_02', '产品域/交易域/报表中心 建表与补列')
ON DUPLICATE KEY UPDATE description = VALUES(description);
