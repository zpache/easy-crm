<script lang="ts" setup>
import type { OpportunityApi } from '#/api/crm/opportunity';

import { computed, ref } from 'vue';

import { useVbenDrawer } from '@vben/common-ui';

import { useVbenForm } from '#/adapter/form';
import {
  createOpportunity,
  updateOpportunity,
} from '#/api/crm/opportunity';
import {
  loadContactsForCustomer,
  useFormSchema,
} from '../data';

const emit = defineEmits<{ success: [] }>();

const formData = ref<null | OpportunityApi.Opportunity>(null);

const getTitle = computed(() =>
  formData.value?.id ? '编辑商机' : '新增商机',
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
    const payload: OpportunityApi.OpportunityPayload = {
      amount: values.amount ?? null,
      contactId: values.contactId ?? null,
      expectedCloseDate: values.expectedCloseDate || undefined,
      name: values.name,
    };
    try {
      if (formData.value?.id) {
        await updateOpportunity(formData.value.id, payload);
      } else {
        await createOpportunity({
          ...payload,
          customerId: values.customerId,
        });
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
    const data = drawerApi.getData<OpportunityApi.Opportunity>();
    formData.value = data && data.id ? data : null;
    formApi.resetForm();
    formApi.setState({ schema: useFormSchema(formData.value?.id != null) });
    if (formData.value?.id) {
      await loadContactsForCustomer(formData.value.customerId);
      formApi.setValues({
        amount: formData.value.amount,
        contactId: formData.value.contactId,
        customerId: formData.value.customerId,
        expectedCloseDate: formData.value.expectedCloseDate,
        name: formData.value.name,
      });
    }
  },
});
</script>

<template>
  <Drawer :title="getTitle" class="w-[520px]">
    <Form />
  </Drawer>
</template>
