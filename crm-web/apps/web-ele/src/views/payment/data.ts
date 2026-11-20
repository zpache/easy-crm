import type { VbenFormSchema } from '#/adapter/form';
import type { OnActionClickFn, VxeTableGridColumns } from '#/adapter/vxe-table';
import type { PaymentApi } from '#/api/crm/payment';

import { z } from '#/adapter/form';
import {
  PLAN_STATUSES,
  WRITEOFF_STATUSES,
} from '#/api/crm/payment';
import { usePermission } from '#/composables/use-permission';
import { loadOrderOptions, orderSelectProps } from '../shared/options';
import {
  PAYMENT_PERMISSIONS,
  PAYMENT_PLAN_PERMISSIONS,
} from '#/constants/crm-batch3-permissions';

// ============================ 回款计划 ============================

export function usePlanFormSchema(): VbenFormSchema[] {
  return [
    {
      component: 'Select',
      componentProps: () => orderSelectProps(),
      fieldName: 'orderId',
      label: '关联订单',
      rules: z.number({ message: '请选择关联订单' }),
    },
    {
      component: 'InputNumber',
      componentProps: { class: 'w-full', min: 1, precision: 0 },
      fieldName: 'periodNo',
      label: '期次',
      rules: z.number({ message: '期次不能为空' }),
    },
    {
      component: 'DatePicker',
      componentProps: { class: 'w-full', valueFormat: 'YYYY-MM-DD' },
      fieldName: 'planDate',
      label: '计划回款日期',
    },
    {
      component: 'InputNumber',
      componentProps: { class: 'w-full', min: 0, precision: 2 },
      fieldName: 'planAmount',
      label: '计划金额',
      rules: z.number({ message: '计划金额不能为空' }),
    },
    {
      component: 'Textarea',
      componentProps: { maxLength: 500, rows: 2, showCount: true },
      fieldName: 'remark',
      label: '备注',
    },
  ];
}

export function usePlanGridFormSchema(): VbenFormSchema[] {
  return [
    {
      component: 'Select',
      componentProps: () => ({ allowClear: true, ...orderSelectProps() }),
      fieldName: 'orderId',
      label: '关联订单',
    },
    {
      component: 'Select',
      componentProps: { allowClear: true, options: [...PLAN_STATUSES] },
      fieldName: 'status',
      label: '核销状态',
    },
  ];
}

export function usePlanColumns<T = PaymentApi.PaymentPlan>(
  onActionClick: OnActionClickFn<T>,
): VxeTableGridColumns {
  const { can } = usePermission();
  const canUpdate = can(PAYMENT_PLAN_PERMISSIONS.update);
  const canDelete = can(PAYMENT_PLAN_PERMISSIONS.delete);
  return [
    { field: 'id', title: 'ID', width: 80 },
    { field: 'orderId', title: '订单 ID', width: 110 },
    { field: 'periodNo', title: '期次', width: 80 },
    { field: 'planDate', title: '计划日期', width: 120 },
    { field: 'planAmount', title: '计划金额', width: 120 },
    { field: 'receivedAmount', title: '已核销', width: 120 },
    {
      cellRender: {
        name: 'CellTag',
        options: PLAN_STATUSES.map((s) => ({ label: s.label, value: s.value })),
      },
      field: 'status',
      title: '核销状态',
      width: 110,
    },
    {
      align: 'center',
      cellRender: {
        attrs: { nameField: 'id', nameTitle: '计划', onClick: onActionClick },
        name: 'CellOperation',
        options: [
          {
            code: 'edit',
            show: (row: PaymentApi.PaymentPlan) => canUpdate && row.status === 0,
          },
          {
            code: 'delete',
            show: (row: PaymentApi.PaymentPlan) => canDelete && row.status === 0,
          },
        ],
      },
      field: 'operation',
      fixed: 'right',
      title: '操作',
      width: 140,
    },
  ];
}

// ============================ 回款记录 ============================

export function usePaymentFormSchema(): VbenFormSchema[] {
  return [
    {
      component: 'Select',
      componentProps: () => orderSelectProps(),
      fieldName: 'orderId',
      label: '关联订单',
      rules: z.number({ message: '订单 ID 不能为空' }),
    },
    {
      component: 'InputNumber',
      componentProps: { class: 'w-full', min: 0, precision: 0 },
      fieldName: 'planId',
      label: '回款计划 ID',
      help: '可空，核销时也可再指定',
    },
    {
      component: 'InputNumber',
      componentProps: { class: 'w-full', min: 0, precision: 2 },
      fieldName: 'amount',
      label: '实收金额',
      rules: z.number({ message: '实收金额不能为空' }),
    },
    {
      component: 'DatePicker',
      componentProps: { class: 'w-full', valueFormat: 'YYYY-MM-DD' },
      fieldName: 'paymentDate',
      label: '回款日期',
    },
    {
      component: 'Textarea',
      componentProps: { maxLength: 500, rows: 2, showCount: true },
      fieldName: 'remark',
      label: '备注',
    },
  ];
}

export function usePaymentGridFormSchema(): VbenFormSchema[] {
  return [
    {
      component: 'Select',
      componentProps: () => ({ allowClear: true, ...orderSelectProps() }),
      fieldName: 'orderId',
      label: '关联订单',
    },
    {
      component: 'Select',
      componentProps: { allowClear: true, options: [...WRITEOFF_STATUSES] },
      fieldName: 'writeoffStatus',
      label: '核销状态',
    },
  ];
}

export function usePaymentColumns<T = PaymentApi.Payment>(
  onActionClick: OnActionClickFn<T>,
): VxeTableGridColumns {
  const { can } = usePermission();
  const canUpdate = can(PAYMENT_PERMISSIONS.update);
  const canDelete = can(PAYMENT_PERMISSIONS.delete);
  const canWriteoff = can(PAYMENT_PERMISSIONS.writeoff);
  return [
    { field: 'id', title: 'ID', width: 80 },
    { field: 'orderId', title: '订单 ID', width: 110 },
    { field: 'planId', title: '计划 ID', width: 100 },
    { field: 'amount', title: '实收金额', width: 120 },
    { field: 'paymentDate', title: '回款日期', width: 120 },
    {
      cellRender: {
        name: 'CellTag',
        options: WRITEOFF_STATUSES.map((s) => ({
          label: s.label,
          value: s.value,
        })),
      },
      field: 'writeoffStatus',
      title: '核销状态',
      width: 110,
    },
    {
      align: 'center',
      cellRender: {
        attrs: { nameField: 'id', nameTitle: '回款', onClick: onActionClick },
        name: 'CellOperation',
        options: [
          {
            code: 'edit',
            show: (row: PaymentApi.Payment) => canUpdate && row.writeoffStatus === 0,
          },
          {
            code: 'delete',
            show: (row: PaymentApi.Payment) => canDelete && row.writeoffStatus === 0,
          },
          {
            code: 'writeoff',
            show: (row: PaymentApi.Payment) => canWriteoff && row.writeoffStatus === 0,
            text: '核销',
          },
        ],
      },
      field: 'operation',
      fixed: 'right',
      title: '操作',
      width: 200,
    },
  ];
}
