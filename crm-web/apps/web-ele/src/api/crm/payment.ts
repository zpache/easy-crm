import { requestClient } from '#/api/request';

/** 交易域：回款计划 / 回款记录 / 开票 / 退款 */
export namespace PaymentApi {
  export interface PageResult<T> {
    pageNum: number;
    pageSize: number;
    pages: number;
    records: T[];
    total: number;
  }

  export interface PaymentPlan {
    [key: string]: any;
    createTime?: string;
    id: number;
    orderId: number;
    periodNo: number;
    planAmount: number;
    planDate?: string;
    receivedAmount: number;
    remark?: string;
    status: number;
    statusName?: string;
    updateTime?: string;
  }

  export interface PaymentPlanPayload {
    orderId: number;
    periodNo: number;
    planAmount: number;
    planDate?: string;
    remark?: string;
  }

  export interface Payment {
    [key: string]: any;
    amount: number;
    createTime?: string;
    id: number;
    orderId: number;
    paymentDate?: string;
    planId?: null | number;
    remark?: string;
    writeoffStatus: number;
    writeoffStatusName?: string;
    writeoffTime?: string;
  }

  export interface PaymentPayload {
    amount: number;
    orderId: number;
    paymentDate?: string;
    planId?: null | number;
    remark?: string;
  }

  export interface Invoice {
    [key: string]: any;
    amount: number;
    applyDate?: string;
    createTime?: string;
    customerId: number;
    id: number;
    invoiceNo?: string;
    orderId: number;
    remark?: string;
    status: number;
    statusName?: string;
    taxNo?: string;
    title?: string;
  }

  export interface InvoicePayload {
    amount: number;
    applyDate?: string;
    customerId: number;
    invoiceNo?: string;
    orderId: number;
    remark?: string;
    taxNo?: string;
    title?: string;
  }

  export interface Refund {
    [key: string]: any;
    amount: number;
    createTime?: string;
    customerId: number;
    id: number;
    orderId: number;
    reason?: string;
    refundDate?: string;
    refundNo: string;
    remark?: string;
    status: number;
    statusName?: string;
  }

  export interface RefundPayload {
    amount: number;
    customerId: number;
    orderId: number;
    reason?: string;
    refundDate?: string;
    refundNo?: string;
    remark?: string;
  }
}

export const PLAN_STATUSES = [
  { label: '未核销', value: 0 },
  { label: '部分核销', value: 1 },
  { label: '已核销', value: 2 },
] as const;

export const WRITEOFF_STATUSES = [
  { label: '未核销', value: 0 },
  { label: '已核销', value: 1 },
] as const;

export const INVOICE_STATUSES = [
  { label: '待开票', value: 0 },
  { label: '已开票', value: 1 },
  { label: '已作废', value: 2 },
] as const;

export const REFUND_STATUSES = [
  { label: '申请中', value: 0 },
  { label: '已审核', value: 1 },
  { label: '已退款', value: 2 },
  { label: '已驳回', value: 3 },
] as const;

// ---------------- 回款计划 ----------------
async function getPaymentPlanList(params: Recordable<any>) {
  const result = await requestClient.get<PaymentApi.PageResult<any>>(
    '/payment-plans',
    { params },
  );
  return { items: result.records, total: result.total };
}

async function createPaymentPlan(data: PaymentApi.PaymentPlanPayload) {
  return requestClient.post<{ id: number }>('/payment-plans', data);
}

async function updatePaymentPlan(
  id: number,
  data: PaymentApi.PaymentPlanPayload,
) {
  return requestClient.put(`/payment-plans/${id}`, data);
}

async function deletePaymentPlan(id: number) {
  return requestClient.delete(`/payment-plans/${id}`);
}

// ---------------- 回款记录 ----------------
async function getPaymentList(params: Recordable<any>) {
  const result = await requestClient.get<PaymentApi.PageResult<any>>(
    '/payments',
    { params },
  );
  return { items: result.records, total: result.total };
}

async function createPayment(data: PaymentApi.PaymentPayload) {
  return requestClient.post<{ id: number }>('/payments', data);
}

async function updatePayment(id: number, data: PaymentApi.PaymentPayload) {
  return requestClient.put(`/payments/${id}`, data);
}

async function deletePayment(id: number) {
  return requestClient.delete(`/payments/${id}`);
}

/** 回款核销：把一条回款记录核销到指定计划 */
async function writeoffPayment(id: number, planId: number) {
  return requestClient.post(`/payments/${id}/writeoff`, { planId });
}

// ---------------- 开票申请 ----------------
async function getInvoiceList(params: Recordable<any>) {
  const result = await requestClient.get<PaymentApi.PageResult<any>>(
    '/invoices',
    { params },
  );
  return { items: result.records, total: result.total };
}

async function createInvoice(data: PaymentApi.InvoicePayload) {
  return requestClient.post<{ id: number }>('/invoices', data);
}

async function updateInvoice(id: number, data: PaymentApi.InvoicePayload) {
  return requestClient.put(`/invoices/${id}`, data);
}

async function changeInvoiceStatus(id: number, status: number) {
  return requestClient.put(`/invoices/${id}/status`, null, {
    params: { status },
  });
}

async function deleteInvoice(id: number) {
  return requestClient.delete(`/invoices/${id}`);
}

// ---------------- 退货 / 退款 ----------------
async function getRefundList(params: Recordable<any>) {
  const result = await requestClient.get<PaymentApi.PageResult<any>>('/refunds', {
    params,
  });
  return { items: result.records, total: result.total };
}

async function createRefund(data: PaymentApi.RefundPayload) {
  return requestClient.post<{ id: number }>('/refunds', data);
}

async function updateRefund(id: number, data: PaymentApi.RefundPayload) {
  return requestClient.put(`/refunds/${id}`, data);
}

async function changeRefundStatus(id: number, status: number) {
  return requestClient.put(`/refunds/${id}/status`, null, {
    params: { status },
  });
}

async function deleteRefund(id: number) {
  return requestClient.delete(`/refunds/${id}`);
}

export {
  changeInvoiceStatus,
  changeRefundStatus,
  createInvoice,
  createPayment,
  createPaymentPlan,
  createRefund,
  deleteInvoice,
  deletePayment,
  deletePaymentPlan,
  deleteRefund,
  getInvoiceList,
  getPaymentList,
  getPaymentPlanList,
  getRefundList,
  updateInvoice,
  updatePayment,
  updatePaymentPlan,
  updateRefund,
  writeoffPayment,
};
