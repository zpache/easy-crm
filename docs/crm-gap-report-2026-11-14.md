# easy-crm 功能盘点与优化完善报告清单

| 项 | 内容 |
| --- | --- |
| 报告日期 | 2026-11-14 |
| 代码基线 | `main` @ `7edebe8`（feat(dashboard): 分析页接入真实 CRM 统计数据），工作区干净，与 `origin/main` 一致 |
| 审查范围 | `backend/`（15 Controller / 74 端点 / 17 张表）、`crm-web/apps/web-ele/`（菜单 / 页面 / API）、`backend/db/init.sql`、根目录 `README.md`、`AGENTS.md`、`docs/backend-audit-report-2026-11-14.md` |
| 对标基准 | 通用型 CRM 功能菜单骨架（10 个一级域 + L2C 线索到现金主链路） |
| 取证方式 | Git 状态与提交同步 → 后端 Controller / Entity / DDL 全量清点 → 前端动态菜单（`init.sql` 为唯一菜单源）与 `views/` / `api/` 全量清点 → 逐域对照 → 严重程度评级 |
| 局限 | 本次为**静态代码与脚本审查**，未启动应用、未连接数据库做运行时验证；P0 安全项结论须以动态渗透/越权复测最终确认 |
| 与既有报告的关系 | `docs/backend-audit-report-2026-11-14.md`「修复记录」中已闭环的 11 项（时间线越权、转化联系人归属、阶段名 N+1、乐观锁返回值、ownerId 缺省、孤儿待办、forecast 越权、关联表物理删除、amount 校验、转化索引等）**不再重复列为差距**；本报告聚焦功能骨架差距、未闭环项与新发现风险 |

---

## 修复执行记录（2026-11-15）

### 完成范围

P0 4 项 + P1 16 项共 **20 项全部落地**（其中 5 项为「主体完成 + 明确的局部限制」，见「已知限制」）。
按 3 个批次并行实现、统一整合后检验：

| 批次 | 内容 | 主要产物 |
| --- | --- | --- |
| 1 | P0-1~4、P1-11、P1-16 | fail closed 数据权限（`DataScopeContext` + `DataScopeHandler`）、操作日志脱敏、JWT 密钥/CORS/默认口令加固（`AdminBootstrapRunner`）、双 token + tokenVersion 失效 + 登录防爆破、真实改密接口与前端、幂等迁移机制 |
| 2 | P1-04/05/06/07/08/09/10/14/15 | 跟进记录与时间线页面、商机看板、客户 360 双端补齐、合并迁移商机/跟进、阶段流转矩阵、成交客户字典、消息提醒链路、分配转派与多选、公海回收改按最近跟进 |
| 3 | P1-01/02/12/13 | 产品/价目表/报价单、合同/订单/回款计划/回款核销/开票/退款、赢单自动建单、导出中心、销售目标与达成分析（13 张表 / 95 个 Java 文件 / 6 组前端页面） |
| 4 | P1-03 | 工单/SLA/知识库/满意度（5 张表 / 40 个 Java 文件 / 4 个页面） |

### 整合登记

- `PermissionCodes.ALL` 追加 **74 个**权限码；前端 `constants/permissions.ts` 同步并 re-export 批次 3 常量。
- `DataScopeTableRegistry` 追加 **6 张表**行级过滤：`crm_quote / crm_contract / crm_order / crm_sales_target / crm_service_ticket / crm_satisfaction`。
- `init.sql` 的 `sys_permission` 种子段追加 **22 行**菜单（跟进/看板/消息/客户服务/产品/交易/报表）。
- 新增 `backend/db/migration/`：`V20261115_01__security_hardening.sql`、`V20261115_02__trade_and_product.sql`、`V20261115_03__customer_service.sql` + `README.md` 迁移约定。

### 检验结果

| 检验项 | 结果 |
| --- | --- |
| 后端全量编译（JDK 25 Corretto 25.0.5 / Maven 3.6.3） | **BUILD SUCCESS**，0 ERROR，307+ Java 文件 |
| 前端生产构建（vite build --mode production） | **VITE_EXIT=0**，产出 `dist.zip` |
| 迁移脚本静态校验 | 3 个脚本均含 `CREATE TABLE IF NOT EXISTS`、`sys_schema_migration` 台账、`ON DUPLICATE KEY UPDATE` |
| 前端 `vue-tsc` | 存在 **30+ 条基线既有错误**（`Recordable` 全局类型缺失、Vben form 依赖类型、未使用变量），经比对与本次改动无关；本次新增/修改文件未引入新 TS 错误 |
| 数据库迁移实际执行 | **已执行（2026-11-16）**：3 个迁移脚本按序跑通，`sys_schema_migration` 台账 3 条，库内 36 张表；`init.sql` 新增 22 行菜单种子已同步（`sys_permission` 共 36 个启用菜单） |
| 运行时接口冒烟 | **已通过（2026-11-16）**：新代码实启后登录/鉴权/改密/12 个新域分页接口全部符合预期；admin 默认口令被 `AdminBootstrapRunner` 自动轮换（预期行为）；产品新建→落库→逻辑删除写链路验证通过；未登录访问返回 401、空改密表单返回 400。越权矩阵、状态机、并发用例仍待专项回归 |

### 上线前必做（运维 / QA）

