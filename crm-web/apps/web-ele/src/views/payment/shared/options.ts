import { ref } from 'vue';

import { getCustomerList } from '#/api/crm/customer';
import { getOrderList } from '#/api/crm/order';

/** 开票 / 退款共用的客户、订单候选数据与状态字典 */

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

export const customerOptions = ref<Array<{ id: number; name: string }>>([]);

export const orderOptions = ref<
  Array<{ customerId: number; customerName?: string; id: number; orderNo: string }>
>([]);

export async function loadCustomerOptions() {
  if (customerOptions.value.length > 0) {
    return;
  }
  const result = await getCustomerList({ pageSize: 200 });
  customerOptions.value = result.items.map((item: any) => ({
    id: item.id,
    name: item.name,
  }));
}

export async function loadOrderOptions() {
  if (orderOptions.value.length > 0) {
    return;
  }
  const result = await getOrderList({ pageSize: 200 });
  orderOptions.value = result.items.map((item: any) => ({
    customerId: item.customerId,
    customerName: item.customerName,
    id: item.id,
    orderNo: item.orderNo,
  }));
}

/** 选中订单后反查其客户，用于自动带出客户 */
export function customerIdOfOrder(orderId?: number) {
  if (!orderId) {
    return undefined;
  }
  return orderOptions.value.find((o) => o.id === orderId)?.customerId;
}

export function customerSelectProps() {
  return {
    allowClear: true,
    class: 'w-full',
    filterOption: (input: string, option: any) =>
      String(option.label).includes(input),
    options: customerOptions.value.map((c) => ({ label: c.name, value: c.id })),
    showSearch: true,
  };
}

export function orderSelectProps() {
  return {
    allowClear: true,
    class: 'w-full',
    filterOption: (input: string, option: any) =>
      String(option.label).includes(input),
    options: orderOptions.value.map((o) => ({
      label: `${o.orderNo}${o.customerName ? ` / ${o.customerName}` : ''}`,
      value: o.id,
    })),
    showSearch: true,
  };
}
