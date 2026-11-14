import { requestClient } from '#/api/request';

export namespace DashboardApi {
  /** 指标卡 */
  export interface Cards {
    customersNew7d: number;
    customersTotal: number;
    leadsNew7d: number;
    leadsTotal: number;
    myTasksOverdue: number;
    myTasksPending: number;
    oppForecastAmount: number;
    oppOpenAmount: number;
    oppOpenCnt: number;
    oppTotal: number;
  }

  /** 漏斗项 */
  export interface FunnelItem {
    stage: string;
    value: number;
  }

  /** 阶段分布项 */
  export interface StageDist {
    amount: number;
    count: number;
    forecastAmount: number;
    sortNo: number;
    stageName: string;
    winRate: number;
  }

  /** 趋势点 */
  export interface TrendPoint {
    count: number;
    date: string;
  }

  /** 新增线索 / 客户趋势点 */
  export interface NewTrendPoint {
    customers: number;
    date: string;
    leads: number;
  }

  /** 负责人排行 */
  export interface OwnerRank {
    amount: number;
    count: number;
    ownerName: string;
  }

  /** 看板总览 */
  export interface Overview {
    cards: Cards;
    followTrend: TrendPoint[];
    funnel: FunnelItem[];
    newTrend: NewTrendPoint[];
    ownerRanking: OwnerRank[];
    stageDistribution: StageDist[];
    staleReminders: Record<string, number>;
  }
}

/**
 * 看板总览（trendDays：趋势统计范围 7/30/90）
 */
export async function getDashboardOverview(trendDays = 30) {
  return requestClient.get<DashboardApi.Overview>('/dashboard/overview', {
    params: { trendDays },
  });
}

export { getDashboardOverview as getDashboardOverviewApi };
