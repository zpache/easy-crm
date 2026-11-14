import { requestClient } from '#/api/request';

/** 客户服务域：工单 / SLA 策略 / 知识库 / 满意度 */

export namespace TicketApi {
  /** 服务工单（列表 / 详情共用） */
  export interface Ticket {
    [key: string]: any;
    actualResolveTime?: string;
    actualResponseTime?: string;
    contactId?: null | number;
    contactName?: string;
    createTime?: string;
    customerId: number;
    customerName?: string;
    deptId?: null | number;
    description?: string;
    id: number;
    orderId?: null | number;
    overdue?: boolean;
    overdueFlag?: number;
    ownerId?: null | number;
    ownerName?: string;
    priority: string;
    priorityName?: string;
    slaDueResolveTime?: string;
    slaDueResponseTime?: string;
    source?: string;
    sourceName?: string;
    status: string;
    statusName?: string;
    ticketNo: string;
    ticketType: string;
    ticketTypeName?: string;
    title: string;
    updateTime?: string;
    version?: number;
  }

  /** 创建 / 编辑表单 */
  export interface TicketPayload {
    contactId?: null | number;
    customerId: number;
    description?: string;
    orderId?: null | number;
    priority: string;
    source?: string;
    ticketType: string;
    title: string;
  }

  /** 状态流转 */
  export interface StatusPayload {
    remark?: string;
    targetStatus: string;
  }

  /** 派单 */
  export interface AssignPayload {
    ownerId: number;
    remark?: string;
  }

  /** 关闭 */
  export interface ClosePayload {
    remark?: string;
  }

  /** 流转日志 */
  export interface TicketLog {
    fromStatus?: string;
    fromStatusName?: string;
    id: number;
    operateTime?: string;
    operatorId?: null | number;
    operatorName?: string;
    remark?: string;
    ticketId: number;
    toStatus: string;
    toStatusName?: string;
  }

  /** 分页结果 */
  export interface PageResult<T> {
    pageNum: number;
    pageSize: number;
    pages: number;
    records: T[];
    total: number;
  }
}

export namespace SlaApi {
  export interface SlaPolicy {
    [key: string]: any;
    createTime?: string;
    enabled: number;
    id: number;
    priority: string;
    priorityName?: string;
    resolveHours: number;
    responseHours: number;
    ticketType: string;
    ticketTypeName?: string;
    updateTime?: string;
  }

  export interface SlaPolicyPayload {
    enabled?: number;
    priority: string;
    resolveHours: number;
    responseHours: number;
    ticketType: string;
  }
}

export namespace KnowledgeApi {
  export interface Knowledge {
    [key: string]: any;
    category: string;
    content?: string;
    createTime?: string;
    id: number;
    publishStatus: string;
    publishStatusName?: string;
    tags?: string;
    title: string;
    updateTime?: string;
    viewCount: number;
  }

  export interface KnowledgePayload {
    category: string;
    content?: string;
    publishStatus: string;
    tags?: string;
    title: string;
  }
}

export namespace SatisfactionApi {
  export interface Satisfaction {
    [key: string]: any;
    content?: string;
    createTime?: string;
    evaluateTime?: string;
    id: number;
    ownerId?: null | number;
    ownerName?: string;
    score: number;
    ticketId: number;
    ticketNo?: string;
  }

  export interface ScoreCount {
    cnt: number;
    score: number;
  }

  export interface Stats {
    averageScore: number;
    distribution: ScoreCount[];
    total: number;
  }

  export interface SatisfactionPayload {
    content?: string;
    score: number;
    ticketId: number;
  }
}

interface PageResult<T> {
  pageNum: number;
  pageSize: number;
  pages: number;
  records: T[];
  total: number;
}