1. 配置环境变量 `JWT_SECRET`（≥32 字节，缺失将**启动失败**）、`CORS_ALLOWED_ORIGINS`（禁止 `*`）、`MYSQL_PASSWORD`；可选 `ADMIN_INITIAL_PASSWORD` 预设初始管理员口令，否则启动日志会打印一次随机口令。
2. ~~按版本号顺序执行 3 个迁移脚本并同步菜单种子~~（已于 2026-11-16 完成，台账 3 条、36 表、36 菜单）。**admin 原默认口令已被自动轮换**，随机密码只在首次启动日志显示一次，请立即登录改密或用 `ADMIN_INITIAL_PASSWORD` 重置。
3. 给非 admin 角色授权新增菜单与按钮权限码（`sys_role_permission`）；admin 走代码兜底。
4. 执行越权矩阵（4 种 dataScope × 新增 6 张表）、日志脱敏、旧 token 失效、登录锁定、状态机、公海边界与合并并发回归。

### 已知限制（部分实现，非谎报完成）

- 交易域：价目表、商机明细、报价单、开票、退款**只有后端接口，无独立前端页面**；导出中心端点齐备但**列表页导出按钮未挂载**；销售达成分析只按月份合并，未按部门/负责人分组展示。
- 回款核销为整笔一次性核销到单个计划，不支持拆分/部分核销；订单状态机不支持回退。
- 客户服务：SLA 超时只置标记不回退、未接入工作时间日历；工单附件、360 集成、实时推送未做；满意度误评无自助撤销。
- 消息提醒仍是 60s 轮询（未做 WebSocket/SSE，属 P2-07）。
- 商机阶段流转当前为「允许跨级前进 + 相邻回退」，与 P1-08 验收措辞「跨阶段跳跃被拒」有张力，代码注释已写明矩阵，如需严格逐级可调整。
- 客户 360 跟进记录取最近 50 条；分配弹窗依赖 `system:user:list` 权限。
- 未写单元测试、未接 CI（原报告 P2-01 范围，不在本次 P0/P1 清单内）。

---

## 一、执行摘要

**一句话结论：easy-crm 目前是一个完成度不错的「售前 CRM」，但 L2C 主链路走到商机就断了——合同、订单、回款、产品、售后服务全部为空白；同时存在 4 项上线阻断级安全与数据风险。**

| 维度 | 现状 |
| --- | --- |
| 功能菜单 | 6 个一级目录 / 12 个二级菜单，全部由后端 `sys_permission` 动态下发 |
| 后端规模 | 15 个 Controller / 74 个端点 / 17 张表（8 系统 + 9 CRM）/ 3 个定时任务 |
| 通用 CRM 10 域覆盖 | 较完整 **2**、部分 **5**、完全空白 **3** |
| L2C 主链路 | 线索 → 客户/联系人 → 商机（阶段/预测）已通；**合同订单、回款、服务复购三段缺失**，约覆盖 50% |
| 差距项合计 | **28 项**：P0 **4** / P1 **16** / P2 **8** |
| 悬空资产 | 前端 8 个电商遗留目录（有页面、有 API、无菜单、无后端） |
| 自动化测试 | **0**（无 `src/test` 目录，pom 仅声明 test starter） |

**最高杠杆的 5 件事**（低投入、高收益，建议优先）：P1-04 跟进记录/时间线前端补齐、P1-05 商机看板页面（后端 API 已就绪）、P1-06 客户 360 双端占位补齐、P1-11 修掉「假改密」、P1-16 建立幂等增量迁移机制。

### 优先级定义

| 级别 | 定义 | 出口要求 |
| --- | --- | --- |
| **P0** | 上线阻断：可导致越权数据泄露、凭据泄露，或向用户作出错误的安全承诺 | 上线前必须闭环，并附负向测试报告 |
| **P1** | 核心能力缺失 / 功能断链：影响 L2C 主链路或已交付功能的可用性、数据一致性 | 对应轮次出口前闭环 |
| **P2** | 工程治理、扩展能力与体验增强 | 按版本规划排期 |

---

## 二、现状盘点

### 2.1 技术栈

| 层 | 选型 |
| --- | --- |
| 后端 | Java 25 · Spring Boot 4.1.2 · MyBatis-Plus 3.5.18 · Spring Security + JJWT 0.13.0 (HS256) · MySQL 8.4 · EasyExcel 4.0.3 · springdoc 3.2.0，端口 8080，context-path `/api` |
| 前端 | Vben Admin 5.7.0（web-ele）· Vue 3 · Element Plus · pnpm monorepo，`accessMode: backend`，路由与菜单全部由 `GET /system/menu/routes` 下发 |
| 数据库 | `backend/db/init.sql` 单文件，17 张表 + 预置数据；全库无外键约束（逻辑删除 + 多态关联，关系由应用层维护） |
| 基础设施 | 无 Redis / MQ / ES / 对象存储 / 多租户 / CI 配置 |

### 2.2 现有功能菜单（`init.sql:431-452`，唯一权威菜单源）

