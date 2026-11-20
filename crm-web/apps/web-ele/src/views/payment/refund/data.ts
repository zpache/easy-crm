import type { VbenFormSchema } from '#/adapter/form';
import type { OnActionClickFn, VxeTableGridColumns } from '#/adapter/vxe-table';
import type { PaymentApi } from '#/api/crm/payment';

import { z } from '#/adapter/form';
import { usePermission } from '#/composables/use-permission';
import { REFUND_PERMISSIONS } from '#/constants/crm-batch3-permissions';

import {
  customerSelectProps,
  orderSelectProps,
  REFUND_STATUSES,
} from '../shared/options';

/** 退款申请表单 */
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
      label: '退款金额',
      rules: z.number({ message: '退款金额不能为空' }),
    },
    {
      component: 'Input',
      fieldName: 'refundNo',
      label: '退款单号',
      help: '留空由系统自动生成',
    },
    {
      component: 'DatePicker',
      componentProps: { class: 'w-full', valueFormat: 'YYYY-MM-DD' },
      fieldName: 'refundDate',
      label: '退款日期',
    },
    {
      component: 'Textarea',
      componentProps: { maxLength: 500, rows: 2, showCount: true },
      fieldName: 'reason',
      label: '退款原因',
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
      component: 'Select',
      componentProps: () => ({ allowClear: true, ...orderSelectProps() }),
      fieldName: 'orderId',
      label: '关联订单',
    },
    {
      component: 'Select',
      componentProps: { allowClear: true, options: [...REFUND_STATUSES] },
      fieldName: 'status',
      label: '状态',
    },
  ];
}

const TAG_TYPES: Record<number, string> = {
  0: 'warning',
  1: 'primary',
  2: 'success',
  3: 'danger',
};

/** 退款列表列 */
export function useColumns<T = PaymentApi.Refund>(
  onActionClick: OnActionClickFn<T>,
): VxeTableGridColumns {
  const { can } = usePermission();
  const canUpdate = can(REFUND_PERMISSIONS.update);
  const canDelete = can(REFUND_PERMISSIONS.delete);
  return [
    { field: 'id', title: 'ID', width: 80 },
    { field: 'refundNo', minWidth: 170, title: '退款单号' },
    { field: 'orderId', title: '订单 ID', width: 100 },
    { field: 'customerId', title: '客户 ID', width: 100 },
    { field: 'amount', title: '退款金额', width: 130 },
    { field: 'reason', minWidth: 160, title: '退款原因' },
    {
      cellRender: {
        name: 'CellTag',
        options: REFUND_STATUSES.map((s) => ({
          label: s.label,
          type: TAG_TYPES[s.value],
          value: s.value,
        })),
      },
      field: 'status',
      title: '状态',
      width: 100,
    },
    { field: 'refundDate', title: '退款日期', width: 120 },
    { field: 'createTime', title: '创建时间', width: 165 },
    {
      align: 'center',
      cellRender: {
        attrs: {
          nameField: 'refundNo',
          nameTitle: '退款',
          onClick: onActionClick,
        },
        name: 'CellOperation',
        options: [
          {
            code: 'edit',
            show: (row: PaymentApi.Refund) => canUpdate && row.status === 0,
          },
          {
            code: 'approve',
            show: (row: PaymentApi.Refund) => canUpdate && row.status === 0,
            text: '审核通过',
          },
          {
            code: 'refunded',
            show: (row: PaymentApi.Refund) => canUpdate && row.status === 1,
            text: '标记已退款',
          },
          {
            code: 'reject',
            show: (row: PaymentApi.Refund) => canUpdate && row.status === 0,
            text: '驳回',
          },
          {
            code: 'delete',
            show: (row: PaymentApi.Refund) => canDelete && row.status !== 2,
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
