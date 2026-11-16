<script lang="ts" setup>
import type { PaymentApi } from '#/api/crm/payment';

import { computed, onMounted, ref } from 'vue';

import { useVbenDrawer } from '@vben/common-ui';

import { ElMessage } from 'element-plus';

import { useVbenForm } from '#/adapter/form';
import { createInvoice, updateInvoice } from '#/api/crm/payment';

import { customerIdOfOrder, loadCustomerOptions, loadOrderOptions } from '../../shared/options';
import { useFormSchema } from '../data';

const emit = defineEmits<{ success: [] }>();

const formData = ref<null | PaymentApi.Invoice>(null);

const getTitle = computed(() =>
  formData.value?.id ? '编辑开票申请' : '新增开票申请',
);

const [Form, formApi] = useVbenForm({
  handleValuesChange: async (values: Record<string, any>) => {
    // 选择订单后联动带出客户，减少手工填写与错挂客户的概率
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
    const payload: PaymentApi.InvoicePayload = {
      amount: Number(values.amount),
      applyDate: values.applyDate || undefined,
      customerId: Number(values.customerId),
      invoiceNo: values.invoiceNo || undefined,
      orderId: Number(values.orderId),
      remark: values.remark || undefined,
      taxNo: values.taxNo || undefined,
      title: values.title || undefined,
    };
    drawerApi.lock();
    try {
      if (formData.value?.id) {
        await updateInvoice(formData.value.id, payload);
      } else {
        await createInvoice(payload);
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
      const data = drawerApi.getData<PaymentApi.Invoice>();
      formData.value = data && data.id ? data : null;
      formApi.resetForm();
      if (formData.value) {
        formApi.setValues({
          amount: formData.value.amount,
          applyDate: formData.value.applyDate,
          customerId: formData.value.customerId,
          invoiceNo: formData.value.invoiceNo,
          orderId: formData.value.orderId,
          remark: formData.value.remark,
          taxNo: formData.value.taxNo,
          title: formData.value.title,
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