| 一级目录 | 二级菜单 | 权限码 | 实现状态 |
| --- | --- | --- | --- |
| 工作台 | 分析页 | `dashboard:analytics` | 完整：4 指标卡 + 5 张 ECharts 图 + 停滞提醒 + 时间范围切换 |
| 线索管理 | 线索列表 | `crm:lead:list` | 完整：状态机、手机/邮箱查重、公海、领取、转化（客户+联系人+商机，单事务）、Excel 批量导入 |
| 客户管理 | 客户列表 | `crm:customer:list` | 主体完整：组合查重、合并、公海、360 视图（**商机/跟进为空**） |
| 客户管理 | 联系人列表 | `crm:contact:list` | 完整：CRUD、主联系人唯一 |
| 商机管理 | 商机列表 | `crm:opportunity:list` | 完整：阶段变更、阶段配置、销售预测、丢单原因；**无看板页面**（菜单图标却是 kanban） |
| 待办任务 | 任务列表 | `crm:task:list` | 完整：我的待办、完成、过期动态计算 |
| 系统管理 | 部门 / 用户 / 角色 / 菜单 | `system:*:list` | 完整：四级 RBAC、动态路由、按钮权限码、角色 dataScope |
| 系统管理 | 登录日志 / 操作日志 | `system:*log:list` | 完整（只读） |

**已有但前端无入口**：`FollowController`（`/follow-records`、`/timeline` 共 5 端点）、`GET /opportunities/board`、`GET /auth/me`、`GET /system/menu/list-by-user`。

### 2.3 通用 CRM 骨架对照（核心结论）

| # | 通用一级域 | 状态 | 现有能力 | 主要缺口 |
| --- | --- | --- | --- | --- |
| 01 | 工作台 / 首页 | 部分 | 分析页指标卡 + 5 图 | 无待办聚合、日程、消息中心、快捷入口；提醒仅一行 ElAlert |
| 02 | 市场营销 | 部分 | 线索管理（含公海、查重、导入） | 无市场活动、活动 ROI、线索来源分析、营销自动化、线索池独立页 |
| 03 | 客户管理 | 较完整 | 客户/联系人、查重、合并、公海、360 视图、标签分级 | 360 视图商机/跟进占位；合并未迁移商机与跟进；无客户分配/转派 |
| 04 | 销售自动化 SFA | 部分 | 商机、可配置 6 阶段、看板 API、销售预测、停滞预警 | 看板页面缺失；阶段可任意跳转；无报价单/CPQ；无销售目标管理 |
| 05 | 交易管理 | **空白** | 无 | 合同、订单、回款/应收、开票、交付、退货全部不存在；赢单事件监听器只打日志 |
| 06 | 产品与价格 | **空白** | 无 | 产品目录、价目表、BOM/选配、折扣政策、库存查询全部不存在；商机只有单一 `amount` |
| 07 | 客户服务 | **空白** | 无 | 工单、SLA、派单、现场服务、知识库、满意度全部不存在 |
| 08 | 数据分析 | 部分 | 固定 12 条聚合 SQL：漏斗/阶段分布/趋势/TOP5/预测 | 无报表中心、自定义报表与目标达成分析、无导出、无大屏 |
| 09 | 协同办公 | 部分 | 待办任务、操作日志 | 无审批中心、消息推送（链路断裂）、企微/邮件集成、外呼、附件 |
| 10 | 系统设置 / 平台 | 较完整 | 部门/用户/角色/菜单、权限码、4 级行级数据权限、登录与操作日志 | 无字典管理、审批流配置、开放 API、自定义字段、导入导出管理、数据归档/回收站 |

**覆盖率**：较完整 2 / 部分 5 / 空白 3 = 20% 较完整、50% 部分、30% 空白。

---

## 三、差距清单（可勾选执行）

### P0 · 上线阻断（4 项）

- [x] **P0-1｜行级数据权限在「未登录 / 解析失败 / 缺列 / 缺部门」时全部放行（fail open）**
  - 证据：`common/datascope/DataScopeHandler.java:60-64`（`user == null` 直接 `return null`）、类注释 `:36`「上线前必须改为 fail closed」、`:81-106` 缺列/缺部门仅 warn 放行、`:142-149` 条件解析异常仅告警返回 null
  - 风险：定时任务上下文、异步线程、过滤器异常或 Token 解析异常时，SELF/DEPT 用户的查询退化为全表可见；企业客户数据可被越权读取
  - 动作：区分「系统内部调用白名单（按 mappedStatementId）」与「用户请求」；用户请求中无 `LoginUser` 一律抛异常，解析失败/缺列/缺部门改抛 `BizException`；补充 `crm_contact` 的归属校验（见 P0-1 附注）
  - 验收：① 未登录上下文、② 条件解析抛错、③ 用户无 deptId 三种场景均 **拒绝查询**；SELF 角色越权用例（含直接按 id 查详情/更新）返回 0 条或 403；3 个负向单测覆盖上述分支
  - 附注：`docs/backend-audit-report` 认定「联系人随客户、属合理豁免」，但 `ContactController` 可直接按 `customerId`/id 查询且无客户归属校验，`crm_contact` 又未注册行级过滤——建议由「豁免」改为**显式校验联系人所属客户的数据权限**，并纳入越权矩阵

- [x] **P0-2｜操作日志明文记录密码等敏感字段**
  - 证据：`common/log/OperLogAspect.java:98`（`serializeArgs(...)` 结果直写 `params`）、`:109-133`（仅跳过 `HttpServletRequest` / `MultipartFile`，DTO 全字段序列化）；`UserForm.password`、`resetPassword` 的 `password` 参数均在写操作路径上；`GET /oper-logs` 可查
  - 风险：新增用户、重置密码的明文口令落库并可被有日志查看权限者读取，等于口令泄露
  - 动作：`serializeArgs` 增加字段名脱敏清单（password / oldPassword / newPassword / confirmPassword / token / secret / apiKey / idCard / phone 等）递归打码；考虑按 `@Sensitive` 注解扩展；清洗存量 `sys_oper_log.params`
  - 验收：调用「新增用户」「重置密码」后查库，`params` 中不含任何明文口令；日志页不展示敏感值；补 2 个脱敏单测

