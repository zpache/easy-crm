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

export {
  createSalesTarget,
  deleteSalesTarget,
  getAchievement,
  getSalesTargetDetail,
  getSalesTargetList,
  updateSalesTarget,
};
