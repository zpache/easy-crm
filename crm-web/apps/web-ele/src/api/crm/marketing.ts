import { requestClient } from '#/api/request';

/** 营销域：活动管理 + 活动效果（线索归因） */
export namespace MarketingApi {
  export interface PageResult<T> {
    pageNum: number;
    pageSize: number;
    pages: number;
    records: T[];
    total: number;
  }

  export interface Campaign {
    [key: string]: any;
    budget: number;
    channel?: string;
    code: string;
    conversionRate?: null | number;
    convertedCount?: number;
    createTime?: string;
    endDate?: string;
    id: number;
    leadCount?: number;
    name: string;
    ownerId?: null | number;
    ownerName?: string;
    pipelineAmount?: number;
    remark?: string;
    roi?: null | number;
    startDate?: string;
    status: number;
    statusName?: string;
    targetLeads?: number;
    targetRate?: null | number;
    type: number;
    typeName?: string;
    wonAmount?: number;
  }

  export interface CampaignPayload {
    budget?: number;
    channel?: string;
    code?: string;
    endDate?: string;
    name: string;
    remark?: string;
    startDate?: string;
    status?: number;
    targetLeads?: number;
    type?: number;
  }
}

/** 活动类型 */
export const CAMPAIGN_TYPES = [
  { label: '线上推广', value: 1 },
  { label: '线下活动', value: 2 },
  { label: '电话营销', value: 3 },
  { label: '渠道合作', value: 4 },
  { label: '内容营销', value: 5 },
] as const;

/** 活动状态 */
export const CAMPAIGN_STATUSES = [
  { label: '草稿', value: 0 },
  { label: '进行中', value: 1 },
  { label: '已结束', value: 2 },
  { label: '已暂停', value: 3 },
] as const;

async function getCampaignList(params: Recordable<any>) {
  const result = await requestClient.get<MarketingApi.PageResult<any>>(
    '/campaigns',
    { params },
  );
  return { items: result.records, total: result.total };
}

async function getCampaignDetail(id: number) {
  return requestClient.get<MarketingApi.Campaign>(`/campaigns/${id}`);
}

async function createCampaign(data: MarketingApi.CampaignPayload) {
  return requestClient.post<{ id: number }>('/campaigns', data);
}

async function updateCampaign(id: number, data: MarketingApi.CampaignPayload) {
  return requestClient.put(`/campaigns/${id}`, data);
}

async function deleteCampaign(id: number) {
  return requestClient.delete(`/campaigns/${id}`);
}

/** 下拉选项（线索表单 / 报表筛选用） */
async function getCampaignOptions() {
  return requestClient.get<
    Array<{ code: string; id: number; name: string; status: number }>
  >('/campaigns/options');
}

export {
  createCampaign,
  deleteCampaign,
  getCampaignDetail,
  getCampaignList,
  getCampaignOptions,
  updateCampaign,
};
