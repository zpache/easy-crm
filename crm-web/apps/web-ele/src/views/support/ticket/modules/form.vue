<script lang="ts" setup>
import type { TicketApi } from '#/api/crm/support';

import { computed, ref } from 'vue';

import { useVbenDrawer } from '@vben/common-ui';

import { useVbenForm } from '#/adapter/form';
import { createTicket, updateTicket } from '#/api/crm/support';

import { loadContactsForCustomer, useFormSchema } from '../data';

const emit = defineEmits<{ success: [] }>();

const formData = ref<null | TicketApi.Ticket>(null);

const getTitle = computed(() =>
  formData.value?.id ? '编辑工单' : '新增工单',
);

const [Form, formApi] = useVbenForm({
  handleValuesChange: async (values: Record<string, any>) => {
    // 选择客户后联动加载该客户的联系人下拉
    await loadContactsForCustomer(values.customerId);
  },
  layout: 'vertical',
  schema: useFormSchema(false),
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
    const payload: TicketApi.TicketPayload = {
      contactId: values.contactId ?? null,
      customerId: values.customerId,
      description: values.description || undefined,
      orderId: values.orderId ?? null,
      priority: values.priority,
      source: values.source || undefined,
      ticketType: values.ticketType,
      title: values.title,
    };
    try {
      if (formData.value?.id) {
        // 编辑：客户 / 类型 / 优先级不可变，后端按 id 保留原值
        await updateTicket(formData.value.id, payload);
      } else {
        await createTicket(payload);
      }
      emit('success');
      drawerApi.close();
    } finally {
      drawerApi.lock(false);
    }
  },
  async onOpenChange(isOpen) {
    if (!isOpen) {
      return;
    }
    const data = drawerApi.getData<TicketApi.Ticket>();
    formData.value = data && data.id ? data : null;
    formApi.resetForm();
    formApi.setState({ schema: useFormSchema(formData.value?.id != null) });
    if (formData.value?.id) {
      await loadContactsForCustomer(formData.value.customerId);
      formApi.setValues({
        contactId: formData.value.contactId,
        customerId: formData.value.customerId,
        description: formData.value.description,
        orderId: formData.value.orderId,
        priority: formData.value.priority,
        source: formData.value.source,
        ticketType: formData.value.ticketType,
        title: formData.value.title,
      });
    }
  },
});
</script>

<template>
  <Drawer :title="getTitle" class="w-[560px]">
    <Form />
  </Drawer>
</template>