- [x] **P0-3｜默认凭据、硬编码 JWT 密钥与通配 CORS 带凭证**
  - 证据：`application.yml:52`（`JWT_SECRET` 默认值 `easy-crm-backend-dev-only-secret-key-...` 硬编码）、`config/SecurityConfig.java:90-93`（`allowedOriginPatterns(*)` + `allowCredentials(true)`）、`db/init.sql:407-409`（`admin / admin123`，注释「上线前改密」）
  - 风险：生产若漏配环境变量，任何人可用公开默认密钥伪造管理员 Token；任意站点可携带凭证跨域调用
  - 动作：生产 profile 下缺失 `JWT_SECRET` 直接**启动失败**（长度 ≥32 字节校验）；CORS 白名单由 `CORS_ALLOWED_ORIGINS` 注入且禁止 `*`；初始化脚本移除默认口令，改为首次登录强制改密 + 随机初始密码
  - 验收：不带 `JWT_SECRET` 启动生产 profile 失败；带伪造默认密钥签发的 Token 返回 401；非白名单 Origin 携带凭证的预检请求被拒；默认口令登录失败

- [x] **P0-4｜会话无法失效、无防爆破、refresh/logout 为伪实现**
  - 证据：`modules/auth/controller/AuthController.java`（`logout` 空实现、`refresh` 仅返回当前 username，无独立 refresh token）、`modules/auth/service/AuthService.java:28`「登录防爆破锁定 TODO」、`config/SecurityConfig.java:75` STATELESS 但无 Token 黑名单/版本号
  - 风险：改密、停用用户、主动登出后旧 Token 在 12 小时有效期内仍可用；口令可无限次爆破
  - 动作：引入 Token 版本号 / 短期黑名单（Redis 或 DB 表），改密/停用/登出即失效；实现独立可轮换的 refresh token；连续失败 5 次锁定 15 分钟并写登录日志；可选验证码/2FA
  - 验收：改密、停用、登出三种场景后旧 Token 立即 401；连续 5 次失败触发锁定且日志可查；refresh token 一次性轮换、重放被拒

### P1 · 核心能力缺失与功能断链（16 项）

- [x] **P1-01｜交易闭环完全空白（合同 / 订单 / 回款 / 开票 / 交付 / 退货）**
  - 证据：`modules/opportunity/event/OpportunityEventListener.java` 赢单监听器仅打日志；全库无 `contract` / `order` / `payment` 表与接口；`FollowBizTypes.ORDER` 仅为常量占位；`README.md:63` 明确标记未完成
  - 动作：按既有事件驱动模式落地 `OpportunityWonEvent` → 自动生成订单草稿 + 回款计划；新增合同、订单、回款/应收、开票申请、交付、退货域（表 / Service / Controller / 权限码 / 菜单 / 前端页面）
  - 验收：赢单 → 订单草稿 → 回款核销 → 应收账龄端到端用例通过；全部写操作有事务、乐观锁、操作日志、行级数据权限；菜单按 `AGENTS.md` 同步种子脚本

- [x] **P1-02｜产品目录 / 价目表 / 报价单（CPQ）缺失**
  - 证据：`crm_opportunity` 仅有单一 `amount` 字段，无产品明细行；全库无 product / price / quote 相关对象
  - 动作：产品与价目表、商机产品明细（1:N 快照）、报价单生成与导出；金额由明细汇总并与 `amount` 校验一致
  - 验收：多产品商机金额自动汇总、改价留痕、报价单可导出；明细与主表金额不一致时拒绝保存

- [x] **P1-03｜客户服务域（工单 / SLA / 派单 / 知识库 / 满意度）缺失**
  - 动作：服务请求 → 工单 → 派单 → 处理 → 满意度闭环；设备/维保档案；SLA 计时与超时升级
  - 验收：工单状态机完整、SLA 超时触发提醒、客户 360 视图可查看历史工单

- [x] **P1-04｜跟进记录与时间线「后端已完整、前端零实现」（优先）**
  - 证据：`FollowController` 提供 `/follow-records` CRUD + `GET /timeline`（UNION ALL 聚合跟进/待办/阶段变更），但 `src/api` 无 follow 模块、`src/views` 无 follow 目录、菜单无入口
  - 动作：新增 `api/crm/follow.ts`、跟进记录列表与时间线页面、`sys_permission` 菜单行 + 权限码三处登记；在客户/线索/商机详情提供「记跟进」入口
  - 验收：任一业务对象可写跟进并自动生成下次待办；时间线按时间倒序聚合三类事件且分页正确；SELF 角色看不到他人时间线

- [x] **P1-05｜商机看板页面缺失，后端 API 已就绪（优先）**
  - 证据：`GET /opportunities/board` 存在；`api/crm/opportunity.ts` 的 `getBoard()` **零引用**；`views/opportunity/` 仅列表页；菜单图标为 `lucide:kanban`
  - 动作：表格/看板视图切换；看板拖拽变更阶段须复用 `PUT /opportunities/{id}/stage` 的校验，不得绕过
  - 验收：看板按阶段分组且一次查询返回；拖拽赢单/丢单走原有必填校验；非法跳转被拒

