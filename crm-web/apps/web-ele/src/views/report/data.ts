import type { VbenFormSchema } from '#/adapter/form';
import type { OnActionClickFn, VxeTableGridColumns } from '#/adapter/vxe-table';
import type { ReportApi } from '#/api/crm/report';

import { z } from '#/adapter/form';
import { usePermission } from '#/composables/use-permission';
import { SALES_TARGET_PERMISSIONS } from '#/constants/crm-batch3-permissions';

/** 销售目标创建 / 编辑表单 */
export function useFormSchema(): VbenFormSchema[] {
  return [
    {
      component: 'Input',
      fieldName: 'targetMonth',
      label: '目标年月',
      help: '格式 YYYY-MM',
      rules: z
        .string()
        .min(1, '目标年月不能为空')
        .regex(/^\d{4}-(0[1-9]|1[0-2])$/, '格式必须为 YYYY-MM'),
    },
    {
      component: 'InputNumber',
      componentProps: { class: 'w-full', min: 0, precision: 0 },
      fieldName: 'deptId',
      label: '部门 ID',
      help: '留空表示不限部门',
    },
    {
      component: 'InputNumber',
      componentProps: { class: 'w-full', min: 0, precision: 0 },
      fieldName: 'ownerId',
      label: '负责人 ID',
      help: '留空表示部门整体目标',
    },
    {
      component: 'InputNumber',
      componentProps: { class: 'w-full', min: 0, precision: 2 },
      fieldName: 'targetAmount',
      label: '目标金额',
      rules: z.number({ message: '目标金额不能为空' }),
    },
    {
      component: 'Textarea',
      componentProps: { maxLength: 500, rows: 2, showCount: true },
      fieldName: 'remark',
      label: '备注',
    },
  ];
}

/** 销售目标查询表单 */
export function useGridFormSchema(): VbenFormSchema[] {
  return [
    { component: 'Input', fieldName: 'targetMonth', label: '目标年月' },
    {
      component: 'InputNumber',
      componentProps: { class: 'w-full', min: 0, precision: 0 },
      fieldName: 'deptId',
      label: '部门 ID',
    },
    {
      component: 'InputNumber',
      componentProps: { class: 'w-full', min: 0, precision: 0 },
      fieldName: 'ownerId',
      label: '负责人 ID',
    },
  ];
}

/** 销售目标表格列 */
export function useColumns<T = ReportApi.SalesTarget>(
  onActionClick: OnActionClickFn<T>,
): VxeTableGridColumns {
  const { can } = usePermission();
  const canUpdate = can(SALES_TARGET_PERMISSIONS.update);
  const canDelete = can(SALES_TARGET_PERMISSIONS.delete);
  return [
    { field: 'targetMonth', title: '目标年月', width: 120 },
    { field: 'deptName', minWidth: 130, title: '部门' },
    { field: 'ownerName', minWidth: 130, title: '负责人' },
    { field: 'targetAmount', title: '目标金额', width: 140 },
    { field: 'remark', minWidth: 140, title: '备注' },
    { field: 'createTime', title: '创建时间', width: 165 },
    {
      align: 'center',
      cellRender: {
        attrs: {
          nameField: 'targetMonth',
          nameTitle: '目标',
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
