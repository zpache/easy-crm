import type { VbenFormSchema } from '#/adapter/form';
import type { OnActionClickFn, VxeTableGridColumns } from '#/adapter/vxe-table';
import type { TicketApi } from '#/api/crm/support';

import { ref } from 'vue';

import { z } from '#/adapter/form';
import { getContactList } from '#/api/crm/contact';
import { getCustomerList } from '#/api/crm/customer';
import { loadOrderOptions, orderSelectProps } from '../../payment/shared/options';
import {
  SUPPORT_PERMISSIONS,
  TICKET_PRIORITIES,
  TICKET_SOURCES,
  TICKET_STATUSES,
  TICKET_TERMINAL_STATUSES,
  TICKET_TYPES,
} from '#/api/crm/support';
import { getUserList } from '#/api/system/user';
import { usePermission } from '#/composables/use-permission';

/** 客户候选（创建表单 / 查询共用，页面启动时加载一次） */
export const orderOptions = ref<Array<{ id: number; orderNo: string; customerId: number }>>([]);

/** 订单候选（工单关联订单用） */
export async function preloadOrderOptions() {
  await loadOrderOptions();
}

<Array<{ id: number; name: string }>>([]);

/** 受理人候选（派单抽屉 / 查询共用） */
export const userOptions = ref<Array<{ id: number; name: string }>>([]);

/** 联系人候选缓存：customerId -> options */
const contactCache = ref<Record<number, Array<{ id: number; name: string }>>>({});

export async function loadCustomerOptions() {
  const result = await getCustomerList({ pageSize: 100 });
  customerOptions.value = result.items.map((item: any) => ({
    id: item.id,
    name: item.name,
  }));
}

export async function loadUserOptions() {
  const result = await getUserList({ pageSize: 100 });
  userOptions.value = result.items.map((item: any) => ({
    id: item.id,
    name: item.nickname || item.username,
  }));
}

export async function loadContactsForCustomer(customerId?: number) {
  if (!customerId || contactCache.value[customerId]) {
    return;
  }
  const result = await getContactList({ customerId, pageSize: 100 });
  contactCache.value[customerId] = result.items.map((item: any) => ({
    id: item.id,
    name: item.name,
  }));
}

function contactOptionsFor(customerId?: number) {
  return customerId == null ? [] : (contactCache.value[customerId] ?? []);
}

/**
 * 工单创建 / 编辑表单。编辑时客户 / 类型 / 优先级不可变更（后端 SLA 已按创建时计算）。
 */
export function useFormSchema(isEdit: boolean): VbenFormSchema[] {
  return [
    {
      component: 'Select',
      componentProps: () => ({
        class: 'w-full',
        disabled: isEdit,
        filterOption: (input: string, option: any) =>
          option.label.includes(input),
        options: customerOptions.value.map((c) => ({
          label: c.name,
          value: c.id,
        })),
        showSearch: true,
      }),
      fieldName: 'customerId',
      label: '所属客户',
      rules: isEdit ? undefined : z.number({ message: '请选择所属客户' }),
    },
    {
      component: 'Select',
      componentProps: ({ formModel }: any) => ({
        allowClear: true,
        class: 'w-full',
        options: contactOptionsFor(formModel?.customerId),
      }),
      dependencies: { trigger: ['customerId'] },
      fieldName: 'contactId',
      label: '联系人',
    },
    {
      component: 'Select',
      componentProps: () => ({ allowClear: true, ...orderSelectProps() }),
      fieldName: 'orderId',
      label: '关联订单（选填）',
    },
    {
      component: 'Select',
      componentProps: {
        class: 'w-full',
        disabled: isEdit,
        options: [...TICKET_TYPES],
      },
      fieldName: 'ticketType',
      label: '工单类型',
      rules: isEdit ? undefined : z.string().min(1, '请选择工单类型'),
    },
    {
      component: 'Select',
      componentProps: {
        class: 'w-full',
        disabled: isEdit,
        options: [...TICKET_PRIORITIES],
      },
      fieldName: 'priority',
      label: '优先级',
      rules: isEdit ? undefined : z.string().min(1, '请选择优先级'),
    },
    {
      component: 'Select',
      componentProps: {
        allowClear: true,
        class: 'w-full',
        options: [...TICKET_SOURCES],
      },
      fieldName: 'source',
      label: '来源',
    },
    {
      component: 'Input',
      fieldName: 'title',
      label: '工单标题',
      rules: z
        .string()
        .min(1, '工单标题不能为空')
        .max(128, '工单标题不能超过 128 个字符'),
    },
    {
      component: 'Textarea',
      componentProps: { maxLength: 2000, rows: 4, showCount: true },
      fieldName: 'description',
      label: '问题描述',
    },
  ];
}

