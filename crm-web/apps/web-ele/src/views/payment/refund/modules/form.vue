<script lang="ts" setup>
import type { PaymentApi } from '#/api/crm/payment';

import { computed, onMounted, ref } from 'vue';

import { useVbenDrawer } from '@vben/common-ui';

import { ElMessage } from 'element-plus';

import { useVbenForm } from '#/adapter/form';
import { createRefund, updateRefund } from '#/api/crm/payment';

import {
  customerIdOfOrder,
  loadCustomerOptions,
  loadOrderOptions,
} from '../../shared/options';
import { useFormSchema } from '../data';

const emit = defineEmits<{ success: [] }>();

const formData = ref<null | PaymentApi.Refund>(null);

const getTitle = computed(() =>
  formData.value?.id ? '编辑退款申请' : '新增退款申请',
);

const [Form, formApi] = useVbenForm({
  handleValuesChange: async (values: Record<string, any>) => {
    const customerId = customerIdOfOrder(values.orderId);
    if (customerId && values.customerId !== customerId) {
      await formApi.setValues({ customerId });
    }
  },
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
    const payload: PaymentApi.RefundPayload = {
      amount: Number(values.amount),
      customerId: Number(values.customerId),
      orderId: Number(values.orderId),
      reason: values.reason || undefined,
      refundDate: values.refundDate || undefined,
      refundNo: values.refundNo || undefined,
      remark: values.remark || undefined,
    };
    drawerApi.lock();
    try {
      if (formData.value?.id) {
        await updateRefund(formData.value.id, payload);
      } else {
        await createRefund(payload);
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
      const data = drawerApi.getData<PaymentApi.Refund>();
      formData.value = data && data.id ? data : null;
      formApi.resetForm();
      if (formData.value) {
        formApi.setValues({
          amount: formData.value.amount,
          customerId: formData.value.customerId,
          orderId: formData.value.orderId,
          reason: formData.value.reason,
          refundDate: formData.value.refundDate,
          refundNo: formData.value.refundNo,
          remark: formData.value.remark,
        });
      }
    }
  },
});

onMounted(async () => {
  await Promise.all([loadCustomerOptions(), loadOrderOptions()]);
});
</script>

<template>
  <Drawer :title="getTitle" class="w-[520px]">
    <Form />
  </Drawer>
</template>
