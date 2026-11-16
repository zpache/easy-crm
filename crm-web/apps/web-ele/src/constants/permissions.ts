/** 管理端权限码：必须与后端 @PreAuthorize / sys_menu.auth_code 保持一致 */

export const DASHBOARD_PERMISSIONS = {
  analytics: 'dashboard:analytics',
} as const;

export const SYSTEM_PERMISSIONS = {
  deptCreate: 'system:dept:create',
  deptDelete: 'system:dept:delete',
  deptList: 'system:dept:list',
  deptUpdate: 'system:dept:update',
  dictCreate: 'system:dict:create',
  dictDelete: 'system:dict:delete',
  dictList: 'system:dict:list',
  dictUpdate: 'system:dict:update',
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
