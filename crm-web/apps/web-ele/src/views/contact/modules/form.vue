<script lang="ts" setup>
import { computed, ref } from 'vue';

import { useVbenDrawer } from '@vben/common-ui';

import { useVbenForm } from '#/adapter/form';
import {
  createContact,
  updateContact,
} from '#/api/crm/contact';
import { loadCustomerOptions, useFormSchema } from '../data';

const emit = defineEmits<{ success: [] }>();

const formData = ref<null | ContactApi.Contact>(null);

const getTitle = computed(() =>
  formData.value?.id ? '编辑联系人' : '新增联系人',
);

const [Form, formApi] = useVbenForm({
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
    const payload: ContactApi.ContactPayload = {
      customerId: values.customerId,
      email: values.email || undefined,
      isPrimary: values.isPrimary === 1,
      name: values.name,
      phone: values.phone || undefined,
      position: values.position || undefined,
      remark: values.remark || undefined,
    };
    try {
      if (formData.value?.id) {
        await updateContact(formData.value.id, payload);
      } else {
        await createContact(payload);
      }
      emit('success');
      drawerApi.close();
    } finally {
      drawerApi.lock(false);
    }
  },
  async onOpenChange(isOpen) {
    if (isOpen) {
      await loadCustomerOptions();
      const data = drawerApi.getData<ContactApi.Contact>();
      formData.value = data && data.id ? data : null;
      formApi.resetForm();
      // 编辑时表单 schema 切换为编辑模式（客户不可变更）
      formApi.setState({ schema: useFormSchema(formData.value?.id != null) });
      if (formData.value?.id) {
        formApi.setValues({
          customerId: formData.value.customerId,
          email: formData.value.email,
          isPrimary: formData.value.isPrimary ? 1 : 0,
          name: formData.value.name,
          phone: formData.value.phone,
          position: formData.value.position,
          remark: formData.value.remark,
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
