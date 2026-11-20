import { requestClient } from '#/api/request';

export namespace OrderApi {
  export interface PageResult<T> {
    pageNum: number;
    pageSize: number;
    pages: number;
    records: T[];
    total: number;
  }

  export interface OrderItem {
    [key: string]: any;
    discount?: number;
    id?: number;
    orderId?: number;
    productId?: null | number;
    productName: string;
    quantity: number;
    remark?: string;
    subtotal?: number;
    unitPrice: number;
  }

  export interface Order {
    [key: string]: any;
    contractId
    quoteId?: null | number;?: null | number;
    createTime?: string;
    customerId: number;
    customerName?: string;
    deliveryDate?: string;
    id: number;
    items?: OrderItem[];
    opportunityId?: null | number;
    orderNo: string;
    ownerId?: null | number;
    remark?: string;
    shipDate?: string;
    status: number;
    statusName?: string;
    totalAmount: number;
    updateTime?: string;
  }

  export interface OrderPayload {
    contractId?: null | number;
    quoteId?: null | number;
    customerId: number;
    deliveryDate?: string;
    items: OrderItem[];
    opportunityId?: null | number;
    orderNo?: string;
    remark?: string;
    shipDate?: string;
    totalAmount: number;
  }

  export interface OrderStatusPayload {
    deliveryDate?: string;
    shipDate?: string;
    status: number;
  }
}

/** 订单状态机：0 草稿 / 1 待确认 / 2 已确认 / 3 已交付 / 4 已完成 / 9 已取消 */
export const ORDER_STATUSES = [
  { label: '草稿', value: 0 },
  { label: '待确认', value: 1 },
  { label: '已确认', value: 2 },
  { label: '已交付', value: 3 },
  { label: '已完成', value: 4 },
  { label: '已取消', value: 9 },
] as const;

async function getOrderList(params: Recordable<any>) {
  const result = await requestClient.get<OrderApi.PageResult<any>>('/orders', {
    params,
  });
  return { items: result.records, total: result.total };
}

async function getOrderDetail(id: number) {
  return requestClient.get<OrderApi.Order>(`/orders/${id}`);
}

async function getOrderItems(id: number) {
  return requestClient.get<OrderApi.OrderItem[]>(`/orders/${id}/items`);
}

async function createOrder(data: OrderApi.OrderPayload) {
  return requestClient.post<{ id: number }>('/orders', data);
}

async function updateOrder(id: number, data: OrderApi.OrderPayload) {
  return requestClient.put(`/orders/${id}`, data);
}

async function changeOrderStatus(id: number, data: OrderApi.OrderStatusPayload) {
  return requestClient.put(`/orders/${id}/status`, data);
}

async function deleteOrder(id: number) {
  return requestClient.delete(`/orders/${id}`);
}

export {
  changeOrderStatus,
  createOrder,
  deleteOrder,
  getOrderDetail,
  getOrderItems,
  getOrderList,
  updateOrder,
};