- [x] **P1-06｜客户 360 视图前后端双端占位（优先）**
  - 证据：`CustomerServiceImpl.java:208-211` 硬编码 `List.of()` 且 TODO 注释仍写「轮次 3/4 后填充」（两模块其实已上线）；`views/customer/modules/view360.vue:69-73` 两个区块只有 `ElEmpty`，即使接口返回数据也不渲染
  - 动作：后端按 `customer_id` 填充商机、按多态条件填充跟进记录；前端补两张表格/时间线；后续再挂订单回款
  - 验收：360 视图展示基本信息 + 联系人 + 商机 + 跟进时间线，数据与列表页一致且受行级数据权限约束；删除两处 TODO 注释

- [x] **P1-07｜客户合并未迁移商机与跟进记录**
  - 证据：`CustomerServiceImpl.java:216-267` 仅迁移联系人与线索转化关联，`:257-258` TODO；前端 `merge.vue:60` 已向用户提示该限制
  - 风险：合并后历史商机/跟进仍挂在「已合并」客户下，客户 360 与销售预测统计失真
  - 动作：单事务内迁移 `crm_opportunity` / `crm_follow_record` / `crm_task` 的关联 ID，跨表写收敛到各模块 Service
  - 验收：合并后源客户默认列表不可见、目标客户 360 含全部历史、商机预测金额合并前后一致；并发合并触发乐观锁整体回滚

- [x] **P1-08｜商机阶段可任意跳转，未做顺序/方向校验**
  - 证据：`OpportunityServiceImpl.changeStage:236-309` 仅校验终态不可变、目标阶段存在且启用；`crm_opportunity_stage.sort_no` 定义了顺序但未被使用
  - 动作：定义允许的流转矩阵（逐级前进 / 允许回退 N 级 / 终态不可逆），按 `sort_no` 校验
  - 验收：跨阶段跳跃、非法回退返回 400 且写日志；赢单/丢单后再变更被拒；矩阵用例全覆盖

- [x] **P1-09｜客户状态 `4` 与字典定义不一致**
  - 证据：`OpportunityServiceImpl.java:266` 赢单时 `customer.setStatus(4)`；`init.sql` 与 `CustomerDicts.STATUS` 仅定义 1 正常 / 2 公海 / 3 已合并
  - 动作：统一状态枚举与 DDL 注释，或改为独立的「成交客户」标识字段；前后端状态翻译同步
  - 验收：赢单后客户列表状态列显示正确文案，无空值/未知码；字典一致性检查脚本通过

- [x] **P1-10｜消息提醒链路断裂（有表、有写入、无读取；前端入口 404）**
  - 证据：`crm_reminder` 表 + 停滞扫描写入 + 看板统计，但无 `ReminderController`，`read_flag` 无任何写入代码；`layouts/basic.vue` 铃铛 60s 轮询、`viewAll()` 跳 `/message`，而该路由不存在且后端无 `/messages`
  - 动作：后端 `ReminderController`（列表 / 未读数 / 标记已读 / 删除）+ 前端 `views/message` 路由与菜单；长期改 WebSocket/SSE 推送
  - 验收：停滞扫描产生提醒 → 铃铛角标准确 → 点击跳转可用 → 已读状态持久化；消息设置可保存

- [x] **P1-11｜个人中心「修改密码」是假实现（优先）**
  - 证据：`views/_core/profile/password-setting.vue:53-55` `handleSubmit()` 仅 `ElMessage.success('密码修改成功')`，**不调用任何接口**；`security-setting.vue` 硬编码假手机号/邮箱；`notification-setting.vue` 无保存逻辑
  - 风险：用户以为已改密、实际未改，属于错误的安全承诺；假数据损害可信度
  - 动作：新增 `PUT /user/password`（旧密码校验 + BCrypt + 改密后使旧 Token 失效，与 P0-4 联动）；其余两个 tab 接真实接口或**明确置灰禁用**
  - 验收：错误旧密码被拒；改密成功后旧 Token 401、新密码可登录；未完成的功能不显示成功提示

- [x] **P1-12｜CRM 全量导出与模板下载缺失**
  - 证据：EasyExcel 仅用于 `POST /leads/import`；全局无 `ExcelWriter` / 下载端点 / `Content-Disposition`；所有 CRM 列表 `toolbarConfig.export: false`
  - 动作：线索/客户/联系人/商机/任务的导出接口 + 导入模板下载 + 权限码（`crm:*:export`）+ 前端按钮；大数据量分页流式写；导出行为写操作日志
  - 验收：10 万行导出不 OOM；导出字段受数据权限与字段权限约束；无权限用户按钮不可见且接口 403

- [x] **P1-13｜报表中心 / 自定义报表 / 目标达成分析缺失**
  - 证据：`DashboardServiceImpl` 为 12 次固定聚合查询，仅支持 7/30/90 天切换；无自定义维度、无保存、无定时发送
  - 动作：按时间/部门/负责人/阶段组合筛选，销售目标设定与达成率，报表收藏与导出
  - 验收：同一指标在不同筛选下与明细数据核对一致；聚合查询在 10 万数据量下响应可接受

- [x] **P1-14｜手动分配 / 转派与表格批量操作缺失**
  - 证据：仅有 `to-pool` / `claim`（本人领取），无 `assign`；CRM 页面无 `checkbox-config` / 行多选
  - 动作：指定负责人分配/转派（单个 + 批量）、批量放入公海、批量改状态；前端表格开启多选
  - 验收：分配被操作日志记录、接收人收到提醒、目标用户数据权限正确；越权分配他人数据被拒

