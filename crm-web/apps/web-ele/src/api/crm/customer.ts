import { requestClient } from '#/api/request';

export namespace CustomerApi {
  /** 客户记录 */
  export interface Customer {
    [key: string]: any;
    creditCode?: string;
    deptId?: null | number;
    deptName?: string;
    id: number;
    industry?: null | number;
    industryName?: string;
    lastFollowTime?: string;
    level?: null | number;
    levelName?: string;
    mergedToId?: null | number;
    name: string;
    ownerId?: null | number;
    ownerName?: string;
    region?: null | number;
    regionName?: string;
    remark?: string;
    scale?: null | number;
    scaleName?: string;
    status: 1 | 2 | 3;
    statusName?: string;
    version?: number;
  }

  /** 创建 / 编辑表单 */
  export interface CustomerPayload {
    creditCode?: string;
    deptId?: null | number;
    industry?: null | number;
    level?: null | number;
    name: string;
    ownerId?: null | number;
    region?: null | number;
    remark?: string;
    scale?: null | number;
  }

  /** 360 视图 */
  export interface View360 {
    contacts: Array<Recordable<any>>;
    customer: Customer;
    followRecords: Array<Recordable<any>>;
    opportunities: Array<Recordable<any>>;
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

/** 字典（与后端 CustomerDicts 常量类保持一致） */
export const CUSTOMER_INDUSTRY = [
  { label: '制造业', value: 1 },
  { label: '互联网', value: 2 },
  { label: '金融', value: 3 },
  { label: '零售消费', value: 4 },
  { label: '医疗健康', value: 5 },
  { label: '教育', value: 6 },
  { label: '其他', value: 9 },
] as const;

export const CUSTOMER_SCALE = [
  { label: '1-50 人', value: 1 },
  { label: '51-200 人', value: 2 },
  { label: '201-1000 人', value: 3 },
  { label: '1000 人以上', value: 4 },
] as const;

export const CUSTOMER_REGION = [
  { label: '华北', value: 1 },
  { label: '华东', value: 2 },
  { label: '华南', value: 3 },
  { label: '西南', value: 4 },
  { label: '海外', value: 5 },
] as const;

export const CUSTOMER_LEVEL = [
  { label: 'A（战略）', value: 1 },
  { label: 'B（重点）', value: 2 },
  { label: 'C（普通）', value: 3 },
] as const;

export const CUSTOMER_STATUS = [
  { label: '正常', value: 1 },
  { label: '公海', value: 2 },
  { label: '已合并', value: 3 },
] as const;

/**
 * 客户分页列表
 */
async function getCustomerList(params: Recordable<any>) {
  const result = await requestClient.get<CustomerApi.PageResult<any>>(
    '/customers',
    { params },
  );
  return { items: result.records, total: result.total };
}

async function getCustomerDetail(id: number) {
  return requestClient.get<CustomerApi.Customer>(`/customers/${id}`);
}

/**
 * 客户 360 视图
 */
async function getCustomer360(id: number) {
  return requestClient.get<CustomerApi.View360>(`/customers/${id}/360`);
}

async function createCustomer(data: CustomerApi.CustomerPayload) {
  return requestClient.post<{ id: number }>('/customers', data);
}

async function updateCustomer(id: number, data: CustomerApi.CustomerPayload) {
  return requestClient.put(`/customers/${id}`, data);
}

/**
 * 客户合并：source 合并到 target
 */
async function mergeCustomer(sourceId: number, targetId: number) {
  return requestClient.post('/customers/merge', {
    sourceId,
    targetId,
  });
}

async function customerToPool(id: number) {
  return requestClient.post(`/customers/${id}/to-pool`);
}

async function claimCustomer(id: number) {
  return requestClient.post(`/customers/${id}/claim`);
}

export {
  claimCustomer,
  createCustomer,
  customerToPool,
  getCustomer360,
  getCustomerDetail,
  getCustomerList,
  mergeCustomer,
  updateCustomer,
};