/** 查询表单 */
export function useGridFormSchema(): VbenFormSchema[] {
  return [
    {
      component: 'Input',
      componentProps: { allowClear: true, placeholder: '工单编号 / 标题' },
      fieldName: 'keyword',
      label: '关键字',
    },
    {
      component: 'Select',
      componentProps: {
        allowClear: true,
        options: [...TICKET_STATUSES],
      },
      fieldName: 'status',
      label: '状态',
    },
    {
      component: 'Select',
      componentProps: { allowClear: true, options: [...TICKET_TYPES] },
      fieldName: 'ticketType',
      label: '工单类型',
    },
    {
      component: 'Select',
      componentProps: { allowClear: true, options: [...TICKET_PRIORITIES] },
      fieldName: 'priority',
      label: '优先级',
    },
    {
      component: 'Select',
      componentProps: {
        allowClear: true,
        options: [
          { label: '正常', value: 0 },
          { label: '超时', value: 1 },
        ],
      },
      fieldName: 'overdueFlag',
      label: '超时',
    },
    {
      component: 'RangePicker',
      componentProps: {
        class: 'w-full',
        showTime: true,
        valueFormat: 'YYYY-MM-DD HH:mm:ss',
      },
      fieldName: 'createTimeRange',
      label: '创建时间',
    },
  ];
}

const STATUS_TAG_COLORS: Record<string, string> = {
  ASSIGNED: 'primary',
  CLOSED: 'info',
  COMPLETED: 'success',
  PENDING: 'warning',
  PROCESSING: 'primary',
  WAITING_CONFIRM: 'warning',
};

/** 工单表格列 */
export function useColumns<T = TicketApi.Ticket>(
  onActionClick: OnActionClickFn<T>,
): VxeTableGridColumns {
  const { can } = usePermission();
  const canUpdate = can(SUPPORT_PERMISSIONS.ticketUpdate);
  const canStatus = can(SUPPORT_PERMISSIONS.ticketStatus);
  const canAssign = can(SUPPORT_PERMISSIONS.ticketAssign);
  const isTerminal = (status: string) =>
    (TICKET_TERMINAL_STATUSES as readonly string[]).includes(status);
  return [
    { field: 'ticketNo', title: '工单编号', width: 190 },
    { field: 'title', minWidth: 160, title: '工单标题' },
    { field: 'customerName', title: '客户', width: 130 },
    {
      cellRender: {
        name: 'CellTag',
        options: TICKET_STATUSES.map((s) => ({
          label: s.label,
          type: STATUS_TAG_COLORS[s.value] ?? 'info',
          value: s.value,
        })),
      },
      field: 'status',
      title: '状态',
      width: 120,
    },
    { field: 'priorityName', title: '优先级', width: 90 },
    { field: 'ticketTypeName', title: '类型', width: 100 },
    { field: 'ownerName', title: '受理人', width: 100 },
    {
      cellRender: {
        name: 'CellTag',
        options: [
          { label: '正常', type: 'success', value: 0 },
          { label: '超时', type: 'danger', value: 1 },
        ],
      },
      field: 'overdueFlag',
      title: 'SLA',
      width: 90,
    },
    { field: 'slaDueResolveTime', title: '应解决时间', width: 165 },
    { field: 'createTime', title: '创建时间', width: 165 },
    {
      align: 'center',
      cellRender: {
        attrs: {
          nameField: 'title',
          nameTitle: '工单',
          onClick: onActionClick,
        },
        name: 'CellOperation',
        options: [
          { code: 'detail', text: '详情' },
          {
            code: 'edit',
            show: (row: TicketApi.Ticket) =>
              canUpdate && !isTerminal(row.status),
          },
          {
            code: 'flow',
            show: (row: TicketApi.Ticket) =>
              canStatus && !isTerminal(row.status),
            text: '流转',
          },
          {
            code: 'assign',
            show: (row: TicketApi.Ticket) =>
              canAssign && !isTerminal(row.status),
            text: '派单',
          },
        ],
      },
      field: 'operation',
      fixed: 'right',
      title: '操作',
      width: 220,
    },
  ];
}
