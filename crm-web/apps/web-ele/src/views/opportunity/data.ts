import type { VbenFormSchema } from '#/adapter/form';
import type { OnActionClickFn, VxeTableGridColumns } from '#/adapter/vxe-table';
import type { OpportunityApi } from '#/api/crm/opportunity';

import { ref } from 'vue';

import { z } from '#/adapter/form';
import { getContactList } from '#/api/crm/contact';
import { getCustomerList } from '#/api/crm/customer';
import { LOST_REASONS } from '#/api/crm/opportunity';
import { usePermission } from '#/composables/use-permission';
import { OPPORTUNITY_PERMISSIONS } from '#/constants/permissions';

/** 客户候选（模块级共享，页面启动时加载一次） */
export const customerOptions = ref<Array<{ id: number; name: string }>>([]);

/** 阶段配置（查询表单 / 变更阶段抽屉共用，list.vue 启动时加载） */
export const stageOptions = ref<Array<{ stageCode: string; stageName: string }>>([]);

/** 已加载的联系人候选缓存：customerId -> options（选客户后联动加载） */
const contactCache = ref<Record<number, Array<{ id: number; name: string }>>>({});

/** 商机分配权限码（与后端 @PreAuthorize 一致，由统一整合方登记到 permissions.ts） */
export const OPPORTUNITY_ASSIGN_PERMISSION = 'crm:opportunity:assign';

/** 停滞等级标签 */
export const STALE_LEVEL_OPTIONS = [
  { label: '正常', type: 'success', value: 0 },
  { label: '黄（7天）', type: 'warning', value: 1 },
  { label: '橙（14天）', type: 'warning', value: 2 },
  { label: '红（30天）', type: 'danger', value: 3 },
] as const;

export async function loadCustomerOptions() {
  const result = await getCustomerList({ pageSize: 100 });
  customerOptions.value = result.items.map((item: any) => ({
    id: item.id,
    name: item.name,
  }));
}

/**
 * 按客户加载联系人候选（进入缓存，供下拉函数读取）
 */
export async function loadContactsForCustomer(customerId?: number) {
  if (!customerId) {
    return;
  }
  if (!contactCache.value[customerId]) {
    const result = await getContactList({ customerId, pageSize: 100 });
    contactCache.value[customerId] = result.items.map((item: any) => ({
      id: item.id,
      name: item.name,
    }));
  }
}

function contactOptionsFor(customerId?: number) {
  return customerId == null ? [] : (contactCache.value[customerId] ?? []);
}

/**
 * 商机创建 / 编辑表单。编辑时客户不可变更（商机必须挂在创建时的客户下）。
 * 联系人下拉按 formModel.customerId 动态取缓存。
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
        placeholder: isEdit ? undefined : '请选择客户',
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
      dependencies: {
        trigger: ['customerId'],
      },
      fieldName: 'contactId',
      label: '联系人',
    },
    {
      component: 'Input',
      fieldName: 'name',
      label: '商机名称',
      rules: z
        .string()
        .min(1, '商机名称不能为空')
        .max(128, '商机名称不能超过 128 个字符'),
    },
    {
      component: 'InputNumber',
      componentProps: { class: 'w-full', min: 0, precision: 2 },
      fieldName: 'amount',
      label: '预计金额',
    },
    {
      component: 'DatePicker',
      componentProps: { class: 'w-full', valueFormat: 'YYYY-MM-DD' },
      fieldName: 'expectedCloseDate',
      label: '预计成交日期',
    },
  ];
}

/**
 * 阶段变更表单（丢单原因 / 竞争对手仅在目标为丢单时显示；赢单显示成交信息）
 */
