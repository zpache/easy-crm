<script lang="ts" setup>
import type { PaymentApi } from '#/api/crm/payment';

import { computed, ref } from 'vue';

import { useVbenDrawer } from '@vben/common-ui';

import { useVbenForm } from '#/adapter/form';
import { createPayment, updatePayment } from '#/api/crm/payment';

import { usePaymentFormSchema } from '../data';

const emit = defineEmits<{ success: [] }>();

const formData = ref<null | PaymentApi.Payment>(null);

const getTitle = computed(() =>
  formData.value?.id ? '编辑回款记录' : '登记回款',
);

const [Form, formApi] = useVbenForm({
  layout: 'vertical',
  schema: usePaymentFormSchema(),
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
    const payload: PaymentApi.PaymentPayload = {
      amount: values.amount,
      orderId: values.orderId,
      paymentDate: values.paymentDate || undefined,
      planId: values.planId ?? undefined,
      remark: values.remark || undefined,
    };
    try {
      if (formData.value?.id) {
        await updatePayment(formData.value.id, payload);
      } else {
        await createPayment(payload);
      }
      emit('success');
      drawerApi.close();
    } finally {
      drawerApi.lock(false);
    }
  },
  onOpenChange(isOpen) {
    if (isOpen) {
      const data = drawerApi.getData<PaymentApi.Payment>();
      formData.value = data && data.id ? data : null;
      formApi.resetForm();
      if (formData.value?.id) {
        formApi.setValues({
          amount: formData.value.amount,
          orderId: formData.value.orderId,
          paymentDate: formData.value.paymentDate,
          planId: formData.value.planId,
          remark: formData.value.remark,
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
