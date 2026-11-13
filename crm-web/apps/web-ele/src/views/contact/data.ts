import type { VbenFormSchema } from '#/adapter/form';
import type { OnActionClickFn, VxeTableGridColumns } from '#/adapter/vxe-table';
import type { ContactApi } from '#/api/crm/contact';

import { ref } from 'vue';

import { z } from '#/adapter/form';
import { getCustomerList } from '#/api/crm/customer';
import { usePermission } from '#/composables/use-permission';
import { CONTACT_PERMISSIONS } from '#/constants/permissions';

/** 客户候选选项（模块级共享，list.vue 启动时加载一次） */
export const customerOptions = ref<Array<{ id: number; name: string }>>([]);

/** 加载客户候选选项（前 100 个） */
export async function loadCustomerOptions() {
  const result = await getCustomerList({ pageSize: 100 });
  customerOptions.value = result.items.map((item: any) => ({
    id: item.id,
    name: item.name,
  }));
}

function customerSelectProps() {
  return {
    class: 'w-full',
    filterOption: (input: string, option: any) => option.label.includes(input),
    options: customerOptions.value.map((c) => ({
      label: c.name,
      value: c.id,
    })),
    showSearch: true,
  };
}

/**
 * 联系人创建 / 编辑表单（编辑时客户不可变更）
 */
export function useFormSchema(isEdit: boolean): VbenFormSchema[] {
  return [
    {
      component: 'Select',
      componentProps: () => ({ ...customerSelectProps(), disabled: isEdit }),
      fieldName: 'customerId',
      label: '所属客户',
      rules: isEdit ? undefined : z.number({ message: '请选择所属客户' }),
    },
    {
      component: 'Input',
      fieldName: 'name',
      label: '姓名',
      rules: z
        .string()
        .min(1, '姓名不能为空')
        .max(64, '姓名不能超过 64 个字符'),
    },
    { component: 'Input', fieldName: 'phone', label: '手机号' },
    { component: 'Input', fieldName: 'email', label: '邮箱' },
    { component: 'Input', fieldName: 'position', label: '职位' },
    {
      component: 'RadioGroup',
      componentProps: {
        buttonStyle: 'solid',
        defaultValue: 0,
        options: [
          { label: '普通联系人', value: 0 },
          { label: '主联系人', value: 1 },
        ],
      },
      fieldName: 'isPrimary',
      help: '同一客户下主联系人唯一，标记后其余主联系人自动取消',
      label: '联系人类型',
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
 * 查询表单
 */
export function useGridFormSchema(): VbenFormSchema[] {
  return [
    {
      component: 'Select',
      componentProps: () => ({
        allowClear: true,
        ...customerSelectProps(),
      }),
      fieldName: 'customerId',
      label: '所属客户',
    },
    { component: 'Input', fieldName: 'name', label: '姓名' },
    { component: 'Input', fieldName: 'phone', label: '手机号' },
  ];
}

/**
 * 联系人表格列
 */
export function useColumns<T = ContactApi.Contact>(
  onActionClick: OnActionClickFn<T>,
): VxeTableGridColumns {
  const { can } = usePermission();
  const canUpdate = can(CONTACT_PERMISSIONS.update);
  const canDelete = can(CONTACT_PERMISSIONS.delete);
  const canPrimary = can(CONTACT_PERMISSIONS.primary);
  return [
    { field: 'name', minWidth: 120, title: '姓名' },
    { field: 'customerName', minWidth: 140, title: '所属客户' },
    { field: 'phone', title: '手机号', width: 130 },
    { field: 'email', minWidth: 140, title: '邮箱' },
    { field: 'position', title: '职位', width: 110 },
    {
      cellRender: {
        name: 'CellTag',
        options: [
          { label: '普通', type: 'info', value: false },
          { label: '主联系人', type: 'primary', value: true },
        ],
      },
      field: 'isPrimary',
      title: '类型',
      width: 100,
    },
    { field: 'remark', minWidth: 120, title: '备注' },
    {
      align: 'center',
      cellRender: {
        attrs: {
          nameField: 'name',
          nameTitle: '联系人',
          onClick: onActionClick,
        },
        name: 'CellOperation',
        options: [
          { code: 'edit', show: canUpdate },
          {
            code: 'setPrimary',
            show: (row: ContactApi.Contact) => canPrimary && !row.isPrimary,
            text: '设为主联系人',
          },
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
