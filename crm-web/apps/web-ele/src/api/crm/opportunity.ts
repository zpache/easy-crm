import { requestClient } from '#/api/request';

export namespace OpportunityApi {
  /** 商机记录 */
  export interface Opportunity {
    [key: string]: any;
    amount?: null | number;
    contactId?: null | number;
    contactName?: string;
    customerId: number;
    customerName?: string;
    expectedCloseDate?: string;
    id: number;
    lastActivityAt?: string;
    leadId?: null | number;
    lostReason?: string;
    lostReasonName?: string;
    name: string;
    ownerId?: null | number;
    ownerName?: string;
    stageCode: string;
    stageName?: string;
    staleLevel?: number;
    winRate?: number;
  }

  /** 创建 / 编辑表单 */
  export interface OpportunityPayload {
    amount?: null | number;
    contactId?: null | number;
    customerId?: number;
    expectedCloseDate?: string;
    name: string;
  }

  /** 阶段变更表单 */
  export interface StageChangePayload {
    actualAmount?: null | number;
    actualCloseDate?: string;
    competitor?: string;
    lostReason?: string;
    targetStageCode: string;
  }

  /** 阶段配置 */
  export interface Stage {
    defaultWinRate: number;
    enabled: boolean;
    id: number;
    isLost: boolean;
    isWon: boolean;
    sortNo: number;
    stageCode: string;
    stageName: string;
  }

  /** 阶段配置表单 */
  export interface StagePayload {
    defaultWinRate: number;
    enabled: number;
    isLost: number;
    isWon: number;
    sortNo: number;
    stageCode: string;
    stageName: string;
  }

  /** 看板阶段分组 */
  export interface BoardStage {
    defaultWinRate: number;
    enabled: boolean;
    lost: boolean;
    opportunities: Opportunity[];
    stageCode: string;
    stageName: string;
    won: boolean;
  }

  /** 预测聚合 */
  export interface Forecast {
    rows: Array<{
      cnt: number;
      forecastAmount: number;
      stageCode: string;
      stageName: string;
      totalAmount: number;
    }>;
    totalForecast: number;
    totalCount: number;
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

/** 丢单原因字典（与后端 LostReasons 保持一致） */
export const LOST_REASONS = [
  { label: '价格因素', value: 'PRICE' },
  { label: '功能不满足', value: 'FEATURE' },
  { label: '选择了竞争对手', value: 'COMPETITOR' },
  { label: '预算取消', value: 'BUDGET' },
  { label: '失去联系', value: 'NO_CONTACT' },
  { label: '其他', value: 'OTHER' },
] as const;

/**
 * 商机分页列表
 */
async function getOpportunityList(params: Recordable<any>) {
  const result = await requestClient.get<OpportunityApi.PageResult<any>>(
    '/opportunities',
    { params },
  );
  return { items: result.records, total: result.total };
}

async function getOpportunityDetail(id: number) {
  return requestClient.get<OpportunityApi.Opportunity>(`/opportunities/${id}`);
}

async function createOpportunity(data: OpportunityApi.OpportunityPayload) {
  return requestClient.post<{ id: number }>('/opportunities', data);
}

async function updateOpportunity(id: number, data: OpportunityApi.OpportunityPayload) {
  return requestClient.put(`/opportunities/${id}`, data);
}

/**
 * 变更阶段（赢单回写成交信息；丢单必填原因）
 */
async function changeStage(id: number, data: OpportunityApi.StageChangePayload) {
  return requestClient.put(`/opportunities/${id}/stage`, data);
}

/**
 * 看板数据（一次请求返回全部阶段分组）
 */
async function getBoard() {
  return requestClient.get<OpportunityApi.BoardStage[]>('/opportunities/board');
}

/**
 * 销售预测（按阶段聚合）
 */
async function getForecast(params: Recordable<any>) {
  return requestClient.get<OpportunityApi.Forecast>('/opportunities/forecast', {
    params,
  });
}

/**
 * 阶段配置列表
 */
async function getStages() {
  return requestClient.get<OpportunityApi.Stage[]>('/opportunities/stages');
}

async function createStage(data: OpportunityApi.StagePayload) {
  return requestClient.post<{ id: number }>('/opportunities/stages', data);
}

async function updateStage(id: number, data: OpportunityApi.StagePayload) {
  return requestClient.put(`/opportunities/stages/${id}`, data);
}

/** 分配 / 转派负责人 */
async function assignOpportunity(id: number, ownerId: number) {
  return requestClient.post(`/opportunities/${id}/assign`, null, {
    params: { ownerId },
  });
}

export {
  assignOpportunity,
  changeStage,
  createOpportunity,
  createStage,
  getBoard,
  getForecast,
  getOpportunityDetail,
  getOpportunityList,
  getStages,
  updateOpportunity,
  updateStage,
};