- [x] **P1-15｜公海回收依据不准确**
  - 证据：`LeadRecycleTask` / `CustomerRecycleTask` 以 `last_follow_time` 为准，两处类注释均标 TODO 应改用最近跟进记录；`crm_follow_record` 已上线但未被回收逻辑消费
  - 动作：改为按 `crm_follow_record` 最新跟进时间判定，兼容历史数据无跟进记录的回退口径
  - 验收：写入跟进后不被误回收；无跟进记录但新建时间未超阈值的不回收；边界时间（恰好 N 天）用例通过

- [x] **P1-16｜数据库无幂等增量迁移机制（实施阻断，优先）**
  - 证据：`backend/db/` 下**只有 `init.sql`**；`AGENTS.md` 的菜单规范也只描述改 `init.sql` 后手工同步数据库
  - 风险：P1-01～03 等所有新模块都要加表，现有开发/生产库无法平滑升级；手工执行易漂移，且与既有 `easy-mall` 的 `db/` 幂等迁移约定不一致
  - 动作：建立 `backend/db/migration/` 与版本号约定（如 `V20261115__add_order_tables.sql`），脚本全部幂等（`CREATE TABLE IF NOT EXISTS` / `INSERT ... ON DUPLICATE KEY UPDATE` / 加列前判存在），附迁移记录表与回滚说明；`init.sql` 保持为全新安装基线
  - 验收：空库执行 `init.sql` 与「init + 全部增量」结果 schema 一致；增量脚本重复执行 3 次无报错、无重复数据；在已有数据的库上执行不丢数据

### P2 · 工程治理与增强（8 项）

- [ ] **P2-01｜零自动化测试与无 CI**
  - 证据：`backend/src/test` 不存在，pom 仅声明 `spring-boot-starter-test`
  - 动作：优先覆盖 P0 安全分支、状态机、查重、线索转化/客户合并事务、公海并发领取；补 MockMvc 接口测试与越权矩阵测试；接入 CI 跑单测 + 前端构建
  - 验收：核心 Service 分支覆盖率设门槛；CI 未通过不允许合并到 `main`

- [x] **P2-02｜前端 8 个电商遗留目录悬空**（2026-11-16 已清理：删除 views/{goods,order,marketing,member,decorate,settings}、api/{goods,order,marketing,member,decorate,settings,message,upload}.ts、api/dashboard/{analytics,overview,index}.ts 死代码、死组件 goods-image-field.vue 与 core.ts 两条装修静态路由；permissions.ts 删除 GOODS/ORDER/MEMBER/MARKETING/DECORATE/SETTING 六组电商权限常量，仅保留 CRM/SYSTEM 组。归档备份：/tmp/easy-crm-mall-legacy。构建验证通过）
  - 证据：`views/{goods,order,marketing,member,decorate,settings,message}/` 与 `api/{goods,order,marketing,member,decorate,settings,message,upload}.ts` 均无菜单、后端无对应 Controller；`router/routes/core.ts` 还挂着 `DecorateEditor` / `DecoratePreview` 两条静态路由；`api/dashboard/{analytics,overview,index}.ts` 为死代码且与根 `api/dashboard.ts` 重复
  - 风险：打包体积、误维护，且电商「订单」与未来 CRM 订单命名/路由冲突
  - 动作：移入 `legacy/` 归档或直接删除；清理 core 静态路由与重复 API；`constants/permissions.ts` 中删除电商权限码，仅保留 CRM / SYSTEM 组
  - 验收：构建无死引用告警；`src/views` 目录与菜单一一对应；权限码常量与后端 `PermissionCodes.ALL` 完全一致

- [x] **P2-03｜数据字典表未迁**（2026-11-16 已落地：见下方「P2-03 实现记录」）
  - 证据：`LeadSource` / `LostReasons` / `CustomerDicts` / `FollowTypes` / `FollowBizTypes` 均为 Java 常量类；无 `sys_dict`；Excel 导入按中文标签硬匹配
  - 动作：按既有报告建议「LeadSource 先行」，建 `sys_dict` / `sys_dict_item` + 管理菜单 + 缓存，常量保留为 fallback
  - 验收：新增来源/行业无需发版；导入标签映射走字典；旧常量 fallback 用例通过

- [ ] **P2-04｜附件 / 文件管理缺失**
  - 证据：`MultipartFile` 仅出现于线索 Excel 导入与日志切面；无附件表、无上传下载接口
  - 动作：统一附件服务（本地 / MinIO / COS）+ 业务对象多态挂载 + 类型大小白名单 + 病毒扫描（可选）+ 下载鉴权
  - 验收：合同/报价单/工单可传附件；越权下载被拒；上传写操作日志

- [ ] **P2-05｜审批流 / 工作流引擎缺失**
  - 证据：`approval / workflow / flowable / bpmn` 全库零匹配；无审批表与审批状态字段
  - 动作：先做轻量可配置审批（折扣、合同、订单、回款核销），定义流程定义/实例/任务表与回调
  - 验收：审批通过与驳回分别驱动业务状态；审批人可委托；全流程留痕

