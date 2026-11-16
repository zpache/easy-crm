/**
 * 批次 3 新增权限码（本地文件，不修改共享 constants/permissions.ts）。
 *
 * 整合方式二选一（见 INTEGRATION.md）：
 *   A. 把下列常量合入 src/constants/permissions.ts 并删除本文件，页面改从共享文件导入；
 *   B. 保留本文件，页面继续从 '#/constants/crm-batch3-permissions' 导入。
 * 无论哪种，都必须同时把字符串登记进后端 PermissionCodes.ALL 与 sys_permission.auth_code。
 */

export const PRODUCT_PERMISSIONS = {
  create: 'crm:product:create',
  delete: 'crm:product:delete',
  list: 'crm:product:list',
  update: 'crm:product:update',
} as const;

export const PRICE_PERMISSIONS = {
  create: 'crm:price:create',
  delete: 'crm:price:delete',
  list: 'crm:price:list',
  update: 'crm:price:update',
} as const;

export const OPP_ITEM_PERMISSIONS = {
  create: 'crm:opp-item:create',
  delete: 'crm:opp-item:delete',
  list: 'crm:opp-item:list',
  update: 'crm:opp-item:update',
} as const;

export const QUOTE_PERMISSIONS = {
  create: 'crm:quote:create',
  delete: 'crm:quote:delete',
  list: 'crm:quote:list',
  update: 'crm:quote:update',
} as const;

export const CONTRACT_PERMISSIONS = {
  create: 'crm:contract:create',
  delete: 'crm:contract:delete',
  list: 'crm:contract:list',
  update: 'crm:contract:update',
} as const;

/** 订单权限（CRM 交易域） */
export const CRM_ORDER_PERMISSIONS = {
  create: 'crm:order:create',
  delete: 'crm:order:delete',
  list: 'crm:order:list',
  status: 'crm:order:status',
  update: 'crm:order:update',
} as const;

export const PAYMENT_PLAN_PERMISSIONS = {
  create: 'crm:payment-plan:create',
  delete: 'crm:payment-plan:delete',
  list: 'crm:payment-plan:list',
  update: 'crm:payment-plan:update',
} as const;

export const PAYMENT_PERMISSIONS = {
  create: 'crm:payment:create',
  delete: 'crm:payment:delete',
  list: 'crm:payment:list',
  update: 'crm:payment:update',
  writeoff: 'crm:payment:writeoff',
} as const;

export const INVOICE_PERMISSIONS = {
  create: 'crm:invoice:create',
  delete: 'crm:invoice:delete',
  list: 'crm:invoice:list',
  update: 'crm:invoice:update',
} as const;

export const REFUND_PERMISSIONS = {
  create: 'crm:refund:create',
  delete: 'crm:refund:delete',
  list: 'crm:refund:list',
  update: 'crm:refund:update',
} as const;

export const EXPORT_PERMISSIONS = {
  contact: 'crm:export:contact',
  customer: 'crm:export:customer',
  lead: 'crm:export:lead',
  opportunity: 'crm:export:opportunity',
  order: 'crm:export:order',
  template: 'crm:export:template',
} as const;

export const SALES_TARGET_PERMISSIONS = {
  create: 'crm:sales-target:create',
  delete: 'crm:sales-target:delete',
  list: 'crm:sales-target:list',
  update: 'crm:sales-target:update',
} as const;

export const REPORT_PERMISSIONS = {
  achievement: 'crm:report:achievement',
} as const;

/** 营销域（活动管理） */
export const CAMPAIGN_PERMISSIONS = {
  create: 'crm:campaign:create',
  delete: 'crm:campaign:delete',
  list: 'crm:campaign:list',
  update: 'crm:campaign:update',
} as const;

/** 报表中心扩展（漏斗 / 排行 / 回款 / 客户分布） */
export const REPORT_EXT_PERMISSIONS = {
  customer: 'crm:report:customer',
  funnel: 'crm:report:funnel',
  ranking: 'crm:report:ranking',
  revenue: 'crm:report:revenue',
} as const;

/** 审批中心（外部审批镜像，只读入口） */
export const APPROVAL_PERMISSIONS = {
  list: 'crm:approval:list',
} as const;
