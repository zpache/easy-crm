-- =====================================================================
-- V20261115_01 安全加固：迁移台账 + sys_user.token_version
-- 幂等：可重复执行；对已有数据库只做增量，不覆盖任何业务数据
-- =====================================================================
SET NAMES utf8mb4;

-- 迁移台账：每个迁移脚本执行成功后登记一行，便于核对已应用的版本
CREATE TABLE IF NOT EXISTS sys_schema_migration (
    version     VARCHAR(64)  NOT NULL COMMENT '脚本版本号，如 V20261115_01',
    description VARCHAR(255) NULL COMMENT '变更说明',
    applied_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '执行时间',
    PRIMARY KEY (version)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT ='数据库迁移台账';

-- MySQL 8.4 不支持 ADD COLUMN IF NOT EXISTS，用 information_schema + 动态 SQL 保证幂等
SET @col_sql = (
    SELECT IF(
        (SELECT COUNT(*) FROM information_schema.COLUMNS
          WHERE TABLE_SCHEMA = DATABASE()
            AND TABLE_NAME = 'sys_user'
            AND COLUMN_NAME = 'token_version') > 0,
        'SELECT 1',
        'ALTER TABLE sys_user ADD COLUMN token_version BIGINT NOT NULL DEFAULT 0 COMMENT ''令牌版本：改密/登出/停用时递增，旧 JWT 立即失效'''
    )
);
PREPARE col_stmt FROM @col_sql;
EXECUTE col_stmt;
DEALLOCATE PREPARE col_stmt;

INSERT INTO sys_schema_migration (version, description)
VALUES ('V20261115_01', '安全加固：令牌版本列与迁移台账')
ON DUPLICATE KEY UPDATE description = VALUES(description);
