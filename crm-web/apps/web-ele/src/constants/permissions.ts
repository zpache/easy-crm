/** 管理端权限码：必须与后端 @PreAuthorize / sys_menu.auth_code 保持一致 */

export const DASHBOARD_PERMISSIONS = {
  analytics: 'dashboard:analytics',
} as const;

export const GOODS_PERMISSIONS = {
  brandCreate: 'goods:brand:create',
  brandDelete: 'goods:brand:delete',
  brandList: 'goods:brand:list',
  brandStatus: 'goods:brand:status',
  brandUpdate: 'goods:brand:update',
  categoryCreate: 'goods:category:create',
  categoryDelete: 'goods:category:delete',
  categoryList: 'goods:category:list',
  categoryUpdate: 'goods:category:update',
  goodsCreate: 'goods:goods:create',
  goodsDelete: 'goods:goods:delete',
  goodsList: 'goods:goods:list',
  goodsSku: 'goods:goods:sku',
  goodsStatus: 'goods:goods:status',
  goodsUpdate: 'goods:goods:update',
  stockAdjust: 'goods:stock:adjust',
  stockImport: 'goods:stock:import',
  stockList: 'goods:stock:list',
  stockLogs: 'goods:stock:logs',
} as const;

export const ORDER_PERMISSIONS = {
  batchShip: 'order:order:batch-ship',
  close: 'order:order:close',
  commentAudit: 'order:comment:audit',
  commentDelete: 'order:comment:delete',
  commentList: 'order:comment:list',
  commentReply: 'order:comment:reply',
  commentVisible: 'order:comment:visible',
  deliver: 'order:order:deliver',
  detail: 'order:order:detail',
  export: 'order:order:export',
  list: 'order:order:list',
  logistics: 'order:order:logistics',
  refundAudit: 'order:refund:audit',
  refundConfirm: 'order:refund:confirm',
  refundList: 'order:refund:list',
  ship: 'order:order:ship',
} as const;

export const MEMBER_PERMISSIONS = {
  inviteList: 'member:invite:list',
  levelCreate: 'member:level:create',
  levelDelete: 'member:level:delete',
  levelDiscount: 'member:level:discount',
  levelList: 'member:level:list',
  levelStatus: 'member:level:status',
  levelUpdate: 'member:level:update',
  list: 'member:member:list',
  memberPoints: 'member:member:points',
  memberTag: 'member:member:tag',
  pointsList: 'member:points:list',
  pointsUpdate: 'member:points:update',
  tagCreate: 'member:tag:create',
  tagDelete: 'member:tag:delete',
  tagList: 'member:tag:list',
  tagUpdate: 'member:tag:update',
} as const;

export const MARKETING_PERMISSIONS = {
  couponCreate: 'marketing:coupon:create',
  couponDelete: 'marketing:coupon:delete',
  couponGrant: 'marketing:coupon:grant',
  couponList: 'marketing:coupon:list',
  couponRecords: 'marketing:coupon:records',
  couponStatus: 'marketing:coupon:status',
  couponUpdate: 'marketing:coupon:update',
  promotionCreate: 'marketing:promotion:create',
  promotionDelete: 'marketing:promotion:delete',
  promotionList: 'marketing:promotion:list',
  promotionStatus: 'marketing:promotion:status',
  promotionUpdate: 'marketing:promotion:update',
  reductionCreate: 'marketing:reduction:create',
  reductionDelete: 'marketing:reduction:delete',
  reductionList: 'marketing:reduction:list',
  reductionStatus: 'marketing:reduction:status',
  reductionUpdate: 'marketing:reduction:update',
  signinCreate: 'marketing:signin:create',
  signinDelete: 'marketing:signin:delete',
  signinList: 'marketing:signin:list',
  signinLogs: 'marketing:signin:logs',
  signinStatus: 'marketing:signin:status',
  signinUpdate: 'marketing:signin:update',
} as const;

export const DECORATE_PERMISSIONS = {
  materialDelete: 'decorate:material:delete',
  materialList: 'decorate:material:list',
  materialUpdate: 'decorate:material:update',
  materialUpload: 'decorate:material:upload',
  navList: 'decorate:nav:list',
  navPublish: 'decorate:nav:publish',
  navReset: 'decorate:nav:reset',
  navUpdate: 'decorate:nav:update',
  pageCopy: 'decorate:page:copy',
  pageCreate: 'decorate:page:create',
  pageDelete: 'decorate:page:delete',
  pageHome: 'decorate:page:home',
  pageList: 'decorate:page:list',
  pagePublish: 'decorate:page:publish',
  pageUpdate: 'decorate:page:update',
  themeActivate: 'decorate:theme:activate',
  themeCreate: 'decorate:theme:create',
  themeDelete: 'decorate:theme:delete',
  themeList: 'decorate:theme:list',
  themeUpdate: 'decorate:theme:update',
  upload: 'file:upload',
} as const;

