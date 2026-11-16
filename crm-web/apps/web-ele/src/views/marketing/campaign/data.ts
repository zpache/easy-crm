import type { VbenFormSchema } from '#/adapter/form';
import type { OnActionClickFn, VxeTableGridColumns } from '#/adapter/vxe-table';
import type { MarketingApi } from '#/api/crm/marketing';

import { z } from '#/adapter/form';
import { CAMPAIGN_STATUSES, CAMPAIGN_TYPES } from '#/api/crm/marketing';
import { usePermission } from '#/composables/use-permission';
import { CAMPAIGN_PERMISSIONS } from '#/constants/crm-batch3-permissions';

const STATUS_TAG_TYPES: Record<number, string> = {
  0: 'info',
  1: 'primary',
  2: 'success',
  3: 'warning',
};

/** 活动创建 / 编辑表单 */
export function useFormSchema(): VbenFormSchema[] {
  return [
    {
      component: 'Input',
      fieldName: 'name',
      label: '活动名称',
      rules: z
        .string()
        .min(1, '活动名称不能为空')
        .max(128, '活动名称不能超过 128 个字符'),
    },
    {
      component: 'Input',
      fieldName: 'code',
      label: '活动编码',
      help: '留空由系统自动生成',
    },
    {
      component: 'Select',
      componentProps: { class: 'w-full', options: [...CAMPAIGN_TYPES] },
      fieldName: 'type',
      label: '活动类型',
    },
    {
      component: 'Select',
      componentProps: { class: 'w-full', options: [...CAMPAIGN_STATUSES] },
      fieldName: 'status',
      label: '状态',
    },
    { component: 'Input', fieldName: 'channel', label: '投放渠道' },
    {
      component: 'InputNumber',
      componentProps: { class: 'w-full', min: 0, precision: 2 },
      fieldName: 'budget',
      label: '活动预算',
    },
    {
      component: 'InputNumber',
      componentProps: { class: 'w-full', min: 0, precision: 0 },
      fieldName: 'targetLeads',
      label: '目标线索数',
    },
    {
      component: 'DatePicker',
      componentProps: { class: 'w-full', valueFormat: 'YYYY-MM-DD' },
      fieldName: 'startDate',
      label: '开始日期',
    },
    {
      component: 'DatePicker',
      componentProps: { class: 'w-full', valueFormat: 'YYYY-MM-DD' },
      fieldName: 'endDate',
      label: '结束日期',
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
    {
      component: 'Select',
      componentProps: { allowClear: true, options: [...CAMPAIGN_TYPES] },
      fieldName: 'type',
      label: '活动类型',
    },
    {
      component: 'Select',
      componentProps: { allowClear: true, options: [...CAMPAIGN_STATUSES] },
      fieldName: 'status',
      label: '状态',
    },
  ];
}

function money(value: null | number | undefined) {
  return value == null ? '-' : `¥${Number(value).toLocaleString('zh-CN')}`;
}

function percent(value: null | number | undefined) {
  return value == null ? '-' : `${Number(value).toFixed(2)}%`;
}

/** 活动列表列（含活动效果指标） */
export function useColumns<T = MarketingApi.Campaign>(
  onActionClick: OnActionClickFn<T>,
): VxeTableGridColumns {
  const { can } = usePermission();
  const canUpdate = can(CAMPAIGN_PERMISSIONS.update);
  const canDelete = can(CAMPAIGN_PERMISSIONS.delete);
  return [
    { field: 'code', minWidth: 170, title: '活动编码' },
    { field: 'name', minWidth: 170, title: '活动名称' },
    { field: 'typeName', title: '类型', width: 110 },
    {
      cellRender: {
        name: 'CellTag',
        options: CAMPAIGN_STATUSES.map((s) => ({
          label: s.label,
          type: STATUS_TAG_TYPES[s.value],
          value: s.value,
        })),
      },
      field: 'status',
      title: '状态',
      width: 100,
    },
    { field: 'channel', title: '渠道', width: 120 },
    {
      field: 'budget',
      formatter: ({ cellValue }: any) => money(cellValue),
      title: '预算',
      width: 130,
    },
    { field: 'startDate', title: '开始', width: 120 },
    { field: 'endDate', title: '结束', width: 120 },
    { field: 'leadCount', title: '线索数', width: 90 },
    {
      field: 'convertedCount',
      title: '转化数',
      width: 90,
    },
    {
      field: 'conversionRate',
      formatter: ({ cellValue }: any) => percent(cellValue),
      title: '转化率',
      width: 100,
    },
    {
      field: 'wonAmount',
      formatter: ({ cellValue }: any) => money(cellValue),
      title: '赢单金额',
      width: 140,
    },
    {
      field: 'roi',
      formatter: ({ cellValue }: any) => percent(cellValue),
      title: 'ROI',
      width: 100,
    },
    { field: 'ownerName', title: '负责人', width: 110 },
    {
      align: 'center',
      cellRender: {
        attrs: {
          nameField: 'name',
          nameTitle: '活动',
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
