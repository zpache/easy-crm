import type { VbenFormSchema } from '#/adapter/form';
import type { OnActionClickFn, VxeTableGridColumns } from '#/adapter/vxe-table';
import type { ProductApi } from '#/api/crm/product';

import { ref } from 'vue';

import { z } from '#/adapter/form';
import { getCustomerList } from '#/api/crm/customer';
import { QUOTE_STATUSES } from '#/api/crm/product';
import { usePermission } from '#/composables/use-permission';
import { QUOTE_PERMISSIONS } from '#/constants/crm-batch3-permissions';

const STATUS_TAG_TYPES: Record<number, string> = {
  0: 'info',
  1: 'warning',
  2: 'success',
  3: 'danger',
};

/** 客户候选选项 */
export const customerOptions = ref<Array<{ id: number; name: string }>>([]);

export async function loadCustomerOptions() {
  if (customerOptions.value.length > 0) {
    return;
  }
  const result = await getCustomerList({ pageSize: 200 });
  customerOptions.value = result.items.map((item: any) => ({
    id: item.id,
    name: item.name,
  }));
}

function customerSelectProps() {
  return {
    allowClear: true,
    class: 'w-full',
    filterOption: (input: string, option: any) =>
      String(option.label).includes(input),
    options: customerOptions.value.map((c) => ({ label: c.name, value: c.id })),
    showSearch: true,
  };
}

/**
 * 报价单表单（明细由抽屉内动态表格编辑）。
 * 明细汇总 Σ(单价*数量-折扣) 必须等于 totalAmount，保存前在 form.vue 校验。
 */
export function useFormSchema(): VbenFormSchema[] {
  return [
    {
      component: 'Input',
      fieldName: 'quoteNo',
      label: '报价单编号',
      help: '留空由系统自动生成',
    },
    {
      component: 'Select',
      componentProps: () => customerSelectProps(),
      fieldName: 'customerId',
      label: '客户',
      rules: z.number({ message: '请选择客户' }),
    },
    {
      component: 'InputNumber',
      componentProps: { class: 'w-full', min: 0, precision: 0 },
      fieldName: 'opportunityId',
      label: '关联商机 ID',
    },
    {
      component: 'InputNumber',
      componentProps: { class: 'w-full', min: 0, precision: 2 },
      fieldName: 'totalAmount',
      label: '总金额',
      help: '必须与明细汇总一致',
      rules: z.number({ message: '总金额不能为空' }),
    },
    {
      component: 'Select',
      componentProps: { class: 'w-full', options: [...QUOTE_STATUSES] },
      fieldName: 'status',
      label: '状态',
    },
    {
      component: 'DatePicker',
      componentProps: {
        class: 'w-full',
        placeholder: '报价有效期截止日',
        valueFormat: 'YYYY-MM-DD',
      },
      fieldName: 'validUntil',
      label: '有效期至',
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
    { component: 'Input', fieldName: 'quoteNo', label: '报价单编号' },
    {
      component: 'Select',
      componentProps: () => customerSelectProps(),
      fieldName: 'customerId',
      label: '客户',
    },
    {
      component: 'Select',
      componentProps: { allowClear: true, options: [...QUOTE_STATUSES] },
      fieldName: 'status',
      label: '状态',
    },
  ];
}

/** 报价单列 */
export function useColumns<T = ProductApi.Quote>(
  onActionClick: OnActionClickFn<T>,
): VxeTableGridColumns {
  const { can } = usePermission();
  const canUpdate = can(QUOTE_PERMISSIONS.update);
  const canDelete = can(QUOTE_PERMISSIONS.delete);
  return [
    { field: 'quoteNo', minWidth: 180, title: '报价单编号' },
    { field: 'customerName', minWidth: 150, title: '客户' },
    { field: 'totalAmount', title: '总金额', width: 130 },
    {
      cellRender: {
        name: 'CellTag',
        options: QUOTE_STATUSES.map((s) => ({
          label: s.label,
          type: STATUS_TAG_TYPES[s.value],
          value: s.value,
        })),
      },
      field: 'status',
      title: '状态',
      width: 100,
    },
    { field: 'validUntil', title: '有效期至', width: 120 },
    { field: 'createTime', title: '创建时间', width: 165 },
    {
      align: 'center',
      cellRender: {
        attrs: {
          nameField: 'quoteNo',
          nameTitle: '报价单',
          onClick: onActionClick,
        },
        name: 'CellOperation',
        options: [
          { code: 'detail', text: '详情' },
          { code: 'edit', show: canUpdate },
          { code: 'delete', show: canDelete },
        ],
      },
      field: 'operation',
      fixed: 'right',
      title: '操作',
      width: 200,
    },
  ];
}
