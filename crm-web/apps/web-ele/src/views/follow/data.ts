import type { VbenFormSchema } from '#/adapter/form';
import type { OnActionClickFn, VxeTableGridColumns } from '#/adapter/vxe-table';
import type { FollowApi } from '#/api/crm/follow';

import { z } from '#/adapter/form';
import {
  FOLLOW_BIZ_TYPES,
  FOLLOW_TYPES,
  TIMELINE_SOURCES,
} from '#/api/crm/follow';
import { usePermission } from '#/composables/use-permission';

/**
 * 跟进记录权限码（与后端 @PreAuthorize 保持一致）。
 * 这些权限码由统一整合方登记到 constants/permissions.ts，本页面先本地声明。
 */
export const FOLLOW_PERMISSIONS = {
  create: 'crm:follow:create',
  delete: 'crm:follow:delete',
  list: 'crm:follow:list',
  update: 'crm:follow:update',
} as const;

/** 跟进记录创建 / 编辑表单（编辑时关联对象不可变更） */
export function useFormSchema(isEdit: boolean): VbenFormSchema[] {
  return [
    {
      component: 'Select',
      componentProps: { disabled: isEdit, options: [...FOLLOW_BIZ_TYPES] },
      fieldName: 'bizType',
      label: '业务类型',
      rules: z.string().min(1, '请选择业务类型'),
    },
    {
      component: 'InputNumber',
      componentProps: {
        class: 'w-full',
        controls: false,
        disabled: isEdit,
        min: 1,
        placeholder: '关联对象的 ID',
      },
      fieldName: 'bizId',
      label: '业务 ID',
      rules: z.number({ message: '请输入业务 ID' }),
    },
    {
      component: 'Select',
      componentProps: { options: [...FOLLOW_TYPES] },
      fieldName: 'followType',
      label: '跟进类型',
      rules: z.string().min(1, '请选择跟进类型'),
    },
    {
      component: 'Textarea',
      componentProps: { maxLength: 1000, rows: 3, showCount: true },
      fieldName: 'content',
      label: '跟进内容',
      rules: z
        .string()
        .min(1, '跟进内容不能为空')
        .max(1000, '跟进内容不能超过 1000 个字符'),
    },
    {
      component: 'DatePicker',
      componentProps: {
        class: 'w-full',
        showTime: true,
        valueFormat: 'YYYY-MM-DD HH:mm:ss',
      },
      fieldName: 'nextFollowTime',
      label: '下次跟进时间（选填，自动生成待办）',
    },
  ];
}

/** 跟进记录列表查询表单 */
export function useGridFormSchema(): VbenFormSchema[] {
  return [
    {
      component: 'Select',
      componentProps: { allowClear: true, options: [...FOLLOW_BIZ_TYPES] },
      fieldName: 'bizType',
      label: '业务类型',
    },
    {
      component: 'InputNumber',
      componentProps: { class: 'w-full', controls: false, min: 1 },
      fieldName: 'bizId',
      label: '业务 ID',
    },
    {
      component: 'RangePicker',
      componentProps: {
        class: 'w-full',
        valueFormat: 'YYYY-MM-DD HH:mm:ss',
      },
      fieldName: 'createTimeRange',
      label: '跟进时间',
    },
  ];
}

/** 时间线查询表单（按 bizType + bizId 查 /timeline） */
export function useTimelineFormSchema(): VbenFormSchema[] {
  return [
    {
      component: 'Select',
      componentProps: { options: [...FOLLOW_BIZ_TYPES] },
      fieldName: 'bizType',
      label: '业务类型',
    },
    {
      component: 'InputNumber',
      componentProps: { class: 'w-full', controls: false, min: 1 },
      fieldName: 'bizId',
      label: '业务 ID',
    },
  ];
}

/** 跟进记录表格列 */
export function useColumns<T = FollowApi.FollowRecord>(
  onActionClick: OnActionClickFn<T>,
): VxeTableGridColumns {
  const { can } = usePermission();
  const canUpdate = can(FOLLOW_PERMISSIONS.update);
  const canDelete = can(FOLLOW_PERMISSIONS.delete);
  return [
    {
      field: 'bizTypeName',
      formatter: ({ row }: any) =>
        `${row.bizTypeName ?? row.bizType ?? '-'} #${row.bizId ?? '-'}`,
      minWidth: 140,
      title: '关联业务',
    },
    { field: 'followTypeName', title: '跟进类型', width: 100 },
    {
      field: 'content',
      minWidth: 220,
      showOverflow: 'tooltip',
      title: '跟进内容',
    },
    { field: 'ownerName', title: '跟进人', width: 110 },
    { field: 'nextFollowTime', title: '下次跟进', width: 160 },
    { field: 'createTime', title: '跟进时间', width: 160 },
    {
      align: 'center',
      cellRender: {
        attrs: {
          nameField: 'content',
          nameTitle: '跟进记录',
          onClick: onActionClick,
        },
        name: 'CellOperation',
        options: [
          { code: 'edit', show: () => canUpdate, text: '编辑' },
          { code: 'remove', show: () => canDelete, text: '删除' },
        ],
      },
      field: 'operation',
      fixed: 'right',
      title: '操作',
      width: 140,
    },
  ];
}

/** 时间线表格列（跟进 / 待办 / 阶段变更三类聚合） */
export function useTimelineColumns(): VxeTableGridColumns {
  return [
    {
      field: 'time',
      formatter: ({ row }: any) =>
        (row.time ?? '').replace('T', ' ').slice(0, 19),
      title: '时间',
      width: 170,
    },
    {
      cellRender: {
        name: 'CellTag',
        options: TIMELINE_SOURCES.map((s) => ({
          label: s.label,
          type: s.color,
          value: s.value,
        })),
      },
      field: 'source',
      title: '来源',
      width: 110,
    },
    {
      field: 'typeName',
      formatter: ({ row }: any) =>
        row.source === 'stage_log' && row.fromStageName
          ? `${row.fromStageName} → ${row.toStageName ?? '-'}`
          : (row.typeName ?? '-'),
      minWidth: 160,
      title: '类型 / 阶段',
    },
    {
      field: 'content',
      minWidth: 220,
      showOverflow: 'tooltip',
      title: '内容',
    },
    { field: 'operatorName', title: '操作人', width: 110 },
  ];
}
