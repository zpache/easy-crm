import type { VbenFormSchema } from '#/adapter/form';
import type { OnActionClickFn, VxeTableGridColumns } from '#/adapter/vxe-table';
import type { CustomerApi } from '#/api/crm/customer';

import { z } from '#/adapter/form';
import {
  CUSTOMER_INDUSTRY,
  CUSTOMER_LEVEL,
  CUSTOMER_REGION,
  CUSTOMER_SCALE,
  CUSTOMER_STATUS,
} from '#/api/crm/customer';
import { usePermission } from '#/composables/use-permission';
import { CUSTOMER_PERMISSIONS } from '#/constants/permissions';

/** 客户分配权限码（与后端 @PreAuthorize 一致，由统一整合方登记到 permissions.ts） */
export const CUSTOMER_ASSIGN_PERMISSION = 'crm:customer:assign';

/**
 * 客户创建 / 编辑表单
 */
export function useFormSchema(): VbenFormSchema[] {
  return [
    {
      component: 'Input',
      fieldName: 'name',
      label: '客户名称',
      rules: z
        .string()
        .min(1, '客户名称不能为空')
        .max(128, '客户名称不能超过 128 个字符'),
    },
    {
      component: 'Input',
      fieldName: 'creditCode',
      componentProps: { placeholder: '与客户名称组合查重' },
      label: '统一社会信用代码',
    },
    {
      component: 'Select',
      componentProps: { allowClear: true, class: 'w-full', options: [...CUSTOMER_INDUSTRY] },
      fieldName: 'industry',
      label: '行业',
    },
    {
      component: 'Select',
      componentProps: { allowClear: true, class: 'w-full', options: [...CUSTOMER_SCALE] },
      fieldName: 'scale',
      label: '规模',
    },
    {
      component: 'Select',
      componentProps: { allowClear: true, class: 'w-full', options: [...CUSTOMER_REGION] },
      fieldName: 'region',
      label: '区域',
    },
    {
      component: 'Select',
      componentProps: { allowClear: true, class: 'w-full', options: [...CUSTOMER_LEVEL] },
      defaultValue: 3,
      fieldName: 'level',
      label: '客户级别',
    },
    {
      component: 'Textarea',
      componentProps: { maxLength: 500, rows: 2, showCount: true },
      fieldName: 'remark',
      label: '备注',
    },
  ];
}

/**
 * 查询表单
 */
export function useGridFormSchema(): VbenFormSchema[] {
  return [
    { component: 'Input', fieldName: 'name', label: '客户名称' },
    {
      component: 'Select',
      componentProps: { allowClear: true, options: [...CUSTOMER_INDUSTRY] },
      fieldName: 'industry',
      label: '行业',
    },
    {
      component: 'Select',
      componentProps: { allowClear: true, options: [...CUSTOMER_STATUS] },
      fieldName: 'status',
      label: '状态',
    },
    {
      component: 'Select',
      componentProps: {
        allowClear: true,
        options: [{ label: '公海', value: -1 }],
      },
      fieldName: 'ownerId',
      label: '负责人',
    },
  ];
}

/**
 * 客户表格列
 */
export function useColumns<T = CustomerApi.Customer>(
  onActionClick: OnActionClickFn<T>,
): VxeTableGridColumns {
  const { can } = usePermission();
  const canUpdate = can(CUSTOMER_PERMISSIONS.update);
  const canMerge = can(CUSTOMER_PERMISSIONS.merge);
  const canPool = can(CUSTOMER_PERMISSIONS.toPool);
  const canClaim = can(CUSTOMER_PERMISSIONS.claim);
  const canAssign = can(CUSTOMER_ASSIGN_PERMISSION);
  return [
    { type: 'checkbox', width: 50, fixed: 'left' },
    { field: 'name', minWidth: 160, title: '客户名称' },
    { field: 'creditCode', minWidth: 150, title: '信用代码' },
    { field: 'industryName', title: '行业', width: 100 },
    { field: 'scaleName', title: '规模', width: 100 },
    { field: 'levelName', title: '级别', width: 100 },
    {
      cellRender: {
        name: 'CellTag',
        options: [
          { label: '正常', type: 'success', value: 1 },
          { label: '公海', type: 'warning', value: 2 },
          { label: '已合并', type: 'danger', value: 3 },
          { label: '成交客户', type: 'primary', value: 4 },
        ],
      },
      field: 'status',
      title: '状态',
      width: 90,
    },
    { field: 'ownerName', title: '负责人', width: 110 },
    { field: 'lastFollowTime', title: '最近跟进', width: 160 },
    {
      align: 'center',
      cellRender: {
        attrs: {
          nameField: 'name',
          nameTitle: '客户',
          onClick: onActionClick,
        },
        name: 'CellOperation',
        options: [
          { code: 'view360', text: '360', show: () => true },
          { code: 'edit', show: (row: CustomerApi.Customer) => canUpdate && row.status !== 3 },
          {
            code: 'assign',
            show: (row: CustomerApi.Customer) => canAssign && row.status !== 3,
            text: '分配',
          },
          {
            code: 'merge',
            show: (row: CustomerApi.Customer) => canMerge && row.status !== 3,
            text: '合并',
          },
          {
            code: 'toPool',
            show: (row: CustomerApi.Customer) => canPool && row.status === 1,
            text: '放入公海',
          },
          {
            code: 'claim',
            show: (row: CustomerApi.Customer) => canClaim && row.status === 2,
            text: '领取',
          },
        ],
      },
      field: 'operation',
      fixed: 'right',
      title: '操作',
      width: 280,
    },
  ];
}
