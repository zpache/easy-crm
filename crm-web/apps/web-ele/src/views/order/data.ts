import type { VbenFormSchema } from '#/adapter/form';
import type { OnActionClickFn, VxeTableGridColumns } from '#/adapter/vxe-table';
import type { OrderApi } from '#/api/crm/order';

import { ref } from 'vue';

import { z } from '#/adapter/form';
import { getCustomerList } from '#/api/crm/customer';
import { ORDER_STATUSES } from '#/api/crm/order';
import { usePermission } from '#/composables/use-permission';
import { CRM_ORDER_PERMISSIONS } from '#/constants/crm-batch3-permissions';

const STATUS_TAG_TYPES: Record<number, string> = {
  0: 'info',
  1: 'warning',
  2: 'primary',
  3: 'success',
  4: 'success',
  9: 'danger',
};

/** 客户候选选项 */
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

/**
 * 订单创建 / 编辑表单（不含明细行，明细由抽屉内的动态表格编辑）。
 * 明细汇总必须等于 totalAmount，保存前在 form.vue 中校验。
 */
export function useFormSchema(): VbenFormSchema[] {
  return [
    {
      component: 'Input',
      fieldName: 'orderNo',
      label: '订单编号',
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
      componentProps: { class: 'w-full', min: 0, precision: 0 },
      fieldName: 'contractId',
      label: '关联合同 ID',
    },
    {
      component: 'InputNumber',
      componentProps: { class: 'w-full', min: 0, precision: 2 },
      fieldName: 'totalAmount',
      label: '总金额',
      rules: z.number({ message: '总金额不能为空' }),
    },
    {
      component: 'DatePicker',
      componentProps: { class: 'w-full', valueFormat: 'YYYY-MM-DD' },
      fieldName: 'shipDate',
      label: '发货日期',
    },
    {
      component: 'DatePicker',
      componentProps: { class: 'w-full', valueFormat: 'YYYY-MM-DD' },
      fieldName: 'deliveryDate',
      label: '交付日期',
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
    { component: 'Input', fieldName: 'orderNo', label: '订单编号' },
    {
      component: 'Select',
      componentProps: () => ({ allowClear: true, ...customerSelectProps() }),
      fieldName: 'customerId',
      label: '所属客户',
    },
    {
      component: 'Select',
      componentProps: { allowClear: true, options: [...ORDER_STATUSES] },
      fieldName: 'status',
      label: '状态',
    },
  ];
}

/** 订单表格列 */
export function useColumns<T = OrderApi.Order>(
  onActionClick: OnActionClickFn<T>,
): VxeTableGridColumns {
  const { can } = usePermission();
  const canUpdate = can(CRM_ORDER_PERMISSIONS.update);
  const canDelete = can(CRM_ORDER_PERMISSIONS.delete);
  const canStatus = can(CRM_ORDER_PERMISSIONS.status);
  return [
    { field: 'orderNo', minWidth: 170, title: '订单编号' },
    { field: 'customerName', minWidth: 140, title: '客户' },
    { field: 'totalAmount', title: '总金额', width: 130 },
    {
      cellRender: {
        name: 'CellTag',
        options: ORDER_STATUSES.map((s) => ({
          label: s.label,
          type: STATUS_TAG_TYPES[s.value],
          value: s.value,
        })),
      },
      field: 'status',
      title: '状态',
      width: 100,
    },
    { field: 'deliveryDate', title: '交付日期', width: 120 },
    { field: 'createTime', title: '创建时间', width: 165 },
    {
      align: 'center',
      cellRender: {
        attrs: {
          nameField: 'orderNo',
          nameTitle: '订单',
          onClick: onActionClick,
        },
        name: 'CellOperation',
        options: [
          { code: 'detail', text: '详情' },
          {
            code: 'edit',
            show: (row: OrderApi.Order) => canUpdate && row.status !== 4 && row.status !== 9,
          },
          {
            code: 'status',
            show: (row: OrderApi.Order) => canStatus && row.status !== 4 && row.status !== 9,
            text: '推进状态',
          },
          {
            code: 'cancel',
            show: (row: OrderApi.Order) => canStatus && row.status !== 4 && row.status !== 9,
            text: '取消订单',
          },
          {
            code: 'delete',
            show: (row: OrderApi.Order) => canDelete && row.status !== 4 && row.status !== 9,
          },
        ],
      },
      field: 'operation',
      fixed: 'right',
      title: '操作',
      width: 320,
    },
  ];
}
