import type { VbenFormSchema } from '#/adapter/form';
import type { Ref } from 'vue';
import type { OnActionClickFn, VxeTableGridColumns } from '#/adapter/vxe-table';
import type { LeadApi } from '#/api/crm/lead';

import { ref } from 'vue';

import { z } from '#/adapter/form';
import { LEAD_SOURCES, LEAD_STATUSES } from '#/api/crm/lead';
import { useDictOptions } from '#/composables/use-dict';

/** 来源下拉：字典 lead_source 优先，常量兜底（模块级缓存） */
const leadSourceOptions = useDictOptions('lead_source', LEAD_SOURCES);
import { getCampaignOptions } from '#/api/crm/marketing';
import { usePermission } from '#/composables/use-permission';
import { CRM_PERMISSIONS } from '#/constants/permissions';

/** 线索分配权限码（与后端 @PreAuthorize 一致，由统一整合方登记到 permissions.ts） */
export const LEAD_ASSIGN_PERMISSION = 'crm:lead:assign';

/** 营销活动候选（线索来源归因，活动 ROI 口径依赖此字段） */
export const campaignOptions = ref<Array<{ id: number; name: string }>>([]);

export async function loadCampaignOptions() {
  if (campaignOptions.value.length > 0) {
    return;
  }
  const list = await getCampaignOptions();
  campaignOptions.value = list.map((c) => ({ id: c.id, name: c.name }));
}

/**
 * 线索创建 / 编辑表单
 */
export function useFormSchema(): VbenFormSchema[] {
  return [
    {
      component: 'Input',
      fieldName: 'name',
      label: '线索名称',
      rules: z
        .string()
        .min(1, '线索名称不能为空')
        .max(128, '线索名称不能超过 128 个字符'),
    },
    {
      component: 'Input',
      fieldName: 'contactName',
      label: '联系人',
    },
    {
      component: 'Input',
      fieldName: 'phone',
      label: '手机号',
      componentProps: { placeholder: '创建后不可与已有线索重复' },
    },
    {
      component: 'Input',
      fieldName: 'email',
      label: '邮箱',
      componentProps: { placeholder: '创建后不可与已有线索重复' },
    },
    {
      component: 'Select',
      componentProps: () => ({
        allowClear: true,
        class: 'w-full',
        options: [...leadSourceOptions.value],
      }),
      fieldName: 'source',
      label: '来源',
    },
    {
      component: 'Select',
      componentProps: () => ({
        allowClear: true,
        class: 'w-full',
        filterOption: (input: string, option: any) =>
          String(option.label).includes(input),
        options: campaignOptions.value.map((c) => ({
          label: c.name,
          value: c.id,
        })),
        showSearch: true,
      }),
      fieldName: 'campaignId',
      label: '来源活动',
      help: '可选；用于活动效果与 ROI 归因',
    },
    {
      component: 'Textarea',
      componentProps: { maxLength: 500, rows: 2, showCount: true },
      fieldName: 'remark',
      label: '备注',
    },
  ];
}

/**
 * 转化表单：客户 / 联系人可创建或关联（均可选），商机必建（名称缺省取线索名）
 */
export function useConvertSchema(
  customerOptions: Ref<Array<{ id: number; name: string }>>,
  contactOptions: Ref<Array<{ id: number; name: string }>>,
): VbenFormSchema[] {
  return [
    {
      component: 'RadioGroup',
      componentProps: {
        buttonStyle: 'solid',
        options: [
          { label: '创建新客户', value: 'create' },
          { label: '关联已有客户', value: 'link' },
          { label: '暂不处理客户', value: 'skip' },
        ],
        defaultValue: 'skip',
      },
      fieldName: 'customerMode',
      help: '客户与联系人可选；商机将自动创建',
      label: '客户',
    },
    {
      component: 'Input',
      dependencies: {
        if: () => true,
        show: (values) => values.customerMode === 'create',
        triggerFields: ['customerMode'],
      },
      fieldName: 'customerName',
      label: '客户名称',
    },
    {
      component: 'Select',
      componentProps: () => ({
        allowClear: true,
        class: 'w-full',
        filterOption: (input: string, option: any) =>
          String(option.label).includes(input),
        options: customerOptions.value.map((c) => ({
          label: c.name,
          value: c.id,
        })),
        showSearch: true,
      }),
      dependencies: {
        show: (values) => values.customerMode === 'link',
        triggerFields: ['customerMode'],
      },
      fieldName: 'customerId',
      label: '选择客户',
    },
    {
      component: 'RadioGroup',
      componentProps: {
        buttonStyle: 'solid',
        options: [
          { label: '创建新联系人', value: 'create' },
          { label: '关联已有联系人', value: 'link' },
          { label: '暂不处理联系人', value: 'skip' },
        ],
        defaultValue: 'skip',
      },
      fieldName: 'contactMode',
      label: '联系人',
    },
    {
      component: 'Input',
      dependencies: {
        show: (values) => values.contactMode === 'create',
        triggerFields: ['contactMode'],
      },
      fieldName: 'contactName',
      label: '联系人姓名',
    },
    {
      component: 'InputNumber',
      dependencies: {
        show: (values) => values.contactMode === 'link',
        triggerFields: ['contactMode'],
      },
      fieldName: 'contactId',
      label: '联系人 ID',
    },
    {
      component: 'Input',
      fieldName: 'opportunityName',
      help: '留空默认取线索名称',
      label: '商机名称',
    },
  ];
}

