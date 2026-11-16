import { requestClient } from '#/api/request';

/**
 * 审批中心（只读）：审批流对接外部审批系统，此处仅提供
 * 待办 / 已办 / 已发起三个入口列表与外部跳转。
 */
export namespace ApprovalApi {
  export interface PageResult<T> {
    pageNum: number;
    pageSize: number;
    pages: number;
    records: T[];
    total: number;
  }

  export interface Approval {
    [key: string]: any;
    applyTime?: string;
    applyUserId?: null | number;
    applyUserName?: string;
    bizId?: null | number;
    bizNo?: string;
    bizType: string;
    bizTypeName?: string;
    currentApproverId?: null | number;
    currentApproverName?: string;
    externalUrl?: string;
    finishTime?: string;
    id: number;
    nodeName?: string;
    remark?: string;
    sourceSystem?: string;
    status: number;
    statusName?: string;
    title: string;
  }

  /** todo 待办 / done 已办 / applied 已发起 */
  export type Box = 'applied' | 'done' | 'todo';

  export interface Summary {
    applied: number;
    done: number;
    todo: number;
  }
}

/** 业务类型 */
export const APPROVAL_BIZ_TYPES = [
  { label: '报价单', value: 'QUOTE' },
  { label: '合同', value: 'CONTRACT' },
  { label: '订单', value: 'ORDER' },
  { label: '开票', value: 'INVOICE' },
  { label: '退款', value: 'REFUND' },
  { label: '其他', value: 'OTHER' },
] as const;

/** 审批状态 */
export const APPROVAL_STATUSES = [
  { label: '审批中', value: 0 },
  { label: '已通过', value: 1 },
  { label: '已驳回', value: 2 },
  { label: '已撤销', value: 3 },
] as const;

async function getApprovalList(params: Recordable<any>) {
  const result = await requestClient.get<ApprovalApi.PageResult<any>>(
    '/approvals',
    { params },
  );
  return { items: result.records, total: result.total };
}

async function getApprovalSummary() {
  return requestClient.get<ApprovalApi.Summary>('/approvals/summary');
}

async function getApprovalDetail(id: number) {
  return requestClient.get<ApprovalApi.Approval>(`/approvals/${id}`);
}

export { getApprovalDetail, getApprovalList, getApprovalSummary };