/** 工单状态字典（type 用于 CellTag 颜色；与后端 TicketStatus 一致） */
export const TICKET_STATUSES = [
  { label: '待受理', type: 'warning', value: 'PENDING' },
  { label: '已派单', type: 'primary', value: 'ASSIGNED' },
  { label: '处理中', type: 'primary', value: 'PROCESSING' },
  { label: '待客户确认', type: 'warning', value: 'WAITING_CONFIRM' },
  { label: '已完成', type: 'success', value: 'COMPLETED' },
  { label: '已关闭', type: 'info', value: 'CLOSED' },
] as const;

/** 终态（不可再变更） */
export const TICKET_TERMINAL_STATUSES = ['COMPLETED', 'CLOSED'] as const;

/** 合法状态转移（与后端 TicketStatus.canTransitionTo 保持一致） */
export const TICKET_TRANSITIONS: Record<string, string[]> = {
  ASSIGNED: ['PROCESSING', 'CLOSED'],
  CLOSED: [],
  COMPLETED: [],
  PENDING: ['ASSIGNED', 'CLOSED'],
  PROCESSING: ['WAITING_CONFIRM', 'CLOSED'],
  WAITING_CONFIRM: ['COMPLETED', 'PROCESSING', 'CLOSED'],
};

export const TICKET_TYPES = [
  { label: '咨询', value: 'CONSULT' },
  { label: '投诉', value: 'COMPLAINT' },
  { label: '报修', value: 'REPAIR' },
  { label: '退换', value: 'RETURN' },
  { label: '其他', value: 'OTHER' },
] as const;

export const TICKET_PRIORITIES = [
  { label: '低', value: 'LOW' },
  { label: '中', value: 'MEDIUM' },
  { label: '高', value: 'HIGH' },
  { label: '紧急', value: 'URGENT' },
] as const;

export const TICKET_SOURCES = [
  { label: '门户', value: 'PORTAL' },
  { label: '电话', value: 'PHONE' },
  { label: '邮件', value: 'EMAIL' },
  { label: '微信', value: 'WECHAT' },
  { label: '手动录入', value: 'MANUAL' },
  { label: '其他', value: 'OTHER' },
] as const;

export const KNOWLEDGE_STATUSES = [
  { label: '草稿', type: 'info', value: 'DRAFT' },
  { label: '已发布', type: 'success', value: 'PUBLISHED' },
  { label: '已下线', type: 'warning', value: 'OFFLINE' },
] as const;

export const SATISFACTION_SCORES = [
  { label: '1 分', value: 1 },
  { label: '2 分', value: 2 },
  { label: '3 分', value: 3 },
  { label: '4 分', value: 4 },
  { label: '5 分', value: 5 },
] as const;

/**
 * 客户服务域权限码。
 * 注意：这些条目需同步合入共享文件 src/constants/permissions.ts 与后端
 * PermissionCodes.ALL（见 INTEGRATION.md）；本对象供本次新增页面直接引用，
 * 避免改动项目内既有共享文件。
 */
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

export function statusLabel(code?: string) {
  return TICKET_STATUSES.find((s) => s.value === code)?.label ?? code ?? '-';
}

export function typeLabel(code?: string) {
  return TICKET_TYPES.find((s) => s.value === code)?.label ?? code ?? '-';
}

export function priorityLabel(code?: string) {
  return TICKET_PRIORITIES.find((s) => s.value === code)?.label ?? code ?? '-';
}

export function sourceLabel(code?: string) {
  return TICKET_SOURCES.find((s) => s.value === code)?.label ?? code ?? '-';
}

export function knowledgeStatusLabel(code?: string) {
  return KNOWLEDGE_STATUSES.find((s) => s.value === code)?.label ?? code ?? '-';
}

// ============================================================ 工单

async function getTicketList(params: Recordable<any>) {
  const result = await requestClient.get<PageResult<TicketApi.Ticket>>('/tickets', {
    params,
  });
  return { items: result.records, total: result.total };
}