/**
 * 查询表单
 */
export function useGridFormSchema(): VbenFormSchema[] {
  return [
    {
      component: 'Input',
      fieldName: 'name',
      label: '线索名称',
    },
    {
      component: 'Input',
      fieldName: 'phone',
      label: '手机号',
    },
    {
      component: 'Select',
      componentProps: {
        allowClear: true,
        options: [...LEAD_STATUSES],
      },
      fieldName: 'status',
      label: '状态',
    },
    {
      component: 'Select',
      componentProps: () => ({
        allowClear: true,
        options: [...leadSourceOptions.value],
      }),
      fieldName: 'source',
      label: '来源',
    },
    {
      component: 'Select',
      componentProps: {
        allowClear: true,
        options: [
          { label: '公海', value: -1 },
        ],
      },
      fieldName: 'ownerId',
      label: '负责人',
    },
  ];
}

/**
 * 线索表格列
 */
export function useColumns<T = LeadApi.Lead>(
  onActionClick: OnActionClickFn<T>,
): VxeTableGridColumns {
  const { can } = usePermission();
  const canUpdate = can(CRM_PERMISSIONS.leadUpdate);
  const canConvert = can(CRM_PERMISSIONS.leadConvert);
  const canPool = can(CRM_PERMISSIONS.leadToPool);
  const canClaim = can(CRM_PERMISSIONS.leadClaim);
  const canAssign = can(LEAD_ASSIGN_PERMISSION);
  return [
    { type: 'checkbox', width: 50, fixed: 'left' },
    { field: 'name', minWidth: 150, title: '线索名称' },
    { field: 'contactName', title: '联系人', width: 90 },
    { field: 'phone', title: '手机号', width: 130 },
    { field: 'email', minWidth: 140, title: '邮箱' },
    { field: 'sourceName', title: '来源', width: 100 },
    { field: 'campaignName', title: '来源活动', width: 150 },
    {
      cellRender: {
        name: 'CellTag',
        options: [
          { label: '新建', type: 'info', value: 0 },
          { label: '跟进中', type: 'warning', value: 1 },
          { label: '已验证', type: 'success', value: 2 },
          { label: '不合格', type: 'danger', value: 3 },
        ],
      },
      field: 'status',
      title: '状态',
      width: 90,
    },
    { field: 'ownerName', title: '负责人', width: 110 },
    { field: 'lastFollowTime', title: '最近跟进', width: 160 },
    { field: 'createTime', title: '创建时间', width: 160 },
    {
      align: 'center',
      cellRender: {
        attrs: {
          nameField: 'name',
          nameTitle: '线索',
          onClick: onActionClick,
        },
        name: 'CellOperation',
        options: [
          // 编辑 / 转化 / 放入公海：仅未转化的线索；领取：仅公海线索
          { code: 'edit', show: (row: LeadApi.Lead) => canUpdate && row.status !== 2 },
          {
            code: 'assign',
            show: (row: LeadApi.Lead) => canAssign && row.status !== 2,
            text: '分配',
          },
          {
            code: 'convert',
            show: (row: LeadApi.Lead) => canConvert && row.status === 0 || row.status === 1,
            text: '转化',
          },
          {
            code: 'toPool',
            show: (row: LeadApi.Lead) => canPool && row.status !== 2 && row.ownerId != null,
            text: '放入公海',
          },
          {
            code: 'claim',
            show: (row: LeadApi.Lead) => canClaim && row.ownerId == null,
            text: '领取',
          },
        ],
      },
      field: 'operation',
      fixed: 'right',
      title: '操作',
      width: 280,
    },
  ];
}
