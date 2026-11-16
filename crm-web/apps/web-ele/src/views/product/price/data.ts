import type { VbenFormSchema } from '#/adapter/form';
import type { OnActionClickFn, VxeTableGridColumns } from '#/adapter/vxe-table';
import type { ProductApi } from '#/api/crm/product';

import { ref } from 'vue';

import { z } from '#/adapter/form';
import { getProductList } from '#/api/crm/product';
import { usePermission } from '#/composables/use-permission';
import { PRICE_PERMISSIONS } from '#/constants/crm-batch3-permissions';

/** 价目表状态：1 有效 / 0 失效 */
export const PRICE_STATUSES = [
  { label: '有效', value: 1 },
  { label: '失效', value: 0 },
] as const;

/** 产品候选选项（价目表按产品筛选与选择） */
export const productOptions = ref<Array<{ id: number; name: string }>>([]);

export async function loadProductOptions() {
  if (productOptions.value.length > 0) {
    return;
  }
  const result = await getProductList({ pageSize: 200 });
  productOptions.value = result.items.map((item: any) => ({
    id: item.id,
    name: `${item.name}${item.code ? `（${item.code}）` : ''}`,
  }));
}

function productSelectProps() {
  return {
    allowClear: true,
    class: 'w-full',
    filterOption: (input: string, option: any) =>
      String(option.label).includes(input),
    options: productOptions.value.map((p) => ({ label: p.name, value: p.id })),
    showSearch: true,
  };
}

/** 价目表创建 / 编辑表单 */
export function useFormSchema(): VbenFormSchema[] {
  return [
    {
      component: 'Select',
      componentProps: () => productSelectProps(),
      fieldName: 'productId',
      label: '产品',
      rules: z.number({ message: '请选择产品' }),
    },
    {
      component: 'InputNumber',
      componentProps: { class: 'w-full', min: 0, precision: 2 },
      fieldName: 'price',
      label: '价格',
      rules: z.number({ message: '价格不能为空' }),
    },
    {
      component: 'DatePicker',
      componentProps: {
        class: 'w-full',
        placeholder: '留空表示立即生效',
        valueFormat: 'YYYY-MM-DD',
      },
      fieldName: 'effectiveDate',
      label: '生效日期',
    },
    {
      component: 'DatePicker',
      componentProps: {
        class: 'w-full',
        placeholder: '留空表示长期有效',
        valueFormat: 'YYYY-MM-DD',
      },
      fieldName: 'expireDate',
      label: '失效日期',
    },
    {
      component: 'Select',
      componentProps: { class: 'w-full', options: [...PRICE_STATUSES] },
      fieldName: 'status',
      label: '状态',
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
    {
      component: 'Select',
      componentProps: () => productSelectProps(),
      fieldName: 'productId',
      label: '产品',
    },
    {
      component: 'Select',
      componentProps: { allowClear: true, options: [...PRICE_STATUSES] },
      fieldName: 'status',
      label: '状态',
    },
  ];
}

/** 价目表列 */
export function useColumns<T = ProductApi.PriceList>(
  onActionClick: OnActionClickFn<T>,
): VxeTableGridColumns {
  const { can } = usePermission();
  const canUpdate = can(PRICE_PERMISSIONS.update);
  const canDelete = can(PRICE_PERMISSIONS.delete);
  return [
    { field: 'productName', minWidth: 180, title: '产品' },
    { field: 'price', title: '价格', width: 130 },
    { field: 'effectiveDate', title: '生效日期', width: 120 },
    { field: 'expireDate', title: '失效日期', width: 120 },
    {
      cellRender: {
        name: 'CellTag',
        options: PRICE_STATUSES.map((s) => ({
          label: s.label,
          type: s.value === 1 ? 'success' : 'info',
          value: s.value,
        })),
      },
      field: 'status',
      title: '状态',
      width: 100,
    },
    { field: 'remark', minWidth: 160, title: '备注' },
    { field: 'createTime', title: '创建时间', width: 165 },
    {
      align: 'center',
      cellRender: {
        attrs: {
          nameField: 'productName',
          nameTitle: '价格',
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
