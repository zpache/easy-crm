import type { VbenFormSchema } from '#/adapter/form';
import type { OnActionClickFn, VxeTableGridColumns } from '#/adapter/vxe-table';
import type { ProductApi } from '#/api/crm/product';

import { z } from '#/adapter/form';
import { PRODUCT_STATUSES } from '#/api/crm/product';
import { usePermission } from '#/composables/use-permission';
import { PRODUCT_PERMISSIONS } from '#/constants/crm-batch3-permissions';

const STATUS_TAG_TYPES: Record<number, string> = {
  0: 'info',
  1: 'success',
  2: 'danger',
};

/** 产品创建 / 编辑表单 */
export function useFormSchema(): VbenFormSchema[] {
  return [
    {
      component: 'Input',
      fieldName: 'code',
      label: '产品编码',
      rules: z
        .string()
        .min(1, '产品编码不能为空')
        .max(64, '产品编码不能超过 64 个字符'),
    },
    {
      component: 'Input',
      fieldName: 'name',
      label: '产品名称',
      rules: z
        .string()
        .min(1, '产品名称不能为空')
        .max(128, '产品名称不能超过 128 个字符'),
    },
    { component: 'Input', fieldName: 'category', label: '分类' },
    { component: 'Input', fieldName: 'specification', label: '规格' },
    { component: 'Input', fieldName: 'unit', label: '单位' },
    {
      component: 'Select',
      componentProps: { class: 'w-full', options: [...PRODUCT_STATUSES] },
      fieldName: 'status',
      label: '状态',
    },
    {
      component: 'InputNumber',
      componentProps: { class: 'w-full', min: 0, precision: 2 },
      fieldName: 'costPrice',
      label: '成本价',
    },
    {
      component: 'InputNumber',
      componentProps: { class: 'w-full', min: 0, precision: 2 },
      fieldName: 'standardPrice',
      label: '标准价',
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
    { component: 'Input', fieldName: 'keyword', label: '名称/编码' },
    { component: 'Input', fieldName: 'category', label: '分类' },
    {
      component: 'Select',
      componentProps: { allowClear: true, options: [...PRODUCT_STATUSES] },
      fieldName: 'status',
      label: '状态',
    },
  ];
}

/** 产品表格列 */
export function useColumns<T = ProductApi.Product>(
  onActionClick: OnActionClickFn<T>,
): VxeTableGridColumns {
  const { can } = usePermission();
  const canUpdate = can(PRODUCT_PERMISSIONS.update);
  const canDelete = can(PRODUCT_PERMISSIONS.delete);
  return [
    { field: 'code', minWidth: 130, title: '产品编码' },
    { field: 'name', minWidth: 150, title: '产品名称' },
    { field: 'category', title: '分类', width: 110 },
    { field: 'specification', title: '规格', width: 120 },
    { field: 'unit', title: '单位', width: 80 },
    {
      cellRender: {
        name: 'CellTag',
        options: PRODUCT_STATUSES.map((s) => ({
          label: s.label,
          type: STATUS_TAG_TYPES[s.value],
          value: s.value,
        })),
      },
      field: 'status',
      title: '状态',
      width: 100,
    },
    { field: 'costPrice', title: '成本价', width: 120 },
    { field: 'standardPrice', title: '标准价', width: 120 },
    { field: 'createTime', title: '创建时间', width: 165 },
    {
      align: 'center',
      cellRender: {
        attrs: {
          nameField: 'name',
          nameTitle: '产品',
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
