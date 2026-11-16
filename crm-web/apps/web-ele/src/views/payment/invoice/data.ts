import type { VbenFormSchema } from '#/adapter/form';
import type { OnActionClickFn, VxeTableGridColumns } from '#/adapter/vxe-table';
import type { PaymentApi } from '#/api/crm/payment';

import { z } from '#/adapter/form';
import { usePermission } from '#/composables/use-permission';
import { INVOICE_PERMISSIONS } from '#/constants/crm-batch3-permissions';

import {
  customerSelectProps,
  INVOICE_STATUSES,
  orderSelectProps,
} from '../shared/options';

/** 开票申请表单 */
export function useFormSchema(): VbenFormSchema[] {
  return [
    {
      component: 'Select',
      componentProps: () => orderSelectProps(),
      fieldName: 'orderId',
      label: '关联订单',
      rules: z.number({ message: '请选择订单' }),
    },
    {
      component: 'Select',
      componentProps: () => customerSelectProps(),
      fieldName: 'customerId',
      label: '客户',
      help: '选择订单后自动带出，可手工修正',
      rules: z.number({ message: '请选择客户' }),
    },
    {
      component: 'InputNumber',
      componentProps: { class: 'w-full', min: 0, precision: 2 },
      fieldName: 'amount',
      label: '开票金额',
      rules: z.number({ message: '开票金额不能为空' }),
    },
    {
      component: 'Input',
      fieldName: 'invoiceNo',
      label: '发票号',
      help: '开票后回填',
    },
    { component: 'Input', fieldName: 'title', label: '发票抬头' },
    { component: 'Input', fieldName: 'taxNo', label: '税号' },
    {
      component: 'DatePicker',
      componentProps: { class: 'w-full', valueFormat: 'YYYY-MM-DD' },
      fieldName: 'applyDate',
      label: '申请日期',
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
    {
      component: 'InputNumber',
      componentProps: { class: 'w-full', min: 0, precision: 0 },
      fieldName: 'orderId',
      label: '订单 ID',
    },
    {
      component: 'Select',
      componentProps: { allowClear: true, options: [...INVOICE_STATUSES] },
      fieldName: 'status',
      label: '状态',
    },
  ];
}

const TAG_TYPES: Record<number, string> = {
  0: 'warning',
  1: 'success',
  2: 'info',
};

/** 开票列表列 */
export function useColumns<T = PaymentApi.Invoice>(
  onActionClick: OnActionClickFn<T>,
): VxeTableGridColumns {
  const { can } = usePermission();
  const canUpdate = can(INVOICE_PERMISSIONS.update);
  const canDelete = can(INVOICE_PERMISSIONS.delete);
  return [
    { field: 'id', title: 'ID', width: 80 },
    { field: 'orderId', title: '订单 ID', width: 100 },
    { field: 'customerId', title: '客户 ID', width: 100 },
    { field: 'invoiceNo', minWidth: 150, title: '发票号' },
    { field: 'amount', title: '开票金额', width: 130 },
    { field: 'title', minWidth: 150, title: '发票抬头' },
    {
      cellRender: {
        name: 'CellTag',
        options: INVOICE_STATUSES.map((s) => ({
          label: s.label,
          type: TAG_TYPES[s.value],
          value: s.value,
        })),
      },
      field: 'status',
      title: '状态',
      width: 100,
    },
    { field: 'applyDate', title: '申请日期', width: 120 },
    { field: 'createTime', title: '创建时间', width: 165 },
    {
      align: 'center',
      cellRender: {
        attrs: {
          nameField: 'invoiceNo',
          nameTitle: '开票',
          onClick: onActionClick,
        },
        name: 'CellOperation',
        options: [
          {
            code: 'edit',
            show: (row: PaymentApi.Invoice) => canUpdate && row.status === 0,
          },
          {
            code: 'issue',
            show: (row: PaymentApi.Invoice) => canUpdate && row.status === 0,
            text: '确认开票',
          },
          {
            code: 'void',
            show: (row: PaymentApi.Invoice) => canUpdate && row.status === 1,
            text: '作废',
          },
          {
            code: 'delete',
            show: (row: PaymentApi.Invoice) => canDelete && row.status !== 1,
          },
        ],
      },
      field: 'operation',
      fixed: 'right',
      title: '操作',
      width: 260,
    },
  ];
}
