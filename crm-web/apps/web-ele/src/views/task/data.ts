import type { VbenFormSchema } from '#/adapter/form';
import type { OnActionClickFn, VxeTableGridColumns } from '#/adapter/vxe-table';
import type { TaskApi } from '#/api/crm/task';

import { z } from '#/adapter/form';
import { sourceLabel, TASK_STATUSES } from '#/api/crm/task';
import { usePermission } from '#/composables/use-permission';
import { TASK_PERMISSIONS } from '#/constants/permissions';

const STATUS_TAG_COLORS: Record<number, string> = {
  0: 'warning',
  1: 'success',
  2: 'danger',
};

/**
 * 任务创建 / 编辑表单（编辑不含关联业务字段）
 */
export function useFormSchema(): VbenFormSchema[] {
  return [
    {
      component: 'Input',
      fieldName: 'title',
      label: '任务标题',
      rules: z
        .string()
        .min(1, '任务标题不能为空')
        .max(128, '任务标题不能超过 128 个字符'),
    },
    {
      component: 'Textarea',
      componentProps: { maxLength: 1000, rows: 3, showCount: true },
      fieldName: 'content',
      label: '任务内容',
    },
    {
      component: 'DatePicker',
      componentProps: {
        class: 'w-full',
        showTime: true,
        valueFormat: 'YYYY-MM-DD HH:mm:ss',
      },
      fieldName: 'dueTime',
      label: '截止时间',
      rules: z.string().min(1, '截止时间不能为空'),
    },
  ];
}

/**
 * 查询表单
 */
export function useGridFormSchema(): VbenFormSchema[] {
  return [
    {
      component: 'Select',
      componentProps: {
        allowClear: true,
        options: [...TASK_STATUSES],
      },
      fieldName: 'status',
      label: '状态',
    },
    {
      component: 'RangePicker',
      componentProps: {
        class: 'w-full',
        showTime: true,
        valueFormat: 'YYYY-MM-DD HH:mm:ss',
      },
      fieldName: 'dueTimeRange',
      label: '截止时间',
    },
  ];
}

/**
 * 任务表格列
 */
export function useColumns<T = TaskApi.Task>(
  onActionClick: OnActionClickFn<T>,
): VxeTableGridColumns {
  const { can } = usePermission();
  const canUpdate = can(TASK_PERMISSIONS.update);
  const canComplete = can(TASK_PERMISSIONS.complete);
  return [
    { field: 'title', minWidth: 160, title: '任务标题' },
    { field: 'content', minWidth: 180, title: '任务内容' },
    { field: 'dueTime', title: '截止时间', width: 165 },
    {
      cellRender: {
        name: 'CellTag',
        options: TASK_STATUSES.map((s) => ({
          label: s.label,
          type: STATUS_TAG_COLORS[s.value],
          value: s.value,
        })),
      },
      field: 'status',
      title: '状态',
      width: 100,
    },
    {
      field: 'bizTypeName',
      formatter: ({ row }: any) =>
        row.bizTypeName ? `${row.bizTypeName} #${row.bizId}` : '-',
      title: '关联业务',
      width: 130,
    },
    {
      field: 'source',
      formatter: ({ cellValue }: any) => sourceLabel(cellValue),
      title: '来源',
      width: 100,
    },
    { field: 'ownerName', title: '负责人', width: 110 },
    {
      align: 'center',
      cellRender: {
        attrs: {
          nameField: 'title',
          nameTitle: '任务',
          onClick: onActionClick,
        },
        name: 'CellOperation',
        options: [
          {
            code: 'edit',
            show: (row: TaskApi.Task) => canUpdate && row.status !== 1,
          },
          {
            code: 'complete',
            show: (row: TaskApi.Task) => canComplete && row.status !== 1,
            text: '完成',
          },
        ],
      },
      field: 'operation',
      fixed: 'right',
      title: '操作',
      width: 160,
    },
  ];
}
