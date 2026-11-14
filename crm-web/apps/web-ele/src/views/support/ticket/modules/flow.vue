<script lang="ts" setup>
import type { TicketApi } from '#/api/crm/support';
import type { VbenFormSchema } from '#/adapter/form';

import { ref } from 'vue';

import { useVbenDrawer } from '@vben/common-ui';

import { useVbenForm, z } from '#/adapter/form';
import {
  changeTicketStatus,
  closeTicket,
  statusLabel,
  TICKET_STATUSES,
  TICKET_TRANSITIONS,
} from '#/api/crm/support';

const emit = defineEmits<{ success: [] }>();

const ticket = ref<null | TicketApi.Ticket>(null);

function useFlowSchema(): VbenFormSchema[] {
  const allowed = TICKET_TRANSITIONS[ticket.value?.status ?? ''] ?? [];
  const options = TICKET_STATUSES.filter((s) =>
    (allowed as readonly string[]).includes(s.value),
  ).map((s) => ({ label: s.label, value: s.value }));
  return [
    {
      component: 'Select',
      componentProps: {
        class: 'w-full',
        options,
        placeholder: '请选择目标状态',
      },
      fieldName: 'targetStatus',
      label: '目标状态',
      rules: z.string().min(1, '请选择目标状态'),
    },
    {
      component: 'Textarea',
      componentProps: { maxLength: 500, rows: 3, showCount: true },
      fieldName: 'remark',
      label: '流转备注',
    },
  ];
}

const [Form, formApi] = useVbenForm({
  layout: 'vertical',
  schema: [],
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
      if (values.targetStatus === 'CLOSED') {
        // 关闭走专用接口，与后端状态机的 CLOSED 转移等价
        await closeTicket(ticket.value!.id, {
          remark: values.remark || undefined,
        });
      } else {
        await changeTicketStatus(ticket.value!.id, {
          remark: values.remark || undefined,
          targetStatus: values.targetStatus,
        });
      }
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
      formApi.setState({ schema: useFlowSchema() });
    }
  },
});
</script>

<template>
  <Drawer
    :title="`状态流转 - ${ticket?.ticketNo ?? ''}`"
    class="w-[460px]"
  >
    <div class="mb-3 text-sm text-gray-500">
      当前状态：{{ statusLabel(ticket?.status) }}。终态（已完成 / 已关闭）不可再变更，
      选择「已关闭」将调用关闭接口。
    </div>
    <Form />
  </Drawer>
</template>
