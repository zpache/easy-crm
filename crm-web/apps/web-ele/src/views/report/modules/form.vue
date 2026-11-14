<script lang="ts" setup>
import type { ReportApi } from '#/api/crm/report';

import { computed, ref } from 'vue';

import { useVbenDrawer } from '@vben/common-ui';

import { useVbenForm } from '#/adapter/form';
import { createSalesTarget, updateSalesTarget } from '#/api/crm/report';

import { useFormSchema } from '../data';

const emit = defineEmits<{ success: [] }>();

const formData = ref<null | ReportApi.SalesTarget>(null);

const getTitle = computed(() =>
  formData.value?.id ? '编辑销售目标' : '新增销售目标',
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
    const payload: ReportApi.SalesTargetPayload = {
      deptId: values.deptId ?? undefined,
      ownerId: values.ownerId ?? undefined,
      remark: values.remark || undefined,
      targetAmount: values.targetAmount,
      targetMonth: values.targetMonth,
    };
    try {
      if (formData.value?.id) {
        await updateSalesTarget(formData.value.id, payload);
      } else {
        await createSalesTarget(payload);
      }
      emit('success');
      drawerApi.close();
    } finally {
      drawerApi.lock(false);
    }
  },
  onOpenChange(isOpen) {
    if (isOpen) {
      const data = drawerApi.getData<ReportApi.SalesTarget>();
      formData.value = data && data.id ? data : null;
      formApi.resetForm();
      if (formData.value?.id) {
        formApi.setValues({
          deptId: formData.value.deptId,
          ownerId: formData.value.ownerId,
          remark: formData.value.remark,
          targetAmount: formData.value.targetAmount,
          targetMonth: formData.value.targetMonth,
        });
      }
    }
  },
});
</script>

<template>
  <Drawer :title="getTitle" class="w-[500px]">
    <Form />
  </Drawer>
</template>