async function getTicketDetail(id: number) {
  return requestClient.get<TicketApi.Ticket>(`/tickets/${id}`);
}

async function getTicketLogs(id: number) {
  return requestClient.get<TicketApi.TicketLog[]>(`/tickets/${id}/logs`);
}

async function createTicket(data: TicketApi.TicketPayload) {
  return requestClient.post<{ id: number }>('/tickets', data);
}

async function updateTicket(id: number, data: TicketApi.TicketPayload) {
  return requestClient.put(`/tickets/${id}`, data);
}

async function changeTicketStatus(id: number, data: TicketApi.StatusPayload) {
  return requestClient.put(`/tickets/${id}/status`, data);
}

async function assignTicket(id: number, data: TicketApi.AssignPayload) {
  return requestClient.post(`/tickets/${id}/assign`, data);
}

async function closeTicket(id: number, data: TicketApi.ClosePayload) {
  return requestClient.post(`/tickets/${id}/close`, data);
}

// ============================================================ SLA 策略

async function getSlaPolicyList(params: Recordable<any>) {
  const result = await requestClient.get<PageResult<SlaApi.SlaPolicy>>(
    '/sla-policies',
    { params },
  );
  return { items: result.records, total: result.total };
}

async function getSlaPolicyDetail(id: number) {
  return requestClient.get<SlaApi.SlaPolicy>(`/sla-policies/${id}`);
}

async function createSlaPolicy(data: SlaApi.SlaPolicyPayload) {
  return requestClient.post<{ id: number }>('/sla-policies', data);
}

async function updateSlaPolicy(id: number, data: SlaApi.SlaPolicyPayload) {
  return requestClient.put(`/sla-policies/${id}`, data);
}

async function deleteSlaPolicy(id: number) {
  return requestClient.delete(`/sla-policies/${id}`);
}

// ============================================================ 知识库

async function getKnowledgeList(params: Recordable<any>) {
  const result = await requestClient.get<PageResult<KnowledgeApi.Knowledge>>(
    '/knowledge',
    { params },
  );
  return { items: result.records, total: result.total };
}

async function getKnowledgeDetail(id: number) {
  return requestClient.get<KnowledgeApi.Knowledge>(`/knowledge/${id}`);
}

async function createKnowledge(data: KnowledgeApi.KnowledgePayload) {
  return requestClient.post<{ id: number }>('/knowledge', data);
}

async function updateKnowledge(id: number, data: KnowledgeApi.KnowledgePayload) {
  return requestClient.put(`/knowledge/${id}`, data);
}

async function deleteKnowledge(id: number) {
  return requestClient.delete(`/knowledge/${id}`);
}

async function viewKnowledge(id: number) {
  return requestClient.post(`/knowledge/${id}/view`);
}

// ============================================================ 满意度

async function getSatisfactionList(params: Recordable<any>) {
  const result = await requestClient.get<
    PageResult<SatisfactionApi.Satisfaction>
  >('/satisfactions', { params });
  return { items: result.records, total: result.total };
}

async function getSatisfactionStats(params: Recordable<any>) {
  return requestClient.get<SatisfactionApi.Stats>('/satisfactions/stats', {
    params,
  });
}

async function createSatisfaction(data: SatisfactionApi.SatisfactionPayload) {
  return requestClient.post<{ id: number }>('/satisfactions', data);
}

export {
  assignTicket,
  changeTicketStatus,
  closeTicket,
  createKnowledge,
  createSatisfaction,
  createSlaPolicy,
  createTicket,
  deleteKnowledge,
  deleteSlaPolicy,
  getKnowledgeDetail,
  getKnowledgeList,
  getSatisfactionList,
  getSatisfactionStats,
  getSlaPolicyDetail,
  getSlaPolicyList,
  getTicketDetail,
  getTicketList,
  getTicketLogs,
  updateKnowledge,
  updateSlaPolicy,
  updateTicket,
  viewKnowledge,
};
