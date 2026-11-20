import { requestClient } from '#/api/request';
import { ref } from 'vue';

/** 产品域：产品 / 价目表 / 商机明细 / 报价单 */
export namespace ProductApi {
  export interface PageResult<T> {
    pageNum: number;
    pageSize: number;
    pages: number;
    records: T[];
    total: number;
  }

  export interface Product {
    [key: string]: any;
    category?: string;
    code: string;
    costPrice?: number;
    createTime?: string;
    id: number;
    name: string;
    remark?: string;
    specification?: string;
    standardPrice?: number;
    status: number;
    statusName?: string;
    unit?: string;
    updateTime?: string;
  }

  export interface ProductPayload {
    category?: string;
    code: string;
    costPrice?: number;
    name: string;
    remark?: string;
    specification?: string;
    standardPrice?: number;
    status?: number;
    unit?: string;
  }

  export interface PriceList {
    [key: string]: any;
    effectiveDate?: string;
    expireDate?: string;
    id: number;
    price: number;
    productId: number;
    productName?: string;
    remark?: string;
    status: number;
  }

  export interface PriceListPayload {
    effectiveDate?: string;
    expireDate?: string;
    price: number;
    productId: number;
    remark?: string;
    status?: number;
  }

  export interface OpportunityItem {
    [key: string]: any;
    discount?: number;
    id: number;
    opportunityId: number;
    productId?: null | number;
    productName: string;
    quantity: number;
    remark?: string;
    subtotal: number;
    unitPrice: number;
  }

  export interface OpportunityItemPayload {
    discount?: number;
    opportunityId: number;
    productId?: null | number;
    productName: string;
    quantity: number;
    remark?: string;
    unitPrice: number;
  }

  export interface QuoteItem {
    [key: string]: any;
    discount?: number;
    id?: number;
    productId?: null | number;
    productName: string;
    quantity: number;
    quoteId?: number;
    remark?: string;
    subtotal?: number;
    unitPrice: number;
  }

  export interface Quote {
    [key: string]: any;
    createTime?: string;
    customerId: number;
    customerName?: string;
    id: number;
    items?: QuoteItem[];
    opportunityId?: null | number;
    ownerId?: null | number;
    quoteNo: string;
    remark?: string;
    status: number;
    statusName?: string;
    totalAmount: number;
    updateTime?: string;
    validUntil?: string;
  }

  export interface QuotePayload {
    customerId: number;
    items: QuoteItem[];
    opportunityId?: null | number;
    quoteNo?: string;
    remark?: string;
    status?: number;
    totalAmount: number;
    validUntil?: string;
  }
}

/** 产品状态字典 */
export const PRODUCT_STATUSES = [
  { label: '草稿', value: 0 },
  { label: '上架', value: 1 },
  { label: '停用', value: 2 },
] as const;

/** 报价单状态字典 */
export const QUOTE_STATUSES = [
  { label: '草稿', value: 0 },
  { label: '已提交', value: 1 },
  { label: '已确认', value: 2 },
  { label: '已作废', value: 3 },
] as const;

// ---------------- 产品 ----------------
async function getProductList(params: Recordable<any>) {
  const result = await requestClient.get<ProductApi.PageResult<any>>('/products', {
    params,
  });
  return { items: result.records, total: result.total };
}

async function getProductDetail(id: number) {
  return requestClient.get<ProductApi.Product>(`/products/${id}`);
}

async function createProduct(data: ProductApi.ProductPayload) {
  return requestClient.post<{ id: number }>('/products', data);
}

async function updateProduct(id: number, data: ProductApi.ProductPayload) {
  return requestClient.put(`/products/${id}`, data);
}

async function deleteProduct(id: number) {
  return requestClient.delete(`/products/${id}`);
}

// ---------------- 价目表 ----------------
async function getPriceList(params: Recordable<any>) {
  const result = await requestClient.get<ProductApi.PageResult<any>>(
    '/price-lists',
    { params },
  );
  return { items: result.records, total: result.total };
}

async function getEffectivePrices(productId: number, date?: string) {
  return requestClient.get<ProductApi.PriceList[]>('/price-lists/effective', {
    params: { date, productId },
  });
}

async function createPriceList(data: ProductApi.PriceListPayload) {
  return requestClient.post<{ id: number }>('/price-lists', data);
}

async function updatePriceList(id: number, data: ProductApi.PriceListPayload) {
  return requestClient.put(`/price-lists/${id}`, data);
}

async function deletePriceList(id: number) {
  return requestClient.delete(`/price-lists/${id}`);
}

// ---------------- 商机产品明细 ----------------
async function getOpportunityItemList(params: Recordable<any>) {
  const result = await requestClient.get<ProductApi.PageResult<any>>(
    '/opportunity-items',
    { params },
  );
  return { items: result.records, total: result.total };
}

async function getOpportunityItemSummary(opportunityId: number) {
  return requestClient.get<number>('/opportunity-items/summary', {
    params: { opportunityId },
  });
}

async function createOpportunityItem(data: ProductApi.OpportunityItemPayload) {
  return requestClient.post<{ id: number }>('/opportunity-items', data);
}

async function updateOpportunityItem(
  id: number,
  data: ProductApi.OpportunityItemPayload,
) {
  return requestClient.put(`/opportunity-items/${id}`, data);
}

async function deleteOpportunityItem(id: number) {
  return requestClient.delete(`/opportunity-items/${id}`);
}

// ---------------- 报价单 ----------------
async function getQuoteList(params: Recordable<any>) {
  const result = await requestClient.get<ProductApi.PageResult<any>>('/quotes', {
    params,
  });
  return { items: result.records, total: result.total };
}

async function getQuoteDetail(id: number) {
  return requestClient.get<ProductApi.Quote>(`/quotes/${id}`);
}

async function createQuote(data: ProductApi.QuotePayload) {
  return requestClient.post<{ id: number }>('/quotes', data);
}

async function updateQuote(id: number, data: ProductApi.QuotePayload) {
  return requestClient.put(`/quotes/${id}`, data);
}

async function deleteQuote(id: number) {
  return requestClient.delete(`/quotes/${id}`);
}

/** 商品下拉共享选项（订单/报价明细引用商品主数据，模块级缓存） */
export const productOptions = ref<
  Array<{ id: number; name: string; standardPrice?: number }>
>([]);

export async function loadProductOptions() {
  if (productOptions.value.length > 0) {
    return;
  }
  const result = await getProductList({ pageSize: 200, status: 1 });
  productOptions.value = result.items.map((item: any) => ({
    id: item.id,
    name: item.name,
    standardPrice: item.standardPrice,
  }));
}

export {
  createOpportunityItem,
  createPriceList,
  createProduct,
  createQuote,
  deleteOpportunityItem,
  deletePriceList,
  deleteProduct,
  deleteQuote,
  getEffectivePrices,
  getOpportunityItemList,
  getOpportunityItemSummary,
  getPriceList,
  getProductDetail,
  getProductList,
  getQuoteDetail,
  getQuoteList,
  updateOpportunityItem,
  updatePriceList,
  updateProduct,
  updateQuote,
};
