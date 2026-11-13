# easy-crm backend

Java 25 · Spring Boot 4.1.2 · MyBatis-Plus 3.5.18（`mybatis-plus-spring-boot4-starter` + `mybatis-plus-jsqlparser`）· MySQL 8.4（utf8mb4 / InnoDB）。

单模块工程，按业务域分包：`com.easykits.crm.{module}.{controller|service|mapper|entity|dto|vo}`。

## 依赖清单（pom.xml）

| 依赖 | 用途 |
| --- | --- |
| spring-boot-starter-parent 4.1.2 | 父 POM，统一依赖版本管理 |
| spring-boot-starter-web | RESTful API（Spring MVC + 内嵌 Tomcat），供 Vue 前端调用 |
| spring-boot-starter-validation | JSR-380 参数校验（DTO 注解） |
| mybatis-plus-spring-boot4-starter 3.5.18 | MyBatis-Plus 官方为 Spring Boot 4 提供的专用 starter（当前最新，内含 mybatis-spring 4.x） |
| mybatis-plus-jsqlparser 3.5.18 | MP 3.5.9 起分页/防全表更新/数据权限等 SQL 解析插件被拆分到此模块，必须显式引入 |
| mysql-connector-j（runtime） | MySQL 8.4 官方驱动，版本由 Boot BOM 管理（9.7.0） |
| lombok（optional） | 实体 / DTO / 配置类样板代码，Boot BOM 管理（1.18.48，支持 JDK 25） |
| spring-boot-starter-test（test） | JUnit 5 / AssertJ |

无版本冲突：MP starter 自身 import spring-boot-dependencies 4.1.0，与本项目 parent 4.1.2 兼容；已用 JDK 25（corretto-25.0.5）编译验证通过。

## 统一响应与异常

- `common/api/Result<T>`：`code / message / data`，成功码 0（与 Vben 前端约定一致）
- `common/api/ResultCode`：框架级沿用 HTTP 语义（400/401/403/404/500），业务码从 4xxx 起按模块分段
- `common/exception/BizException`：业务异常，携带业务码
- `common/exception/GlobalExceptionHandler`：`@RestControllerAdvice` 全局兜底。
  约定：业务异常返回 HTTP 200 + 业务码；参数校验失败/资源不存在等返回对应 HTTP 状态码

## 登录上下文

- `common/context/LoginUser`（record）：userId、deptId、username、nickname、roles、dataScope
- `common/context/DataScopeType`：SELF / DEPT / DEPT_AND_CHILD / ALL（数值 1/2/3/4，未知值兜底为 SELF）
- `common/context/LoginUserContext`：ThreadLocal 持有当前登录主体，认证过滤器负责 set/clear

## MyBatis-Plus 配置（config/）

`MybatisPlusConfig` 注册的插件（顺序有讲究，数据权限在前、分页必须最后）：

1. `DataPermissionInterceptor`（行级数据权限，见下文）
2. `OptimisticLockerInnerInterceptor`（乐观锁，配合 BaseEntity 的 @Version）
3. `PaginationInnerInterceptor`（分页，单页上限 200）
4. `BlockAttackInnerInterceptor`（防全表 update/delete）

`AuditMetaObjectHandler` 自动填充 create_time / update_time / create_by / update_by / version（update 必须用 setFieldValByName 覆盖，不能用 strictUpdateFill）。

`JacksonConfig`：LocalDateTime 统一输出 `yyyy-MM-dd HH:mm:ss`，反序列化宽松兼容 ISO / 空格分隔 / 仅日期 / 毫秒时间戳（Boot 4 默认 Jackson 3，包名为 tools.jackson.*）。

## BaseEntity（entity/BaseEntity.java）

| 字段 | 列 | 说明 |
| --- | --- | --- |
| id | id BIGINT AUTO_INCREMENT | **自增主键**（全库约定）：单库单体下自增即可保证唯一，值域远小于 JS 2^53 安全整数，无需雪花算法的机器号分配与时钟回拨处理；未来拆多写库再迁移 |
| createTime / updateTime | create_time / update_time | 自动填充 |
| createBy / updateBy | create_by / update_by | 自动填充当前登录用户 userId |
| version | version | @Version 乐观锁 |
| deleted | deleted | @TableLogic 逻辑删除（0/1），全局配置于 application.yml |

继承 BaseEntity 的业务表建表时必须携带上述全部列，字符集 utf8mb4、引擎 InnoDB。

## 行级数据权限（common/datascope/）

组成：

| 类 | 职责 |
| --- | --- |
| `DataScopeType` | 数据范围枚举：本人 / 本部门 / 本部门及下级 / 全部 |
| `DataScopeTable` | 一张表的过滤配置（tableName + ownerColumn + deptColumn） |
| `DataScopeTableRegistry` | 表注册表：登记哪些业务表参与行级过滤 |
| `DataScopeHandler` | 实现 MP 的 `MultiDataPermissionHandler`，按表逐个向 WHERE 注入条件 |

已接入：`crm_lead`（线索，owner_id + dept_id）。

注入规则（列名带表别名限定，多表 join 不歧义）：

