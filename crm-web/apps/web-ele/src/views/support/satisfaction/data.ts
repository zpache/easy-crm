import type { VbenFormSchema } from '#/adapter/form';
import type { VxeTableGridColumns } from '#/adapter/vxe-table';
import type { SatisfactionApi } from '#/api/crm/support';

import { SATISFACTION_SCORES } from '#/api/crm/support';

/** 查询表单 */
export function useGridFormSchema(): VbenFormSchema[] {
  return [
    {
      component: 'InputNumber',
      componentProps: { class: 'w-full', min: 0 },
      fieldName: 'ticketId',
      label: '工单 ID',
    },
    {
      component: 'Select',
      componentProps: {
        allowClear: true,
        options: [...SATISFACTION_SCORES],
      },
      fieldName: 'score',
      label: '评分',
    },
    {
      component: 'RangePicker',
      componentProps: {
        class: 'w-full',
        showTime: true,
        valueFormat: 'YYYY-MM-DD HH:mm:ss',
      },
      fieldName: 'evaluateTimeRange',
      label: '评价时间',
    },
  ];
}

/** 满意度评价列表列 */
export function useColumns(): VxeTableGridColumns<SatisfactionApi.Satisfaction> {
  return [
    { field: 'ticketNo', title: '工单编号', width: 190 },
    {
      cellRender: {
        name: 'CellTag',
        options: SATISFACTION_SCORES.map((s) => ({
          label: s.label,
          type: s.value <= 2 ? 'danger' : s.value === 3 ? 'warning' : 'success',
          value: s.value,
        })),
      },
      field: 'score',
      title: '评分',
      width: 100,
    },
    { field: 'content', minWidth: 240, title: '评价内容' },
    { field: 'ownerName', title: '受理人', width: 110 },
    { field: 'evaluateTime', title: '评价时间', width: 165 },
  ];
}
