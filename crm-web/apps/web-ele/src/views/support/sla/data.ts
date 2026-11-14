import type { VbenFormSchema } from '#/adapter/form';
import type { OnActionClickFn, VxeTableGridColumns } from '#/adapter/vxe-table';
import type { SlaApi } from '#/api/crm/support';

import { z } from '#/adapter/form';
import { SUPPORT_PERMISSIONS, TICKET_PRIORITIES, TICKET_TYPES } from '#/api/crm/support';
import { usePermission } from '#/composables/use-permission';

/** SLA 策略新增 / 编辑表单 */
export function useFormSchema(): VbenFormSchema[] {
  return [
    {
      component: 'Select',
      componentProps: { class: 'w-full', options: [...TICKET_TYPES] },
      fieldName: 'ticketType',
      label: '工单类型',
      rules: z.string().min(1, '请选择工单类型'),
    },
    {
      component: 'Select',
      componentProps: { class: 'w-full', options: [...TICKET_PRIORITIES] },
      fieldName: 'priority',
      label: '优先级',
      rules: z.string().min(1, '请选择优先级'),
    },
    {
      component: 'InputNumber',
      componentProps: { class: 'w-full', min: 1 },
      fieldName: 'responseHours',
      label: '响应时限（小时）',
      rules: z.number({ message: '请填写响应小时数' }).min(1),
    },
    {
      component: 'InputNumber',
      componentProps: { class: 'w-full', min: 1 },
      fieldName: 'resolveHours',
      label: '解决时限（小时）',
      rules: z.number({ message: '请填写解决小时数' }).min(1),
    },
    {
      component: 'Select',
      componentProps: {
        class: 'w-full',
        options: [
          { label: '启用', value: 1 },
          { label: '停用', value: 0 },
        ],
      },
      fieldName: 'enabled',
      label: '状态',
    },
  ];
}

/** 查询表单 */
export function useGridFormSchema(): VbenFormSchema[] {
  return [
    {
      component: 'Select',
      componentProps: { allowClear: true, options: [...TICKET_TYPES] },
      fieldName: 'ticketType',
      label: '工单类型',
    },
    {
      component: 'Select',
      componentProps: { allowClear: true, options: [...TICKET_PRIORITIES] },
      fieldName: 'priority',
      label: '优先级',
    },
    {
      component: 'Select',
      componentProps: {
        allowClear: true,
        options: [
          { label: '启用', value: 1 },
          { label: '停用', value: 0 },
        ],
      },
      fieldName: 'enabled',
      label: '状态',
    },
  ];
}

/** SLA 策略表格列 */
export function useColumns<T = SlaApi.SlaPolicy>(
  onActionClick: OnActionClickFn<T>,
): VxeTableGridColumns {
  const { can } = usePermission();
  const canUpdate = can(SUPPORT_PERMISSIONS.slaUpdate);
  const canDelete = can(SUPPORT_PERMISSIONS.slaDelete);
  return [
    { field: 'ticketTypeName', minWidth: 130, title: '工单类型' },
    { field: 'priorityName', title: '优先级', width: 100 },
    { field: 'responseHours', title: '响应时限(h)', width: 130 },
    { field: 'resolveHours', title: '解决时限(h)', width: 130 },
    {
      cellRender: {
        name: 'CellTag',
        options: [
          { label: '启用', type: 'success', value: 1 },
          { label: '停用', type: 'info', value: 0 },
        ],
      },
      field: 'enabled',
      title: '状态',
      width: 100,
    },
    { field: 'updateTime', title: '更新时间', width: 165 },
    {
      align: 'center',
      cellRender: {
        attrs: {
          nameField: 'ticketTypeName',
          nameTitle: '策略',
          onClick: onActionClick,
        },
        name: 'CellOperation',
        options: [
          { code: 'edit', show: canUpdate },
          { code: 'delete', show: canDelete, text: '删除' },
        ],
      },
      field: 'operation',
      fixed: 'right',
      title: '操作',
      width: 150,
    },
  ];
}