| 数据范围 | 注入条件 |
| --- | --- |
| SELF | `{t}.owner_id = ?userId` |
| DEPT | `{t}.dept_id = ?deptId` |
| DEPT_AND_CHILD | `{t}.dept_id IN (WITH RECURSIVE dept_tree AS (...) SELECT id FROM dept_tree)` |
| ALL / 未登录 | 不追加条件（TODO：认证上线后未登录必须 fail closed） |

### 如何接入一张新表

1. 表上确认有 `owner_id`（归属人）或 `dept_id`（归属部门）列，至少其一；列名不同则在 `DataScopeTable` 构造时指定。
2. 在 `DataScopeTableRegistry` 构造器里登记一行：

   ```java
   register(DataScopeTable.of("crm_customer"));            // owner_id + dept_id
   register(new DataScopeTable("crm_follow_log", "owner_id", null)); // 仅 owner_id
   ```

3. 实体继承 `BaseEntity`，Mapper 用 MyBatis-Plus 的常规查询（`selectPage` / `selectList` / 自定义 XML 均可，拦截器对两者都生效）。
4. 无需其他改动；未登记的表（如 sys_user、sys_dept 等系统表）不做行级过滤，可见性由 RBAC 功能权限控制。

### 待办（TODO，随后续轮次完成）

1. **认证接入**：JWT 过滤器解析 token 后构造 LoginUser（含 deptId、dataScope，dataScope 取用户全部角色中数据范围最大者）写入 LoginUserContext，并在 finally 中 clear。当前无认证过滤器，`DataScopeHandler` 对未登录放行，**上线前必须移除该放行逻辑**。
2. **DEPT_AND_CHILD 子树 SQL**：当前按「sys_dept.pid 指向父部门、根 pid=0」编写递归 CTE；若实际 sys_dept 用 ancestors 路径列，改为 `find_in_set(deptId, ancestors)` 单层子查询。
3. **豁免清单**：定时任务、跨部门报表等场景可按 mappedStatementId 在 `DataScopeHandler.getSqlSegment` 增加白名单。
4. **fail closed**：`buildCondition` 缺列、`parse` 失败时当前仅告警放行，接入后改为抛 BizException。

## 业务模块

### 线索管理（modules/lead，表 crm_lead）

- 状态机：新建(0) → 跟进中(1) → 已验证(2)，不合格(3) 可从 0/1 进入；只能前进，进入「已验证」只能走转化接口（`LeadStatus`）
- 来源字典：`LeadSource` 常量类（官网/电话/广告投放/客户转介绍/展会活动/其他），后续迁字典表
- 查重：手机号 / 邮箱业务层全局唯一；公司名（name）重复仅提示不阻断（VO 的 companyDuplicate + duplicates）
- 转化 `POST /leads/{id}/convert`：`@Transactional` 内创建/关联客户、联系人（均可选）+ 必须创建商机，成功后回写 converted_* 三字段并置已验证；客户/联系人/商机表轮次 3 建立，当前为占位服务（伪 ID、不写库），接口签名与事务边界不变
- 公海：`/leads/{id}/to-pool`（置空 owner）、`/leads/{id}/claim`（领取）；自动回收定时任务骨架 `LeadRecycleTask`（cron：application.yml `easy-crm.lead.recycle-cron`，默认每天 2 点；天数 `easy-crm.lead.recycle-days`，默认 15）
- Excel 导入 `POST /leads/import`：EasyExcel 流式解析（模板列：线索名称|联系人|手机号|邮箱|来源|备注），逐行校验、错误行带行号集中返回，单行失败不中断

### 客户/联系人管理（modules/customer、modules/contact，表 crm_customer / crm_contact）

- 客户查重：名称 + 信用代码组合，重复阻断并提示已存在客户 ID（CustomerServiceImpl.findDuplicate）
- 合并 `POST /customers/merge`：事务内迁移联系人、线索转化关联（商机/跟进 TODO 轮次 3/4），source 标记已合并并退出默认列表
- 360 视图 `GET /customers/{id}/360`：固定 2 次查询（客户 + 联系人），商机/跟进返回空数组待后续填充
- 联系人：挂在客户下；主联系人唯一性用「事务内先清后设」（(customer_id,is_primary) 唯一索引会误伤多条普通联系人，DDL 注释有说明）
- 占位服务已转真实：CustomerService/ContactService 接口签名不变，线索转化真实落库（客户查重失败整笔回滚并提示走关联已有）
- 公海：与线索规则独立配置（easy-crm.customer.recycle-days 默认 30，cron recycle-cron 默认每天 02:30）
- 行级数据权限：crm_customer 已注册；联系人可见性随客户

## 运行

```bash
# 需 JDK 25（本机为 corretto-25.0.5），Maven 3.6.3+
export JAVA_HOME=~/Library/Java/JavaVirtualMachines/corretto-25.0.5/Contents/Home
mvn compile          # 编译验证
MYSQL_PASSWORD=xxx mvn spring-boot:run
```

配置：`application.yml`（数据源 / MP / 日志级别），数据库密码经 `MYSQL_PASSWORD` 环境变量或 gitignore 的 `application-local.yml` 注入；SQL 日志由 `com.easykits.crm.**.mapper: DEBUG` 控制。
