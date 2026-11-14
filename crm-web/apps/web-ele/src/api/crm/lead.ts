import { requestClient } from '#/api/request';

export namespace LeadApi {
  /** 线索记录 */
  export interface Lead {
    [key: string]: any;
    companyDuplicate?: boolean;
    contactName?: string;
    convertedContactId?: null | number;
    convertedCustomerId?: null | number;
    convertedOpportunityId?: null | number;
    createTime?: string;
    deptId?: null | number;
    deptName?: string;
    duplicates?: Array<{ id: number; name: string }>;
    email?: string;
    id: number;
    lastFollowTime?: string;
    name: string;
    ownerId?: null | number;
    ownerName?: string;
    phone?: string;
    remark?: string;
    source?: null | number;
    sourceName?: string;
    status: 0 | 1 | 2 | 3;
    statusName?: string;
    version?: number;
  }

  /** 创建 / 编辑表单 */
  export interface LeadPayload {
    contactName?: string;
    email?: string;
    name: string;
    phone?: string;
    remark?: string;
    source?: null | number;
    status?: number;
  }

  /** 转化表单 */
  export interface ConvertPayload {
    contactId?: null | number;
    contactInfo?: {
      email?: string;
      name?: string;
      phone?: string;
      position?: string;
    };
    createContact?: boolean;
    createCustomer?: boolean;
    customerId?: null | number;
    customerInfo?: {
      address?: string;
      email?: string;
      name?: string;
      phone?: string;
    };
    opportunityName?: string;
  }

  /** 转化结果 */
  export interface ConvertResult {
    contactId?: null | number;
    customerId?: null | number;
    opportunityId: number;
  }

  /** 导入结果 */
  export interface ImportResult {
    errors: Array<{ message: string; row: number }>;
    failCount: number;
    successCount: number;
    total: number;
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

/** 线索来源字典（与后端 LeadSource 常量类保持一致） */
export const LEAD_SOURCES = [
  { label: '官网', value: 1 },
  { label: '电话', value: 2 },
  { label: '广告投放', value: 3 },
  { label: '客户转介绍', value: 4 },
  { label: '展会活动', value: 5 },
  { label: '其他', value: 6 },
] as const;

/** 线索状态字典 */
export const LEAD_STATUSES = [
  { label: '新建', value: 0 },
  { label: '跟进中', value: 1 },
  { label: '已验证', value: 2 },
  { label: '不合格', value: 3 },
] as const;

/**
 * 线索分页列表
 */
async function getLeadList(params: Recordable<any>) {
  const result = await requestClient.get<LeadApi.PageResult<any>>(
    '/leads',
    { params },
  );
  return { items: result.records, total: result.total };
}

/**
 * 线索详情
 */
async function getLeadDetail(id: number) {
  return requestClient.get<LeadApi.Lead>(`/leads/${id}`);
}

async function createLead(data: LeadApi.LeadPayload) {
  return requestClient.post<LeadApi.Lead>('/leads', data);
}

async function updateLead(id: number, data: LeadApi.LeadPayload) {
  return requestClient.put<LeadApi.Lead>(`/leads/${id}`, data);
}

/**
 * 线索转化，返回商机 ID
 */
async function convertLead(id: number, data: LeadApi.ConvertPayload) {
  return requestClient.post<LeadApi.ConvertResult>(`/leads/${id}/convert`, data);
}

/**
 * 放入公海
 */
async function toPool(id: number) {
  return requestClient.post(`/leads/${id}/to-pool`);
}

/**
 * 从公海领取
 */
async function claimLead(id: number) {
  return requestClient.post(`/leads/${id}/claim`);
}

/** 分配 / 转派负责人 */
async function assignLead(id: number, ownerId: number) {
  return requestClient.post(`/leads/${id}/assign`, null, {
    params: { ownerId },
  });
}

/**
 * Excel 批量导入
 */
async function importLeads(file: File) {
  const formData = new FormData();
  formData.append('file', file);
  return requestClient.post<LeadApi.ImportResult>('/leads/import', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  });
}

export {
  assignLead,
  claimLead,
  convertLead,
  createLead,
  getLeadDetail,
  getLeadList,
  importLeads,
  toPool,
  updateLead,
};
