<script lang="ts" setup>
import { computed, ref } from 'vue';

import { useVbenDrawer } from '@vben/common-ui';

import { ElMessageBox } from 'element-plus';

import { useVbenForm } from '#/adapter/form';
import { convertLead } from '#/api/crm/lead';

import { useConvertSchema } from '../data';

const emit = defineEmits<{ success: [] }>();

const lead = ref<null | Record<string, any>>(null);

const getTitle = computed(() =>
  lead.value ? `转化线索「${lead.value.name}」` : '转化线索',
);

const [Form, formApi] = useVbenForm({
  layout: 'vertical',
  schema: useConvertSchema(),
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
    const payload = {
      contactId: values.contactMode === 'link' ? values.contactId : null,
      contactInfo:
        values.contactMode === 'create'
          ? { name: values.contactName }
          : undefined,
      createContact: values.contactMode === 'create',
      createCustomer: values.customerMode === 'create',
      customerId: values.customerMode === 'link' ? values.customerId : null,
      customerInfo:
        values.customerMode === 'create'
          ? { name: values.customerName }
          : undefined,
      opportunityName: values.opportunityName || undefined,
    };
    try {
      const result = await convertLead(lead.value.id, payload);
      await ElMessageBox.alert(
        `商机 ID：${result.opportunityId}` +
          (result.customerId ? `；客户 ID：${result.customerId}` : '') +
          (result.contactId ? `；联系人 ID：${result.contactId}` : ''),
        '转化成功',
        { confirmButtonText: '知道了' },
      );
      emit('success');
      drawerApi.close();
    } finally {
      drawerApi.lock(false);
    }
  },
  onOpenChange(isOpen) {
    if (isOpen) {
      lead.value = drawerApi.getData<Record<string, any>>();
      formApi.resetForm();
    }
  },
});

</script>

<template>
  <Drawer :title="getTitle" class="w-[520px]">
    <Form />
  </Drawer>
</template>