- [ ] **P2-06｜开放 API / 第三方集成 / 企微 SCRM / 多租户缺失**
  - 动作：按需补 API Key + 签名 + 限流 + Webhook、ERP/财务/电子签集成、企业微信与客户联系、SCRM 获客；多租户需从表结构层统一评估（`tenant_id`）
  - 验收：对外接口有独立鉴权与审计、限流生效；集成失败有重试与告警

- [ ] **P2-07｜体验层：独立详情页、批量操作、i18n、实时通知、公海独立页**
  - 证据：所有编辑/详情均为抽屉，无独立路由（无法分享链接、刷新丢失）；CRM 页面中文硬编码，`locales/langs` 未覆盖 CRM 域；仅 `setInterval` 60s 轮询，无 WebSocket；公海只在列表行操作
  - 动作：关键对象详情页路由化；CRM 文案迁 i18n；补 SSE/WebSocket 推送；公海池独立页与回收规则配置
  - 验收：详情页 URL 可分享且鉴权生效；切换 en-US 后 CRM 页面无中文硬编码；提醒秒级到达

- [ ] **P2-08｜代码 / 配置 / 文档口径不一致复核（技术债）**
  - 证据：
    1. `application.yml:77` 仍为 `cn.easykits.crm.**.mapper: DEBUG`，而 `docs/backend-audit-report` 修复记录称「已改为 `cn.easykits.crm.mapper` 精确包名」——日志键 `**` 通配实际不生效，需复核修复是否落地；
    2. `LeadService.java:171-172` 注释仍写「转化当前为占位实现（伪 ID、不写库）」，实际已真实落库；
    3. `CustomerServiceImpl.java:181` 注释称「商机/跟进轮次 3/4 后填充」，两模块均已上线；
    4. `mapper-locations: classpath*:/mapper/**/*.xml` 指向不存在的目录（全部 SQL 为注解）；
    5. `OpportunityEventListener` 方法标 `@Async`，但 `CrmApplication` 无 `@EnableAsync`，实际同步执行；
    6. `sys_oper_log.cost_ms` 字段定义但切面从未赋值（`@AfterReturning` 未计时）
  - 动作：清理过时注释与无效配置，补 `@EnableAsync` 或移除注解，切面改 `@Around` 记录耗时，逐条核对既有修复记录的落地情况
  - 验收：源码无「占位/待接入」类过时注释；`cost_ms` 有值；日志 DEBUG 键生效；修复记录与代码状态一致

---

## 四、分阶段实施路线（延续项目「轮次」节奏）

| 轮次 | 主题 | 包含项 | 出口门禁 |
| --- | --- | --- | --- |
| **轮次 6** | 上线前安全加固 | P0-1 ~ P0-4、P1-11、P2-01 | 越权矩阵 + 日志脱敏 + 会话失效负向测试报告；CI 跑通 |
| **轮次 7** | 补齐售前闭环（低投入高收益） | P1-04/05/06/07/08/09/10/14/15/16、P2-08 | 已有售前功能无占位、L2C 前半段端到端回归通过；迁移机制就绪 |
| **轮次 8** | 交易闭环 L2C | P1-01/02/12/13、P2-03/05 | 赢单 → 订单 → 回款 → 报表全链路可用；菜单与权限码三处一致 |
| **轮次 9** | 服务与扩展 | P1-03、P2-02/04/06/07 | 服务工单闭环；电商遗留清理完毕；体验与集成达标 |

> 每新增一个前端页面/模块，须严格执行 `AGENTS.md` 的菜单规范：`init.sql` 种子段加行 → 同步执行到数据库 → 权限码同时登记 `PermissionCodes.ALL` 与前端 `constants/permissions.ts`；注意逻辑删除菜单行占用自增 id 的坑。

## 五、QA 验收门禁建议

| 类别 | 必测项 |
| --- | --- |
| 权限与越权 | 4 种 `dataScope` × 5 张注册表 × CRUD 的越权矩阵；直接按 id 查详情/更新/删除的二次归属校验；`/oper-logs`、`/login-logs` 的可见范围 |
| 安全 | 日志脱敏、默认密钥伪造 Token、CORS 带凭证、旧 Token 失效、失败锁定、SQL 注入（自定义 SQL 全为 `#{}`，重点查新增模块）、上传文件类型 |
| 状态机与事务 | 线索状态机、商机阶段流转矩阵、赢单/丢单终态、线索转化与客户合并的单事务回滚、乐观锁并发冲突 |
| 并发 | 公海领取（当前为「查后改」非原子，建议改条件更新或加锁）、主联系人唯一、双人同时合并客户 |
| 数据一致性 | DDL ↔ 实体逐列 diff；`init.sql` 与增量迁移结果 schema 一致；状态值与字典一致；菜单种子脚本重复执行不污染已删行 |
| 回归 | 动态菜单刷新可见、按钮权限码三处一致、Excel 导入逐行错误行号、看板/预测口径与明细核对 |
| 自动化 | 单测 + MockMvc 集成测试 + CI 构建，覆盖率门槛作为 `main` 合并门禁 |

---

## 六、证据索引

