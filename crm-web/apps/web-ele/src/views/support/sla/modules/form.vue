<script lang="ts" setup>
import type { SlaApi } from '#/api/crm/support';

import { computed, ref } from 'vue';

import { useVbenDrawer } from '@vben/common-ui';

import { useVbenForm } from '#/adapter/form';
import { createSlaPolicy, updateSlaPolicy } from '#/api/crm/support';

import { useFormSchema } from '../data';

const emit = defineEmits<{ success: [] }>();

const formData = ref<null | SlaApi.SlaPolicy>(null);

const getTitle = computed(() =>
  formData.value?.id ? '编辑 SLA 策略' : '新增 SLA 策略',
);

const [Form, formApi] = useVbenForm({
  layout: 'vertical',
  schema: useFormSchema(),
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
    const payload: SlaApi.SlaPolicyPayload = {
      enabled: values.enabled ?? 1,
      priority: values.priority,
      resolveHours: values.resolveHours,
      responseHours: values.responseHours,
      ticketType: values.ticketType,
    };
    try {
      if (formData.value?.id) {
        await updateSlaPolicy(formData.value.id, payload);
      } else {
        await createSlaPolicy(payload);
      }
      emit('success');
      drawerApi.close();
    } finally {
      drawerApi.lock(false);
    }
  },
  onOpenChange(isOpen) {
    if (isOpen) {
      const data = drawerApi.getData<SlaApi.SlaPolicy>();
      formData.value = data && data.id ? data : null;
      formApi.resetForm();
      if (formData.value?.id) {
        formApi.setValues({
          enabled: formData.value.enabled,
          priority: formData.value.priority,
          resolveHours: formData.value.resolveHours,
          responseHours: formData.value.responseHours,
          ticketType: formData.value.ticketType,
        });
      }
    }
  },
});
</script>

<template>
  <Drawer :title="getTitle" class="w-[480px]">
    <Form />
  </Drawer>
</template>
