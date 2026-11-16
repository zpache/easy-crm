import { requestClient } from '#/api/request';

/** 报表中心：销售目标 + 达成分析 */
export namespace ReportApi {
  export interface PageResult<T> {
    pageNum: number;
    pageSize: number;
    pages: number;
    records: T[];
    total: number;
  }

  export interface SalesTarget {
    [key: string]: any;
    createTime?: string;
    deptId?: null | number;
    deptName?: string;
    id: number;
    ownerId?: null | number;
    ownerName?: string;
    remark?: string;
    targetAmount: number;
    targetMonth: string;
    updateTime?: string;
  }

  export interface SalesTargetPayload {
    deptId?: null | number;
    ownerId?: null | number;
    remark?: string;
    targetAmount: number;
    targetMonth: string;
  }

  export interface AchievementQuery {
    deptId?: null | number;
    endMonth?: string;
    ownerId?: null | number;
    startMonth?: string;
  }

  export interface AchievementRow {
    achievementRate?: null | number;
    month: string;
    opportunityAmount: number;
    orderAmount: number;
    targetAmount: number;
  }

  export interface Achievement {
    achievementRate?: null | number;
    rows: AchievementRow[];
    totalOpportunityAmount: number;
    totalOrderAmount: number;
    totalTarget: number;
  }

  /** 漏斗环节（conversionRate 为百分比数值，如 32.15 表示 32.15%） */
  export interface FunnelStage {
    amount?: null | number;
    code: string;
    conversionRate?: null | number;
    count: number;
    label: string;
  }

  export interface Funnel {
    stages: FunnelStage[];
  }

  export interface RankingRow {
    orderAmount: number;
    orderCount: number;
    ownerId: number;
    ownerName?: string;
    paymentAmount: number;
    rank: number;
    wonAmount: number;
    wonCount: number;
  }

  export interface Ranking {
    rows: RankingRow[];
  }

  export interface RevenueRow {
    completionRate?: null | number;
    invoiceAmount: number;
    month: string;
    netAmount: number;
    paymentAmount: number;
    planAmount: number;
    refundAmount: number;
  }

  export interface Revenue {
    rows: RevenueRow[];
    totalInvoiceAmount: number;
    totalNetAmount: number;
    totalPaymentAmount: number;
    totalPlanAmount: number;
    totalRefundAmount: number;
  }

  export interface DistributionItem {
    count: number;
    key: string;
    label: string;
    percent: number;
  }

  export interface Distribution {
    dimension: string;
    dimensionName: string;
    items: DistributionItem[];
    total: number;
  }
}

// ---------------- 销售目标 ----------------
async function getSalesTargetList(params: Recordable<any>) {
  const result = await requestClient.get<ReportApi.PageResult<any>>(
    '/sales-targets',
    { params },
  );
  return { items: result.records, total: result.total };
}

async function getSalesTargetDetail(id: number) {
  return requestClient.get<ReportApi.SalesTarget>(`/sales-targets/${id}`);
}

async function createSalesTarget(data: ReportApi.SalesTargetPayload) {
  return requestClient.post<{ id: number }>('/sales-targets', data);
}

async function updateSalesTarget(id: number, data: ReportApi.SalesTargetPayload) {
  return requestClient.put(`/sales-targets/${id}`, data);
}

async function deleteSalesTarget(id: number) {
  return requestClient.delete(`/sales-targets/${id}`);
}

// ---------------- 达成分析 ----------------
async function getAchievement(params: ReportApi.AchievementQuery) {
  return requestClient.get<ReportApi.Achievement>('/reports/achievement', {
    params,
  });
}

// ---------------- 漏斗 / 排行 / 回款 / 客户分布 ----------------
async function getFunnel(params: ReportApi.AchievementQuery) {
  return requestClient.get<ReportApi.Funnel>('/reports/funnel', { params });
}

async function getRanking(params: ReportApi.AchievementQuery) {
  return requestClient.get<ReportApi.Ranking>('/reports/ranking', { params });
}

async function getRevenue(params: ReportApi.AchievementQuery) {
  return requestClient.get<ReportApi.Revenue>('/reports/revenue', { params });
}

async function getCustomerDistribution(
  params: ReportApi.AchievementQuery & { dimension?: string },
) {
  return requestClient.get<ReportApi.Distribution>(
    '/reports/customer-distribution',
    { params },
  );
}

export {
  createSalesTarget,
  deleteSalesTarget,
  getAchievement,
  getCustomerDistribution,
  getFunnel,
  getRanking,
  getRevenue,
  getSalesTargetDetail,
  getSalesTargetList,
  updateSalesTarget,
};
