# CRM 后端代码审查报告（2026-11-14）

> 审查范围：轮次 1–5 产出的全部后端代码（基座/系统管理、线索、客户/联系人、商机、跟进/待办）。
> 取证方式：逐表 DDL↔实体字段 diff、构造器注入图、@Transactional 清单、数据权限 SQL 注入验证、分页 count 路径核对、控制器 Mapping 清点。

## 1. DDL 与实体字段一致性

**结论：7 张表逐列比对全部一一对应，无遗漏、无类型不一致。** BaseEntity 公共列由实体继承声明（设计）；`crm_opportunity_stage_log` 有意不继承 BaseEntity（日志表只插入不改）。

| 问题 | 修复建议 | 优先级 |
| --- | --- | --- |
| `crm_opportunity.amount` 无 >=0 后端校验（前端已限） | 服务层补 amount >= 0 校验 | 低 |
| `crm_lead.converted_customer_id` 无索引，按客户反查转化来源会全表扫 | 加普通索引 idx_converted_customer | 低 |

## 2. 跨模块循环依赖

**结论：Bean 依赖图有向无环，无循环依赖。** 依赖方向：LeadService → Customer/Contact/OpportunityService（Impl 仅依赖对方 Mapper）；security → MenuService → Mapper；RoleService → MenuService（单向）。

| 问题 | 修复建议 | 优先级 |
| --- | --- | --- |
| `CustomerServiceImpl.merge` 直接 UPDATE crm_contact / crm_lead（跨模块表写） | 跨表迁移收敛到各模块 Service（如 ContactService.migrateToCustomer），商机/跟进迁移落地时沿用 | 中 |

## 3. @Transactional 传播与回滚

**结论：全部写方法 `rollbackFor = Exception.class`（REQUIRED），线索转化 / 客户合并 / 商机赢单三个复合操作事务边界完整，事件均 AFTER_COMMIT。**

| 问题 | 修复建议 | 优先级 |
| --- | --- | --- |
| `merge` 中 source 的 updateById 未检查返回值——乐观锁冲突时联系人已迁移但 source 仍为「正常」，出现双活客户 | `if (!updateById(...)) throw DATA_CONFLICT` | **高** |
| 赢单路径 `customerMapper.updateById(customer)`（置成交客户）同样未检查 | 同上 | 中 |
| 线索转化「关联已有联系人」未校验 contact.customerId == customerId，可跨客户挂联系人 | `linkExisting(contactId, customerId)` 增加归属校验 | **高** |
| 管理端创建客户 ownerId 可为空但 status=正常（语义悬空） | ownerId 缺省取当前登录人或直接置公海态 | 中 |
| 跟进记录删除后其自动生成的未完成待办成孤儿 | 删除时级联取消（source=FOLLOW 且 biz 匹配） | 中 |
| 跟进更新按 biz_type+biz_id+source=FOLLOW 刷新所有未完成待办，会波及其他记录生成的待办 | crm_task 增加 follow_record_id 列，按记录维度刷新 | 中 |

## 4. 行级数据权限覆盖

**结论：已注册 crm_lead / crm_customer / crm_opportunity / crm_follow_record(owner+dept) / crm_task(owner)；未注册项均为合理豁免（联系人随客户、阶段配置全局、提醒私有、sys_* 走功能权限）。**

| 问题 | 修复建议 | 优先级 |
| --- | --- | --- |
| **时间线 stage_log 子查询越权**：SQL 中仅 follow/task 子查询被注入 owner 条件，`crm_opportunity_stage_log` 无过滤——SELF 用户可枚举他人商机完整阶段历史 | stage_log 子查询 JOIN crm_opportunity（已注册，别名自动注入条件） | **高** |
| 时间线查询他人 bizId 时静默返回空列表而非显式 403 | Service 层按 bizType 先校验 bizId 归属 | 中 |
| forecast 的 deptId 入参不校验用户部门归属（数据受 owner 条件二次过滤，但 COUNT/SUM 口径可能泄露） | 校验 deptId 或增加专属权限码 | 中 |

## 5. 分页 count 正确性

**结论：全部 MP 分页均为单表 Wrapper 查询，count 正常；两个自定义 SQL 均未走 MP 分页（forecast 不分页、timeline 自写 LIMIT + 同构 COUNT，实测无错位）。**

| 问题 | 修复建议 | 优先级 |
| --- | --- | --- |
| timeline 深分页 LIMIT offset 性能（CRM 量级可忽略） | 后续可改「游标分页（按 time+id）」 | 低 |
| 未来若给聚合 SQL 加 MP 分页，GROUP BY 会使 count 退化为包裹子查询（正确但慢） | 保持聚合不分页 | 低 |

## 6. 硬编码字典值清单（待迁字典表）

| 常量类 | 内容 | 迁移优先级 |
| --- | --- | --- |
| LeadSource（官网/电话/广告投放/转介绍/展会/其他） | Excel 导入按中文标签匹配，耦合最深 | **中（最先迁）** |
| LostReasons（价格/功能/竞对/预算/失联/其他） | 丢单必填校验 | 中 |
| CustomerDicts（行业/规模/区域/级别/状态） | 客户表单与 VO 翻译 | 中 |
| FollowTypes（电话/拜访/邮件/微信/其他） | 跟进表单与时间线 | 中 |
| FollowBizTypes（lead/customer/opportunity/order） | 多态关联码 | **低——建议保留常量**（关联码应随代码枚举） |
| PermissionCodes.ALL、任务 source、状态枚举 | 权限码/来源/状态 | 低（随代码维护） |
| 停滞阈值 7/14/30 天 | OpportunityServiceImpl 常量 | 低（建议挪 yml） |

## 7. N+1 查询风险

| 问题 | 修复建议 | 优先级 |
| --- | --- | --- |
| **商机 pageQuery**：toVO 逐行 stageMapper.selectOne 查阶段名（pageSize=20 → 21 次查询） | 构建 stageCode→Stage Map 一次批量传入 toVO | **高** |
| **商机 board**：每个卡片同样逐卡查阶段 | 同上（board 已有全量 stages，直接建 Map） | **高** |
| loadAuthorities 每请求 3~4 次权限表查询 | 本地缓存 30~60s（权限变更时清除） | 中 |
| 其余批量名称填充（owner/dept/customer/contact）均已批量，无 N+1 | — | — |

## 8. API 清单

见对话输出（与本报告同轮产出），共 5 组 60+ 接口：认证/用户信息、系统管理、线索、客户与联系人、商机、跟进与待办，均带权限码与出入参说明。

## 处理建议汇总

| 批次 | 内容 | 优先级 |
| --- | --- | --- |
| fix 批次 1 | timeline stage_log 越权 + 转化联系人归属校验 | 高 |
| fix 批次 2 | updateById 返回值检查（3 处）+ 管理端建客户默认负责人 + 孤儿待办清理 | 中 |
| perf 批次 3 | 商机列表/看板阶段名 N+1 消除 | 高 |
| refactor 批次 4 | 合并跨表迁移收敛 + 字典表迁移（LeadSource 先行） | 中 |
