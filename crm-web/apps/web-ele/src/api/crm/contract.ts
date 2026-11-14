import { requestClient } from '#/api/request';

export namespace ContractApi {
  export interface PageResult<T> {
    pageNum: number;
    pageSize: number;
    pages: number;
    records: T[];
    total: number;
  }

  export interface Contract {
    [key: string]: any;
    amount: number;
    contractNo: string;
    createTime?: string;
    customerId: number;
    customerName?: string;
    deptId?: null | number;
    id: number;
    opportunityId?: null | number;
    ownerId?: null | number;
    remark?: string;
    signDate?: string;
    status: number;
    statusName?: string;
    updateTime?: string;
  }

  export interface ContractPayload {
    amount: number;
    contractNo?: string;
    customerId: number;
    opportunityId?: null | number;
    remark?: string;
    signDate?: string;
    status?: number;
  }
}

/** 合同状态字典 */
export const CONTRACT_STATUSES = [
  { label: '草稿', value: 0 },
  { label: '生效', value: 1 },
  { label: '已归档', value: 2 },
  { label: '已作废', value: 3 },
] as const;

async function getContractList(params: Recordable<any>) {
  const result = await requestClient.get<ContractApi.PageResult<any>>(
    '/contracts',
    { params },
  );
  return { items: result.records, total: result.total };
}

async function getContractDetail(id: number) {
  return requestClient.get<ContractApi.Contract>(`/contracts/${id}`);
}

async function createContract(data: ContractApi.ContractPayload) {
  return requestClient.post<{ id: number }>('/contracts', data);
}

async function updateContract(id: number, data: ContractApi.ContractPayload) {
  return requestClient.put(`/contracts/${id}`, data);
}

async function changeContractStatus(id: number, status: number) {
  return requestClient.put(`/contracts/${id}/status`, { status });
}

async function deleteContract(id: number) {
  return requestClient.delete(`/contracts/${id}`);
}

export {
  changeContractStatus,
  createContract,
  deleteContract,
  getContractDetail,
  getContractList,
  updateContract,
};