export function useStageChangeSchema(): VbenFormSchema[] {
  return [
    {
      component: 'Select',
      componentProps: () => ({
        class: 'w-full',
        options: stageOptions.value.map((s) => ({ label: s.stageName, value: s.stageCode })),
      }),
      fieldName: 'targetStageCode',
      label: '目标阶段',
      rules: z.string().min(1, '请选择目标阶段'),
    },
    {
      component: 'DatePicker',
      componentProps: { class: 'w-full', valueFormat: 'YYYY-MM-DD' },
      dependencies: {
        show: (values) => values.targetStageCode === 'WON',
        triggerFields: ['targetStageCode'],
      },
      fieldName: 'actualCloseDate',
      label: '实际成交日期（缺省今天）',
    },
    {
      component: 'InputNumber',
      componentProps: { class: 'w-full', min: 0, precision: 2 },
      dependencies: {
        show: (values) => values.targetStageCode === 'WON',
        triggerFields: ['targetStageCode'],
      },
      fieldName: 'actualAmount',
      label: '实际成交金额（缺省预计金额）',
    },
    {
      component: 'Select',
      componentProps: {
        allowClear: false,
        class: 'w-full',
        options: LOST_REASONS.map((r) => ({ ...r })),
      },
      dependencies: {
        rules: (values) =>
          values.targetStageCode === 'LOST'
            ? z.string().min(1, '丢单必须选择原因')
            : undefined,
        show: (values) => values.targetStageCode === 'LOST',
        triggerFields: ['targetStageCode'],
      },
      fieldName: 'lostReason',
      label: '丢单原因',
    },
    {
      component: 'Input',
      dependencies: {
        show: (values) => values.targetStageCode === 'LOST',
        triggerFields: ['targetStageCode'],
      },
      fieldName: 'competitor',
      label: '竞争对手',
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
      componentProps: () => ({
        allowClear: true,
        class: 'w-full',
        filterOption: (input: string, option: any) =>
          option.label.includes(input),
        options: stageOptions.value.map((s) => ({
          label: s.stageName,
          value: s.stageCode,
        })),
        showSearch: true,
      }),
      fieldName: 'stageCode',
      label: '阶段',
    },
    {
      component: 'RangePicker',
      componentProps: { class: 'w-full', valueFormat: 'YYYY-MM-DD' },
      fieldName: 'expectedCloseRange',
      label: '预计成交',
    },
    {
      component: 'Select',
      componentProps: {
        allowClear: true,
        options: [{ label: '公海', value: -1 }],
      },
      fieldName: 'ownerId',
      label: '负责人',
    },
  ];
}

/**
 * 商机表格列
 */
export function useColumns<T = OpportunityApi.Opportunity>(
  onActionClick: OnActionClickFn<T>,
): VxeTableGridColumns {
  const { can } = usePermission();
  const canUpdate = can(OPPORTUNITY_PERMISSIONS.update);
  const canStage = can(OPPORTUNITY_PERMISSIONS.stage);
  const canAssign = can(OPPORTUNITY_ASSIGN_PERMISSION);
  return [
    { type: 'checkbox', width: 50, fixed: 'left' },
    { field: 'name', minWidth: 150, title: '商机名称' },
    { field: 'customerName', minWidth: 140, title: '客户' },
    { field: 'contactName', title: '联系人', width: 100 },
    { field: 'stageName', title: '阶段', width: 100 },
    {
      field: 'amount',
      formatter: ({ cellValue }: any) =>
        cellValue == null ? '-' : Number(cellValue).toLocaleString(),
      title: '预计金额',
      width: 120,
    },
    { field: 'winRate', title: '赢单率%', width: 90 },
    {
      cellRender: {
        name: 'CellTag',
        options: [...STALE_LEVEL_OPTIONS],
      },
      field: 'staleLevel',
      title: '停滞',
      width: 110,
    },
    { field: 'expectedCloseDate', title: '预计成交', width: 120 },
    { field: 'ownerName', title: '负责人', width: 110 },
    {
      align: 'center',
      cellRender: {
        attrs: {
          nameField: 'name',
          nameTitle: '商机',
          onClick: onActionClick,
        },
        name: 'CellOperation',
        options: [
          { code: 'edit', show: canUpdate },
          { code: 'stage', show: canStage, text: '变更阶段' },
          { code: 'assign', show: canAssign, text: '分配' },
        ],
      },
      field: 'operation',
      fixed: 'right',
      title: '操作',
      width: 220,
    },
  ];
}
