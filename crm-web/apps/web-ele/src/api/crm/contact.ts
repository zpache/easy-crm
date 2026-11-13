import { requestClient } from '#/api/request';

export namespace ContactApi {
  /** 联系人记录 */
  export interface Contact {
    [key: string]: any;
    createTime?: string;
    customerId: number;
    customerName?: string;
    email?: string;
    id: number;
    isPrimary?: boolean;
    name: string;
    phone?: string;
    position?: string;
    remark?: string;
    updateTime?: string;
  }

  /** 创建 / 编辑表单 */
  export interface ContactPayload {
    customerId: number;
    email?: string;
    isPrimary?: boolean;
    name: string;
    phone?: string;
    position?: string;
    remark?: string;
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

/**
 * 联系人分页列表（customerId 按客户查询）
 */
async function getContactList(params: Recordable<any>) {
  const result = await requestClient.get<ContactApi.PageResult<any>>(
    '/contacts',
    { params },
  );
  return { items: result.records, total: result.total };
}

async function createContact(data: ContactApi.ContactPayload) {
  return requestClient.post<{ id: number }>('/contacts', data);
}

async function updateContact(id: number, data: ContactApi.ContactPayload) {
  return requestClient.put(`/contacts/${id}`, data);
}

async function deleteContact(id: number) {
  return requestClient.delete(`/contacts/${id}`);
}

/**
 * 设为主联系人（同客户其余主联系人自动取消）
 */
async function setContactPrimary(id: number) {
  return requestClient.put(`/contacts/${id}/primary`);
}

export {
  createContact,
  deleteContact,
  getContactList,
  setContactPrimary,
  updateContact,
};
