<script lang="ts" setup>
import type { TicketApi } from '#/api/crm/support';
import type { VbenFormSchema } from '#/adapter/form';

import { ref } from 'vue';

import { useVbenDrawer } from '@vben/common-ui';

import { useVbenForm, z } from '#/adapter/form';
import { assignTicket } from '#/api/crm/support';

import { userOptions } from '../data';

const emit = defineEmits<{ success: [] }>();

const ticket = ref<null | TicketApi.Ticket>(null);

function useAssignSchema(): VbenFormSchema[] {
  return [
    {
      component: 'Select',
      componentProps: () => ({
        class: 'w-full',
        filterOption: (input: string, option: any) =>
          option.label.includes(input),
        options: userOptions.value.map((u) => ({
          label: u.name,
          value: u.id,
        })),
        placeholder: '请选择受理人',
        showSearch: true,
      }),
      fieldName: 'ownerId',
      label: '受理人',
      rules: z.number({ message: '请选择受理人' }),
    },
    {
      component: 'Textarea',
      componentProps: { maxLength: 500, rows: 3, showCount: true },
      fieldName: 'remark',
      label: '备注',
    },
  ];
}

const [Form, formApi] = useVbenForm({
  layout: 'vertical',
  schema: useAssignSchema(),
  showDefaultActions: false,
});

const [Drawer, drawerApi] = useVbenDrawer({
  async onConfirm() {
    const { valid } = await formApi.validate();
    if (!valid) {
      return;
    }
    const values = await formApi.getValues();
    drawerApi.lock();
    try {
      await assignTicket(ticket.value!.id, {
        ownerId: values.ownerId,
        remark: values.remark || undefined,
      });
      emit('success');
      drawerApi.close();
    } finally {
      drawerApi.lock(false);
    }
  },
  onOpenChange(isOpen) {
    if (isOpen) {
      ticket.value = drawerApi.getData<TicketApi.Ticket>();
      formApi.resetForm();
    }
  },
});
</script>

<template>
  <Drawer
    :title="`派单 - ${ticket?.ticketNo ?? ''}`"
    class="w-[460px]"
  >
    <div class="mb-3 text-sm text-gray-500">
      待受理工单派单后状态变为「已派单」；已派单 / 处理中工单再次派单为「改派」。
    </div>
    <Form />
  </Drawer>
</template>
