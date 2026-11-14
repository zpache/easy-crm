# 数据库迁移约定

## 目录职责

| 文件 | 用途 |
| --- | --- |
| `../init.sql` | **全新安装基线**：建表 + 初始数据，只用于空库初始化 |
| `VyyyyMMdd_NN__description.sql` | **增量迁移**：在已有库上按版本号顺序执行，每次变更一个脚本 |

新功能加表时两处都要维护：`init.sql` 补全新安装的基线定义，`migration/` 补已有库的升级脚本；
两者产出的 schema 必须一致（QA 验收项：空库执行 init 与「init + 全部增量」结果一致）。

## 命名与顺序

- 文件名格式 `V<日期>_<两位序号>__<小写下划线说明>.sql`，按文件名升序执行。
- 同一批变更禁止修改已执行过的脚本，新增一个新版本脚本来纠正。

## 幂等要求（强制）

每个脚本必须能重复执行 3 次以上而不报错、不产生重复数据：

1. 建表：`CREATE TABLE IF NOT EXISTS`，所有新表带 BaseEntity 审计列
   （`id` 自增主键、`create_time`、`update_time`、`create_by`、`update_by`、`version`、`deleted`）。
2. 加列：MySQL 8.4 无 `ADD COLUMN IF NOT EXISTS`，统一用
   `information_schema.COLUMNS` + `PREPARE / EXECUTE / DEALLOCATE PREPARE` 动态 SQL 判存在。
3. 种子数据：`INSERT ... ON DUPLICATE KEY UPDATE`。
4. 脚本开头自建 `sys_schema_migration`（`CREATE TABLE IF NOT EXISTS`），
   结尾登记版本：`INSERT INTO sys_schema_migration (version, description) VALUES (...) ON DUPLICATE KEY UPDATE ...`。
5. 禁止 `DROP TABLE` / `TRUNCATE` / 无条件 `UPDATE` 业务数据。

## 执行方式

项目当前未接入 Flyway/Liquibase，按版本号手工执行并核对台账：

```bash
mysql -u<user> -p<database> < backend/db/migration/V20261115_01__security_hardening.sql
mysql -u<user> -p<database> -e "SELECT * FROM sys_schema_migration ORDER BY version;"
```

> 菜单种子仍按 `AGENTS.md` 的约定维护在 `init.sql` 的 `sys_permission` 段，并在当前库同步执行；
> 菜单是每次请求实时读取的，执行后刷新前端即可见。注意逻辑删除过的菜单行会占用自增 id，
> 若 id 撞上已删行，先物理 DELETE 再执行种子脚本。
