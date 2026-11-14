import type { VbenFormSchema } from '#/adapter/form';
import type { VxeTableGridColumns } from '#/adapter/vxe-table';

/**
 * 消息提醒权限码（与后端 @PreAuthorize 保持一致；由统一整合方登记到
 * constants/permissions.ts，本页面先本地声明）。
 */
export const REMINDER_PERMISSIONS = {
  list: 'crm:reminder:list',
  read: 'crm:reminder:read',
} as const;

export function formatTime(value?: string) {
  return value ? String(value).replace('T', ' ').slice(0, 19) : '';
}

/** 消息提醒查询表单 */
export function useGridFormSchema(): VbenFormSchema[] {
  return [
    {
      component: 'Select',
      componentProps: {
        allowClear: true,
        options: [
          { label: '未读', value: true },
          { label: '全部', value: false },
        ],
      },
      fieldName: 'onlyUnread',
      label: '状态',
    },
  ];
}

/** 消息提醒列表列 */
export function useColumns(): VxeTableGridColumns {
  return [
    {
      cellRender: {
        name: 'CellTag',
        options: [
          { label: '黄', type: 'warning', value: 1 },
          { label: '橙', type: 'warning', value: 2 },
          { label: '红', type: 'danger', value: 3 },
        ],
      },
      field: 'remindLevel',
      title: '等级',
      width: 90,
    },
    {
      field: 'content',
      minWidth: 300,
      showOverflow: 'tooltip',
      title: '提醒内容',
    },
    { field: 'bizType', title: '业务类型', width: 140 },
    {
      field: 'readFlag',
      formatter: ({ row }: any) => (row.readFlag === 1 ? '已读' : '未读'),
      title: '状态',
      width: 90,
    },
    {
      field: 'remindTime',
      formatter: ({ row }: any) => formatTime(row.remindTime),
      title: '提醒时间',
      width: 170,
    },
  ];
}
