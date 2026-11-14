import { requestClient } from '#/api/request';

export namespace FollowApi {
  /** 跟进记录 */
  export interface FollowRecord {
    [key: string]: any;
    bizId: number;
    bizType: string;
    bizTypeName?: string;
    content: string;
    createTime?: string;
    deptId?: null | number;
    followType: string;
    followTypeName?: string;
    id: number;
    nextFollowTime?: string;
    ownerId?: null | number;
    ownerName?: string;
    updateTime?: string;
  }

  /** 创建表单 */
  export interface FollowPayload {
    bizId: number;
    bizType: string;
    content: string;
    followType: string;
    nextFollowTime?: string;
  }

  /** 编辑表单（关联对象不可变更，仅内容 / 类型 / 下次跟进时间） */
  export interface FollowUpdatePayload {
    content: string;
    followType: string;
    nextFollowTime?: string;
  }

  /** 时间线条目（跟进 / 待办 / 阶段变更三类聚合） */
  export interface TimelineItem {
    [key: string]: any;
    content?: string;
    fromStage?: string;
    fromStageName?: string;
    id: number;
    operatorId?: null | number;
    operatorName?: string;
    source: string;
    time?: string;
    toStage?: string;
    toStageName?: string;
    typeCode?: string;
    typeName?: string;
  }

  export interface PageResult<T> {
    pageNum: number;
    pageSize: number;
    pages: number;
    records: T[];
    total: number;
  }

  /** vxe-grid 需要 items + total，后端原始结构是 records + total，这里做一层转换 */
  export interface PagedResult<T> {
    items: T[];
    total: number;
  }

  /** 跟进记录分页查询参数 */
  export interface Query {
    bizId?: number;
    bizType?: string;
    endDate?: string;
    ownerId?: number;
    pageNum?: number;
    pageSize?: number;
    startDate?: string;
  }

  /** 时间线查询参数 */
  export interface TimelineQuery {
    bizId: number;
    bizType: string;
    pageNum?: number;
    pageSize?: number;
  }
}

/** 业务类型字典（与后端 FollowBizTypes 保持一致） */
export const FOLLOW_BIZ_TYPES = [
  { label: '线索', value: 'lead' },
  { label: '客户', value: 'customer' },
  { label: '商机', value: 'opportunity' },
] as const;

/** 跟进类型字典（与后端 FollowTypes 保持一致） */
export const FOLLOW_TYPES = [
  { label: '电话', value: 'PHONE' },
  { label: '拜访', value: 'VISIT' },
  { label: '邮件', value: 'EMAIL' },
  { label: '微信', value: 'WECHAT' },
  { label: '其他', value: 'OTHER' },
] as const;

/** 时间线来源标签 */
export const TIMELINE_SOURCES = [
  { color: 'primary', label: '跟进记录', value: 'follow' },
  { color: 'warning', label: '待办任务', value: 'task' },
  { color: 'success', label: '阶段变更', value: 'stage_log' },
] as const;

/** 跟进记录分页 */
async function getFollowRecords(params: FollowApi.Query) {
  const result = await requestClient.get<FollowApi.PageResult<FollowApi.FollowRecord>>(
    '/follow-records',
    { params },
  );
  return { items: result.records, total: result.total };
}

/** 时间线聚合分页（按 bizType + bizId） */
async function getTimeline(params: FollowApi.TimelineQuery) {
  const result = await requestClient.get<FollowApi.PageResult<FollowApi.TimelineItem>>(
    '/timeline',
    { params },
  );
  return { items: result.records, total: result.total };
}

async function createFollowRecord(data: FollowApi.FollowPayload) {
  return requestClient.post<FollowApi.FollowRecord>('/follow-records', data);
}

async function updateFollowRecord(id: number, data: FollowApi.FollowUpdatePayload) {
  return requestClient.put<FollowApi.FollowRecord>(`/follow-records/${id}`, data);
}

async function deleteFollowRecord(id: number) {
  return requestClient.delete(`/follow-records/${id}`);
}

export {
  createFollowRecord,
  deleteFollowRecord,
  getFollowRecords,
  getTimeline,
  updateFollowRecord,
};
