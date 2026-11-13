<script lang="ts" setup>
import type { CustomerApi } from '#/api/crm/customer';

import { computed, ref } from 'vue';

import { useVbenDrawer } from '@vben/common-ui';

import { useVbenForm } from '#/adapter/form';
import { createCustomer, updateCustomer } from '#/api/crm/customer';

import { useFormSchema } from '../data';

const emit = defineEmits<{ success: [] }>();

const formData = ref<null | CustomerApi.Customer>(null);

const getTitle = computed(() =>
  formData.value?.id ? '编辑客户' : '新增客户',
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
    const payload: CustomerApi.CustomerPayload = {
      creditCode: values.creditCode || undefined,
      industry: values.industry ?? undefined,
      level: values.level ?? undefined,
      name: values.name,
      region: values.region ?? undefined,
      remark: values.remark || undefined,
      scale: values.scale ?? undefined,
    };
    try {
      if (formData.value?.id) {
        await updateCustomer(formData.value.id, payload);
      } else {
        await createCustomer(payload);
      }
      emit('success');
      drawerApi.close();
    } finally {
      drawerApi.lock(false);
    }
  },
  onOpenChange(isOpen) {
    if (isOpen) {
      const data = drawerApi.getData<CustomerApi.Customer>();
      formData.value = data && data.id ? data : null;
      formApi.resetForm();
      if (formData.value?.id) {
        formApi.setValues({
          creditCode: formData.value.creditCode,
          industry: formData.value.industry,
          level: formData.value.level,
          name: formData.value.name,
          region: formData.value.region,
          remark: formData.value.remark,
          scale: formData.value.scale,
        });
      }
    }
  },
});
</script>

<template>
  <Drawer :title="getTitle" class="w-[520px]">
    <Form />
  </Drawer>
</template>
