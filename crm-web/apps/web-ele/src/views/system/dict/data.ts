import type {
  OnActionClickFn,
  VbenFormSchema,
  VxeTableGridColumns,
} from '#/adapter/vxe-table';
import type { SystemDictApi } from '#/api/system/dict';

import { z } from '#/adapter/form';
import { SYSTEM_PERMISSIONS } from '#/constants/permissions';
import { $t } from '#/locales';

/**
 * 字典类型查询表单
 */
export function useGridFormSchema(): VbenFormSchema[] {
  return [
    {
      component: 'Input',
      componentProps: { allowClear: true, placeholder: '编码 / 名称' },
      fieldName: 'keyword',
      label: '关键字',
    },
  ];
}

/**
 * 字典类型表格列
 */
export function useColumns(
  onActionClick: OnActionClickFn<SystemDictApi.Dict>,
): VxeTableGridColumns {
  const canUpdate = SYSTEM_PERMISSIONS.dictUpdate;
  const canDelete = SYSTEM_PERMISSIONS.dictDelete;
  return [
    { type: 'seq', width: 60 },
    { field: 'dictType', title: '编码', minWidth: 160 },
    { field: 'name', title: '名称', minWidth: 120 },
    { field: 'itemCount', title: '字典项数', width: 90 },
    {
      field: 'builtIn',
      title: '内置',
      width: 80,
      formatter: ({ cellValue }) => (cellValue === 1 ? '是' : '否'),
    },
    {
      field: 'status',
      title: '状态',
      width: 90,
      formatter: ({ cellValue }) => (cellValue === 1 ? '启用' : '停用'),
    },
    { field: 'remark', title: '备注', minWidth: 140 },
    {
      field: 'updateTime',
      title: '更新时间',
      width: 160,
      formatter: ({ cellValue }) => (cellValue ? String(cellValue).replace('T', ' ') : ''),
    },
    {
      align: 'center',
      cellRender: {
        attrs: {
          nameField: 'name',
          nameTitle: '字典',
          onClick: onActionClick,
        },
        name: 'CellOperation',
        options: [
          { code: 'items', show: canUpdate, text: '字典项' },
          { code: 'edit', show: canUpdate },
          { code: 'delete', show: canDelete },
        ],
      },
      field: 'operation',
      fixed: 'right',
      title: '操作',
      width: 220,
    },
  ];
}

/**
 * 字典类型编辑表单
 */
export function useFormSchema(isEdit: boolean): VbenFormSchema[] {
  return [
    {
      component: 'Input',
      componentProps: {
        disabled: isEdit,
        placeholder: '小写字母开头的字母/数字/下划线，如 lead_source',
      },
      fieldName: 'dictType',
      help: isEdit ? '编码创建后不可修改' : '创建后不可修改',
      label: '字典编码',
      rules: isEdit
        ? z.string()
        : z
            .string()
            .regex(
              /^[a-z][a-z0-9_]{1,63}$/,
              '小写字母开头，仅字母/数字/下划线，2-64 位',
            ),
    },
    {
      component: 'Input',
      fieldName: 'name',
      label: '字典名称',
      rules: z.string().min(2, '至少 2 个字符').max(64, '最多 64 个字符'),
    },
    {
      component: 'RadioGroup',
      componentProps: {
        buttonStyle: 'solid',
        options: [
          { label: $t('common.enabled'), value: 1 },
          { label: $t('common.disabled'), value: 0 },
        ],
      },
      defaultValue: 1,
      fieldName: 'status',
      label: $t('system.role.status'),
    },
    {
      component: 'Textarea',
      componentProps: { maxLength: 255, rows: 2, showCount: true },
      fieldName: 'remark',
      label: '备注',
    },
  ];
}

/**
 * 字典项表格列（抽屉内）
 */
export function useItemColumns(
  onActionClick: OnActionClickFn<SystemDictApi.DictItem>,
): VxeTableGridColumns {
  return [
    { type: 'seq', width: 55 },
    { field: 'label', title: '显示名称', minWidth: 120 },
    { field: 'value', title: '值', minWidth: 120 },
    { field: 'sortNo', title: '排序', width: 70 },
    {
      field: 'status',
      title: '状态',
      width: 80,
      formatter: ({ cellValue }) => (cellValue === 1 ? '启用' : '停用'),
    },
    { field: 'remark', title: '备注', minWidth: 120 },
    {
      align: 'center',
      cellRender: {
        attrs: {
          nameField: 'label',
          nameTitle: '字典项',
          onClick: onActionClick,
        },
        name: 'CellOperation',
        options: [
          { code: 'edit', show: SYSTEM_PERMISSIONS.dictUpdate },
          { code: 'delete', show: SYSTEM_PERMISSIONS.dictDelete },
        ],
      },
      field: 'operation',
      fixed: 'right',
      title: '操作',
      width: 140,
    },
  ];
}

/**
 * 字典项编辑表单
 */
export function useItemFormSchema(isEdit: boolean): VbenFormSchema[] {
  return [
    {
      component: 'Input',
      componentProps: { disabled: isEdit, placeholder: '数字码或英文码，如 1 / PHONE' },
      fieldName: 'value',
      help: isEdit ? '值创建后不可修改（历史数据兼容），如需调整请新增后停用旧项' : '创建后不可修改',
      label: '值',
      rules: isEdit
        ? z.string()
        : z.string().regex(/^[A-Za-z0-9_-]{1,64}$/, '仅字母/数字/下划线/连字符，1-64 位'),
    },
    {
      component: 'Input',
      fieldName: 'label',
      label: '显示名称',
      rules: z.string().min(1, '必填').max(64, '最多 64 个字符'),
    },
    {
      component: 'InputNumber',
      componentProps: { class: 'w-full', min: 0 },
      defaultValue: 0,
      fieldName: 'sortNo',
      label: '排序',
    },
    {
      component: 'RadioGroup',
      componentProps: {
        buttonStyle: 'solid',
        options: [
          { label: $t('common.enabled'), value: 1 },
          { label: $t('common.disabled'), value: 0 },
        ],
      },
      defaultValue: 1,
      fieldName: 'status',
      label: $t('system.role.status'),
    },
    {
      component: 'Textarea',
      componentProps: { maxLength: 255, rows: 2, showCount: true },
      fieldName: 'remark',
      label: '备注',
    },
  ];
}
