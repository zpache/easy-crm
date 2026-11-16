import { requestClient } from '#/api/request';

export namespace SystemDictApi {
  export interface Dict {
    [key: string]: any;
    builtIn: 0 | 1;
    dictType: string;
    id: number;
    itemCount?: number;
    name: string;
    remark?: string;
    status: 0 | 1;
    updateTime?: string;
  }

  export interface DictItem {
    [key: string]: any;
    dictType?: string;
    id?: number;
    label: string;
    remark?: string;
    sortNo?: number;
    status: 0 | 1;
    value: string;
  }

  export interface DictPayload {
    name: string;
    remark?: string;
    status?: number;
  }

  export interface DictItemPayload {
    label: string;
    remark?: string;
    sortNo?: number;
    status?: number;
  }

  export interface PageResult<T> {
    pageNum: number;
    pageSize: number;
    pages: number;
    records: T[];
    total: number;
  }

  export interface GridPageResult<T> {
    items: T[];
    total: number;
  }
}

/** 管理端：字典类型分页（归一为 { items, total } 供表格代理使用） */
async function pageDicts(params: {
  keyword?: string;
  pageNum: number;
  pageSize: number;
}): Promise<SystemDictApi.GridPageResult<SystemDictApi.Dict>> {
  const result = await requestClient.get<SystemDictApi.PageResult<SystemDictApi.Dict>>(
    '/system/dict/list',
    { params },
  );
  return { items: result.records, total: result.total };
}

/** 管理端：新增字典类型 */
function createDict(data: { dictType: string } & SystemDictApi.DictPayload) {
  return requestClient.post<SystemDictApi.Dict>('/system/dict', data);
}

/** 管理端：编辑字典类型 */
function updateDict(id: number, data: SystemDictApi.DictPayload) {
  return requestClient.put<SystemDictApi.Dict>(`/system/dict/${id}`, data);
}

/** 管理端：删除字典类型（内置字典不可删） */
function deleteDict(id: number) {
  return requestClient.delete(`/system/dict/${id}`);
}

/** 管理端：字典项列表（含停用） */
function listDictItems(dictId: number) {
  return requestClient.get<SystemDictApi.DictItem[]>(`/system/dict/${dictId}/items`);
}

/** 管理端：新增字典项 */
function createDictItem(dictId: number, data: SystemDictApi.DictItemPayload) {
  return requestClient.post<SystemDictApi.DictItem>(
    `/system/dict/${dictId}/items`,
    data,
  );
}

/** 管理端：编辑字典项 */
function updateDictItem(itemId: number, data: SystemDictApi.DictItemPayload) {
  return requestClient.put<void>(`/system/dict/items/${itemId}`, data);
}

/** 管理端：删除字典项 */
function deleteDictItem(itemId: number) {
  return requestClient.delete(`/system/dict/items/${itemId}`);
}

/** 业务端：按字典类型取生效字典项（下拉 / 导入校验共用，带内置默认值回退） */
function getDictData(dictType: string) {
  return requestClient.get<SystemDictApi.DictItem[]>(`/dict-data/${dictType}`);
}

export {
  createDict,
  createDictItem,
  deleteDict,
  deleteDictItem,
  getDictData,
  listDictItems,
  pageDicts,
  updateDict,
  updateDictItem,
};