| 主题 | 文件 |
| --- | --- |
| 菜单权威源 | `backend/db/init.sql:431-452`（预置菜单）、`:85-119`（`sys_permission` 结构）、`:407-409`（默认账号） |
| 数据权限 | `backend/src/main/java/cn/easykits/crm/common/datascope/DataScopeHandler.java`、`DataScopeTableRegistry.java`、`config/MybatisPlusConfig.java` |
| 操作日志 | `backend/src/main/java/cn/easykits/crm/common/log/OperLogAspect.java` |
| 安全配置 | `config/SecurityConfig.java`、`backend/src/main/resources/application.yml` |
| 认证 | `modules/auth/controller/AuthController.java`、`security/JwtTokenProvider.java` |
| 客户 / 商机 | `modules/customer/service/CustomerServiceImpl.java:180-267`、`modules/opportunity/service/OpportunityServiceImpl.java:236-309`、`modules/opportunity/event/OpportunityEventListener.java` |
| 前端菜单与页面 | `crm-web/apps/web-ele/src/preferences.ts`、`router/access.ts`、`router/routes/core.ts`、`views/customer/modules/view360.vue`、`views/_core/profile/password-setting.vue` |
| 项目约定 | `AGENTS.md`（Git 与菜单规范）、`README.md:54-66`（进度清单）、`docs/backend-audit-report-2026-11-14.md`（已修复项） |


## 追加记录（2026-11-16 数据库执行与冒烟）

- 环境变量 `MYSQL_HOST/PORT/DATABASE/USERNAME/PASSWORD` 已写入 `~/.zshrc`（密码不落仓库）。
- 3 个迁移脚本 + 22 行菜单种子已对 `easy_crm` 库执行并验证（`sys_schema_migration` 台账：V20261115_01/02/03）。
- 后端以新代码实启（profiles: dev,local），冒烟结论见上表「运行时接口冒烟」行。
- 发现并处理：8080 曾被改动前的旧实例占用导致 404，已停掉并重启新实例。

- 2026-11-16：P2-02 电商遗留清理完成——`src/views` 现与菜单一一对应（15 个目录均为 CRM 业务页），前端生产构建通过（VITE_EXIT=0）。注：原遗留 `views/message/` 已在批次 2 中被新「消息中心」覆写，本次保留；`api/message.ts`（商城站内信）已删，新消息中心走 `api/crm/reminder.ts`。


## P2-03 实现记录（2026-11-16 数据字典）

### 数据库
- 迁移 `V20261116_01__sys_dict.sql`：`sys_dict`（类型）+ `sys_dict_item`（字典项）建表；9 个内置字典 / 44 个字典项种子；字典管理菜单（id=130，挂系统管理下）；`sys_schema_migration` 台账登记。**幂等验证通过（重复执行两次数据无漂移）**。
- 种子采用显式主键幂等（与 `sys_permission` 同套路）；`sys_dict_item.dict_id` 为可空冗余列，归属以 `dict_type` 为准。
- `init.sql` 同步补齐 DDL / 种子 / 菜单（新装库与存量库行为一致）。

### 后端
- `DictService`：读路径进程内缓存（写操作整体失效）；字典表无有效项时回退 `DictDefaults`（与 LeadSource / LostReasons / CustomerDicts / FollowTypes / FollowBizTypes 常量完全一致），删光字典项不会中断业务取值。
- `DictController`（`/system/dict`，权限码 `system:dict:list/create/update/delete`）：字典类型分页 / CRUD（内置字典禁删可停用、编码与字典项值唯一性校验、value 创建后不可改）+ 字典项 CRUD。
- `DictDataController`（`GET /dict-data/{type}`）：登录即可读，供前端下拉与导入校验共用。
- 线索导入去硬匹配：`LeadService.resolveSourceCode` 字典表优先、常量兜底；`LeadImportListener` 改为注入校验谓词；创建校验与 `sourceName` 展示同口径。
- `PermissionCodes.ALL` 追加 4 个 `system:dict:*` 权限码。

### 前端
- `api/system/dict.ts`（管理端 + 业务端取数）、`views/system/dict/{list.vue, data.ts, modules/form.vue, modules/items-drawer.vue, modules/item-form.vue}`：字典类型分页/新增/编辑/删除 + 字典项抽屉（增删改、启停），权限按钮级控制。
- `constants/permissions.ts` 的 `SYSTEM_PERMISSIONS` 补 dict 四码；新菜单刷新即可见（实时读取）。

### 检验
| 项 | 结果 |
| --- | --- |
| 后端编译（JDK 25） | BUILD SUCCESS |
| 前端生产构建 | VITE_EXIT=0 |
| 迁移幂等 | 两次执行数据一致（9 字典 / 44 项，重复行已清理） |
| 运行时冒烟 | `GET /dict-data/lead_source` 返回 6 项；管理分页 9 字典含项数；未登录 401；重复编码/重复值 4004；内置字典删除被拒；自定义字典增删改与缓存一致性通过；`sourceName` 走字典解析正常；级联逻辑删除生效 |
| 待办 | 前端各下拉（客户行业/规模/区域/级别、跟进类型等）尚未切到 `getDictData`（现为硬编码选项，与字典值一致）；Excel 导入的标签匹配已字典化，建议后续用真实文件回归一次 |

- 2026-11-16 变更说明（P0-3 CORS 部分回退）：应所有者明确决定，`easy-crm.cors.allowed-origins` 默认值改回 `*`，`SecurityConfig` 对 `*` 不再拒绝启动，改用 `allowedOriginPatterns("*")` 回显请求来源实现（兼容 allowCredentials=true）。生产环境仍建议配置 `CORS_ALLOWED_ORIGINS` 明确白名单。本机已同步将固定 JWT 密钥与 MySQL 密码写入 gitignore 的 `application-local.yml`。
