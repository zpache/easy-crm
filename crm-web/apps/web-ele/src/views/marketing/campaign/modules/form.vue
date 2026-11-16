<script lang="ts" setup>
import type { MarketingApi } from '#/api/crm/marketing';

import { computed, ref } from 'vue';

import { useVbenDrawer } from '@vben/common-ui';

import { ElMessage } from 'element-plus';

import { useVbenForm } from '#/adapter/form';
import { createCampaign, updateCampaign } from '#/api/crm/marketing';

import { useFormSchema } from '../data';

const emit = defineEmits<{ success: [] }>();

const formData = ref<null | MarketingApi.Campaign>(null);

const getTitle = computed(() =>
  formData.value?.id ? '编辑营销活动' : '新增营销活动',
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
    const payload: MarketingApi.CampaignPayload = {
      budget: values.budget == null ? 0 : Number(values.budget),
      channel: values.channel || undefined,
      code: values.code || undefined,
      endDate: values.endDate || undefined,
      name: values.name,
      remark: values.remark || undefined,
      startDate: values.startDate || undefined,
      status: values.status ?? 0,
      targetLeads: values.targetLeads == null ? 0 : Number(values.targetLeads),
      type: values.type ?? 1,
    };
    drawerApi.lock();
    try {
      if (formData.value?.id) {
        await updateCampaign(formData.value.id, payload);
      } else {
        await createCampaign(payload);
      }
      ElMessage.success('已保存');
      emit('success');
      drawerApi.close();
    } finally {
      drawerApi.lock(false);
    }
  },
  onOpenChange(isOpen) {
    if (isOpen) {
      const data = drawerApi.getData<MarketingApi.Campaign>();
      formData.value = data && data.id ? data : null;
      formApi.resetForm();
      if (formData.value) {
        formApi.setValues({
          budget: formData.value.budget,
          channel: formData.value.channel,
          code: formData.value.code,
          endDate: formData.value.endDate,
          name: formData.value.name,
          remark: formData.value.remark,
          startDate: formData.value.startDate,
          status: formData.value.status,
          targetLeads: formData.value.targetLeads,
          type: formData.value.type,
        });
      } else {
        formApi.setValues({ status: 0, type: 1 });
      }
    }
  },
});
</script>

<template>
  <Drawer :title="getTitle" class="w-[560px]">
    <Form />
  </Drawer>
</template>