export const SETTING_PERMISSIONS = {
  deliveryCreate: 'settings:delivery:create',
  deliveryDelete: 'settings:delivery:delete',
  deliveryList: 'settings:delivery:list',
  deliveryUpdate: 'settings:delivery:update',
  freightList: 'trade:freight:list',
  freightUpdate: 'trade:freight:update',
  logisticsList: 'settings:logistics:list',
  logisticsUpdate: 'settings:logistics:update',
  notifyList: 'settings:notify:list',
  notifyUpdate: 'settings:notify:update',
  paymentCheck: 'settings:payment:check',
  paymentList: 'settings:payment:list',
  paymentUpdate: 'settings:payment:update',
} as const;

export const SYSTEM_PERMISSIONS = {
  deptCreate: 'system:dept:create',
  deptDelete: 'system:dept:delete',
  deptList: 'system:dept:list',
  deptUpdate: 'system:dept:update',
  loginLogList: 'system:loginlog:list',
  menuCreate: 'system:menu:create',
  menuDelete: 'system:menu:delete',
  menuList: 'system:menu:list',
  menuUpdate: 'system:menu:update',
  messageList: 'system:message:list',
  messageRead: 'system:message:read',
  messageReadAll: 'system:message:read-all',
  operLogList: 'system:operlog:list',
  roleCreate: 'system:role:create',
  roleDelete: 'system:role:delete',
  roleList: 'system:role:list',
  roleStatus: 'system:role:status',
  roleUpdate: 'system:role:update',
  userCreate: 'system:user:create',
  userDelete: 'system:user:delete',
  userList: 'system:user:list',
  userResetPassword: 'system:user:reset-password',
  userStatus: 'system:user:status',
  userUpdate: 'system:user:update',
} as const;

export const CRM_PERMISSIONS = {
  leadClaim: 'crm:lead:claim',
  leadConvert: 'crm:lead:convert',
  leadCreate: 'crm:lead:create',
  leadImport: 'crm:lead:import',
  leadList: 'crm:lead:list',
  leadToPool: 'crm:lead:to-pool',
  leadUpdate: 'crm:lead:update',
} as const;

export const CUSTOMER_PERMISSIONS = {
  claim: 'crm:customer:claim',
  create: 'crm:customer:create',
  list: 'crm:customer:list',
  merge: 'crm:customer:merge',
  toPool: 'crm:customer:to-pool',
  update: 'crm:customer:update',
} as const;

export const CONTACT_PERMISSIONS = {
  create: 'crm:contact:create',
  delete: 'crm:contact:delete',
  list: 'crm:contact:list',
  primary: 'crm:contact:primary',
  update: 'crm:contact:update',
} as const;

export const OPPORTUNITY_PERMISSIONS = {
  create: 'crm:opportunity:create',
  forecast: 'crm:opportunity:forecast',
  list: 'crm:opportunity:list',
  stage: 'crm:opportunity:stage',
  stageManage: 'crm:opportunity:stage-manage',
  update: 'crm:opportunity:update',
} as const;

export const TASK_PERMISSIONS = {
  complete: 'crm:task:complete',
  create: 'crm:task:create',
  list: 'crm:task:list',
  update: 'crm:task:update',
} as const;

/** 批次 2：分配转派与消息提醒 */
export const ASSIGN_PERMISSIONS = {
  customer: 'crm:customer:assign',
  lead: 'crm:lead:assign',
  opportunity: 'crm:opportunity:assign',
} as const;

export const REMINDER_PERMISSIONS = {
  list: 'crm:reminder:list',
  read: 'crm:reminder:read',
} as const;

/** 批次 4：客户服务（与 api/crm/support.ts 中的导出保持一致） */
export const SUPPORT_PERMISSIONS = {
  knowledgeCreate: 'crm:knowledge:create',
  knowledgeDelete: 'crm:knowledge:delete',
  knowledgeList: 'crm:knowledge:list',
  knowledgeUpdate: 'crm:knowledge:update',
  satisfactionCreate: 'crm:satisfaction:create',
  satisfactionList: 'crm:satisfaction:list',
  slaCreate: 'crm:sla:create',
  slaDelete: 'crm:sla:delete',
  slaList: 'crm:sla:list',
  slaUpdate: 'crm:sla:update',
  ticketAssign: 'crm:ticket:assign',
  ticketClose: 'crm:ticket:close',
  ticketCreate: 'crm:ticket:create',
  ticketList: 'crm:ticket:list',
  ticketStatus: 'crm:ticket:status',
  ticketUpdate: 'crm:ticket:update',
} as const;

/** 批次 3：产品 / 交易 / 导出 / 报表权限常量 */
export * from './crm-batch3-permissions';
