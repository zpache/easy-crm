<script lang="ts" setup>
import type { PaymentApi } from '#/api/crm/payment';

import { computed, ref } from 'vue';

import { useVbenDrawer } from '@vben/common-ui';
import { loadOrderOptions } from '../shared/options';

import { useVbenForm } from '#/adapter/form';
import { createPaymentPlan, updatePaymentPlan } from '#/api/crm/payment';

import { usePlanFormSchema } from '../data';

const emit = defineEmits<{ success: [] }>();

const formData = ref<null | PaymentApi.PaymentPlan>(null);

const getTitle = computed(() =>
  formData.value?.id ? '编辑回款计划' : '新增回款计划',
);

const [Form, formApi] = useVbenForm({
  layout: 'vertical',
  schema: usePlanFormSchema(),
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
    const payload: PaymentApi.PaymentPlanPayload = {
      orderId: values.orderId,
      periodNo: values.periodNo,
      planAmount: values.planAmount,
      planDate: values.planDate || undefined,
      remark: values.remark || undefined,
    };
    try {
      if (formData.value?.id) {
        await updatePaymentPlan(formData.value.id, payload);
      } else {
        await createPaymentPlan(payload);
      }
      emit('success');
      drawerApi.close();
    } finally {
      drawerApi.lock(false);
    }
  },
  async async onOpenChange(isOpen) {
    if (isOpen) {
      await loadOrderOptions();
      const data = drawerApi.getData<PaymentApi.PaymentPlan>();
      formData.value = data && data.id ? data : null;
      formApi.resetForm();
      if (formData.value?.id) {
        formApi.setValues({
          orderId: formData.value.orderId,
          periodNo: formData.value.periodNo,
          planAmount: formData.value.planAmount,
          planDate: formData.value.planDate,
          remark: formData.value.remark,
        });
      } else {
        formApi.setValues({ periodNo: 1 });
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
