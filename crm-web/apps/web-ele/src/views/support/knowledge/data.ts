import type { VbenFormSchema } from '#/adapter/form';
import type { OnActionClickFn, VxeTableGridColumns } from '#/adapter/vxe-table';
import type { KnowledgeApi } from '#/api/crm/support';

import { z } from '#/adapter/form';
import { KNOWLEDGE_STATUSES, SUPPORT_PERMISSIONS } from '#/api/crm/support';
import { usePermission } from '#/composables/use-permission';

/** 知识库新增 / 编辑表单 */
export function useFormSchema(): VbenFormSchema[] {
  return [
    {
      component: 'Input',
      fieldName: 'category',
      label: '分类',
      rules: z.string().min(1, '分类不能为空').max(64),
    },
    {
      component: 'Input',
      fieldName: 'title',
      label: '标题',
      rules: z.string().min(1, '标题不能为空').max(128),
    },
    {
      component: 'Input',
      componentProps: { placeholder: '多个标签用逗号分隔' },
      fieldName: 'tags',
      label: '标签',
    },
    {
      component: 'Select',
      componentProps: {
        class: 'w-full',
        options: KNOWLEDGE_STATUSES.map((s) => ({
          label: s.label,
          value: s.value,
        })),
      },
      fieldName: 'publishStatus',
      label: '发布状态',
      rules: z.string().min(1, '请选择发布状态'),
    },
    {
      component: 'Textarea',
      componentProps: { rows: 8 },
      fieldName: 'content',
      label: '内容',
    },
  ];
}

/** 查询 / 检索表单 */
export function useGridFormSchema(): VbenFormSchema[] {
  return [
    {
      component: 'Input',
      componentProps: { allowClear: true, placeholder: '标题 / 分类 / 标签 / 内容' },
      fieldName: 'keyword',
      label: '检索',
    },
    {
      component: 'Input',
      componentProps: { allowClear: true },
      fieldName: 'category',
      label: '分类',
    },
    {
      component: 'Select',
      componentProps: {
        allowClear: true,
        options: KNOWLEDGE_STATUSES.map((s) => ({
          label: s.label,
          value: s.value,
        })),
      },
      fieldName: 'publishStatus',
      label: '发布状态',
    },
  ];
}

/** 知识库表格列 */
export function useColumns<T = KnowledgeApi.Knowledge>(
  onActionClick: OnActionClickFn<T>,
): VxeTableGridColumns {
  const { can } = usePermission();
  const canUpdate = can(SUPPORT_PERMISSIONS.knowledgeUpdate);
  const canDelete = can(SUPPORT_PERMISSIONS.knowledgeDelete);
  return [
    { field: 'title', minWidth: 180, title: '标题' },
    { field: 'category', title: '分类', width: 130 },
    { field: 'tags', minWidth: 140, title: '标签' },
    {
      cellRender: {
        name: 'CellTag',
        options: KNOWLEDGE_STATUSES.map((s) => ({
          label: s.label,
          type: s.type,
          value: s.value,
        })),
      },
      field: 'publishStatus',
      title: '发布状态',
      width: 110,
    },
    { field: 'viewCount', title: '浏览量', width: 100 },
    { field: 'updateTime', title: '更新时间', width: 165 },
    {
      align: 'center',
      cellRender: {
        attrs: {
          nameField: 'title',
          nameTitle: '知识',
          onClick: onActionClick,
        },
        name: 'CellOperation',
        options: [
          { code: 'detail', text: '查看' },
          { code: 'edit', show: canUpdate },
          { code: 'delete', show: canDelete, text: '删除' },
        ],
      },
      field: 'operation',
      fixed: 'right',
      title: '操作',
      width: 180,
    },
  ];
}
