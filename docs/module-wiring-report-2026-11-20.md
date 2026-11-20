# CRM 模块串联核查报告（2026-11-20）

> 核查方式：① 数据库菜单 ↔ 前端页面文件 ↔ 前端 API 路径 ↔ 后端 Controller 端点 四层比对；
> ② 30+ 个页面在浏览器逐页打开验证渲染；③ 37 个读接口 + 端到端业务链路实跑（真实 HTTP + 数据库核对）。

## 一、结论

**主干链路已全部串联，31 个页面、37 个读接口全部可用；发现并修复 1 处真实断点（跟进记录菜单漂移），另有 4 处非阻断型缺口。**

| 层 | 核查结果 |
| --- | --- |
| 菜单 → 页面 | 43 条非按钮菜单 = 12 个目录 + 31 个页面；31 个页面文件全部存在 ✅ |
| 页面 → API | 前端 16 个 API 模块路径与后端端点一一对应 ✅ |
| API → 权限码 | Controller 使用 141 个权限码，`PermissionCodes.ALL` 登记 141 个，**零缺失**（ADMIN 兜底有效）✅ |
| 页面渲染 | 31 个菜单页面浏览器实测，全部 `#app` 正常挂载、无 Vite 错误覆盖层 ✅ |
| 后端接口 | 37 个列表/读接口全部 200 ✅ |

## 二、端到端链路实跑结果

| 链路 | 实测结果 |
| --- | --- |
| 线索 → 转化 → 客户 + 联系人 + 商机 | ✅ 一次调用生成 3 个实体并回写 `converted_*` |
| 商机赢单 → 自动建单 → 首期回款计划 | ✅ 新增订单草稿（SO-2026…）+ 1 行明细 + 1 期回款计划，幂等键 opportunityId |
| 跟进记录 → 自动待办 | ✅ 生成 `source=FOLLOW` 任务，携带 follow_record_id |
| 跟进记录 → 业务对象活动时间 | ✅ 客户/商机 last_follow_time / last_activity_at 随跟进刷新 |
| 时间线聚合 | ✅ 商机时间线返回 task + follow + stage_log 三类，倒序分页正确 |
| 客户 360 | ✅ 基本信息 + 联系人 + 商机 + 跟进四段均有数据 |
| 提醒 → 消息页 | ✅ `/reminders/unread-count` 被顶栏消息入口消费 |
| 字典 | ✅ 9 类字典（lead_source / follow_type / lost_reason / customer_*）已建表，`/dict-data/{type}` 可读；线索导入按字典 label 匹配、常量兜底 |
| 报表 | ✅ 达成/漏斗/排行/营收/客户分布 5 个接口正常，达成率关联 `sales_target` |

## 三、发现的问题

### P1（已修复）跟进记录菜单漂移，页面不可达
- 现象：`/follow/list` 页面文件、后端接口、权限码都存在，但侧边栏无入口、路由树无该节点。
- 根因：库中 `sys_permission` id=20/21 两行 `path` / `component` 为 NULL（早期种子执行时该两行已存在，而种子 `ON DUPLICATE KEY UPDATE` 只更新 `title/status`，未同步 path/component/pid）。
- 修复：修正库中两行（pid=0/20、path、component、auth_code、icon）；种子脚本的 `ON DUPLICATE` 扩展为同步 `pid/name/type/auth_code/path/component/icon/order_no/status`，避免同类漂移复发。
- 验证：`/system/menu/routes` 已出现「跟进管理 → 跟进记录」，页面渲染 3 条记录、写跟进/编辑/删除按钮正常。

### P2 审批中心只读
- 现状：仅 `GET /approvals`、`/summary`、`/{id}`，无提交/通过/驳回接口；业务单据（合同/退款/折扣）创建时不会自动生成审批实例。
- 影响：审批中心目前是「看板展示」，不能真正驱动审批流。
- 建议：补齐 `POST /approvals`（或由业务服务在提交时建实例）+ `PUT /{id}/approve|reject`，并在合同/退款状态机中挂接入参。

### P2 报价单 → 订单无字段关联
- 现状：`crm_quote.opportunity_id` 有值，但 `crm_order` 只有 `customer_id / opportunity_id / contract_id`，没有 `quote_id`；报价无法一键转订单。
- 影响：报价成交后需手工重录订单金额/明细。
- 建议：`crm_order` 增加 `quote_id`，订单创建支持从报价带出明细（或提供 `/quotes/{id}/to-order`）。

### P3 前端字典未接 `/dict-data`
- 现状：后端已字典化（9 类），但线索/客户/跟进页下拉仍用 `src/api/crm/*.ts` 内的硬编码常量；后台改字典不影响 UI。
- 建议：统一封装 `useDict('lead_source')` 并替换硬编码常量，保留常量作为 fallback。

### P3 时间线未覆盖交易/服务域
- 现状：时间线聚合 follow + task + 商机阶段日志；订单、合同、工单的状态变更不入时间线（Order 无 `last_activity_at` 字段）。
- 影响：客户 360 / 商机时间线看不到「赢单后建单、回款、开票」等后续动作。
- 建议：订单状态变更写 `crm_activity_log`（统一动态表），时间线 UNION 增加该来源；或给 Order 增加 last_activity_at 并复用 stage_log 模式。

### P4 过时注释误导
- `OpportunityEventListener` 仍写「TODO 轮次 5 接订单，当前仅打日志」，实际建单由 `modules/order/listener/OpportunityWonOrderListener` 完成。建议删除该 TODO 并注明分工。

### 数据缺口（非代码问题）
- `sales_target` 表为空 → 报表「达成率」无目标基线；`sla_policies` 有 20 条、`approvals` 2 条为演示数据。

## 四、校验脚本复用

- 后端全链路：`/tmp/crm_e2e.py`（登录 → 37 接口 → 转化 → 赢单 → 建单 → 回款计划 → 跟进 → 待办 → 时间线 → 360）
- 菜单漂移比对：种子 `component` 集合 vs `sys_permission.component` 集合
- 权限码比对：`@PreAuthorize` 码集合 vs `PermissionCodes.ALL`
