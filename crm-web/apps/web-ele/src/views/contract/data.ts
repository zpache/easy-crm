import type { VbenFormSchema } from '#/adapter/form';
import type { OnActionClickFn, VxeTableGridColumns } from '#/adapter/vxe-table';
import type { ContractApi } from '#/api/crm/contract';

import { ref } from 'vue';

import { z } from '#/adapter/form';
import { getCustomerList } from '#/api/crm/customer';
import { CONTRACT_STATUSES } from '#/api/crm/contract';
import { usePermission } from '#/composables/use-permission';
import { CONTRACT_PERMISSIONS } from '#/constants/crm-batch3-permissions';

const STATUS_TAG_TYPES: Record<number, string> = {
  0: 'info',
  1: 'success',
  2: 'primary',
  3: 'danger',
};

/** 客户候选选项（list.vue / form.vue 启动时各加载一次） */
export const customerOptions = ref<Array<{ id: number; name: string }>>([]);

export async function loadCustomerOptions() {
  const result = await getCustomerList({ pageSize: 100 });
  customerOptions.value = result.items.map((item: any) => ({
    id: item.id,
    name: item.name,
  }));
}

function customerSelectProps() {
  return {
    class: 'w-full',
    filterOption: (input: string, option: any) => option.label.includes(input),
    options: customerOptions.value.map((c) => ({ label: c.name, value: c.id })),
    showSearch: true,
  };
}

/** 合同创建 / 编辑表单 */
export function useFormSchema(): VbenFormSchema[] {
  return [
    {
      component: 'Input',
      fieldName: 'contractNo',
      label: '合同编号',
      help: '留空由系统自动生成',
    },
    {
      component: 'Select',
      componentProps: () => customerSelectProps(),
      fieldName: 'customerId',
      label: '所属客户',
      rules: z.number({ message: '请选择所属客户' }),
    },
    {
      component: 'InputNumber',
      componentProps: { class: 'w-full', min: 0, precision: 0 },
      fieldName: 'opportunityId',
      label: '来源商机 ID',
    },
    {
      component: 'InputNumber',
      componentProps: { class: 'w-full', min: 0, precision: 2 },
      fieldName: 'amount',
      label: '合同金额',
      rules: z.number({ message: '合同金额不能为空' }),
    },
    {
      component: 'DatePicker',
      componentProps: { class: 'w-full', valueFormat: 'YYYY-MM-DD' },
      fieldName: 'signDate',
      label: '签约日期',
    },
    {
      component: 'Select',
      componentProps: { class: 'w-full', options: [...CONTRACT_STATUSES] },
      fieldName: 'status',
      label: '状态',
    },
    {
      component: 'Textarea',
      componentProps: { maxLength: 500, rows: 2, showCount: true },
      fieldName: 'remark',
      label: '备注',
    },
  ];
}

/** 查询表单 */
export function useGridFormSchema(): VbenFormSchema[] {
  return [
    { component: 'Input', fieldName: 'contractNo', label: '合同编号' },
    {
      component: 'Select',
      componentProps: () => ({ allowClear: true, ...customerSelectProps() }),
      fieldName: 'customerId',
      label: '所属客户',
    },
    {
      component: 'Select',
      componentProps: { allowClear: true, options: [...CONTRACT_STATUSES] },
      fieldName: 'status',
      label: '状态',
    },
  ];
}

/** 合同表格列 */
export function useColumns<T = ContractApi.Contract>(
  onActionClick: OnActionClickFn<T>,
): VxeTableGridColumns {
  const { can } = usePermission();
  const canUpdate = can(CONTRACT_PERMISSIONS.update);
  const canDelete = can(CONTRACT_PERMISSIONS.delete);
  return [
    { field: 'contractNo', minWidth: 160, title: '合同编号' },
    { field: 'customerName', minWidth: 140, title: '客户' },
    { field: 'amount', title: '合同金额', width: 130 },
    { field: 'signDate', title: '签约日期', width: 120 },
    {
      cellRender: {
        name: 'CellTag',
        options: CONTRACT_STATUSES.map((s) => ({
          label: s.label,
          type: STATUS_TAG_TYPES[s.value],
          value: s.value,
        })),
      },
      field: 'status',
      title: '状态',
      width: 100,
    },
    { field: 'createTime', title: '创建时间', width: 165 },
    {
      align: 'center',
      cellRender: {
        attrs: {
          nameField: 'contractNo',
          nameTitle: '合同',
          onClick: onActionClick,
        },
        name: 'CellOperation',
        options: [
          { code: 'edit', show: canUpdate },
          { code: 'delete', show: canDelete },
        ],
      },
      field: 'operation',
      fixed: 'right',
      title: '操作',
      width: 140,
    },
  ];
}
