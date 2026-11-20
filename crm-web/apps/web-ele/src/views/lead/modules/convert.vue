<script lang="ts" setup>
import { computed, ref } from 'vue';

import { useVbenDrawer } from '@vben/common-ui';

import { ElMessageBox } from 'element-plus';

import { useVbenForm } from '#/adapter/form';
import { convertLead, getContactList } from '#/api/crm/contact';
import { getCustomerList } from '#/api/crm/customer';
import { useConvertSchema } from '../data';

const emit = defineEmits<{ success: [] }>();

const lead = ref<null | Record<string, any>>(null);
const customerOptions = ref<Array<{ id: number; name: string }>>([]);
const contactOptions = ref<Array<{ id: number; name: string }>>([]);

const getTitle = computed(() =>
  lead.value ? `转化线索「${lead.value.name}」` : '转化线索',
);

const [Form, formApi] = useVbenForm({
  handleValuesChange: async (values: Record<string, any>) => {
    if (values.customerMode === 'link' && values.customerId) {
      const list = await getContactList({ customerId: values.customerId, pageSize: 100 });
      contactOptions.value = list.items.map((item: any) => ({
        id: item.id,
        name: item.name,
      }));
    }
  },
  layout: 'vertical',
  schema: useConvertSchema(customerOptions, contactOptions),
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
  async onOpenChange(isOpen) {
    if (isOpen) {
      lead.value = drawerApi.getData<Record<string, any>>();
      formApi.resetForm();
      const customers = await getCustomerList({ pageSize: 100 });
      customerOptions.value = customers.items.map((item: any) => ({
        id: item.id,
        name: item.name,
      }));
    }
  },
});

</script>

<template>
  <Drawer :title="getTitle" class="w-[520px]">
    <Form />
  </Drawer>
</template>
