<script lang="ts" setup>
import type { ProductApi } from '#/api/crm/product';

import { computed, onMounted, ref } from 'vue';

import { useVbenDrawer } from '@vben/common-ui';

import { ElMessage } from 'element-plus';

import { useVbenForm } from '#/adapter/form';
import { createPriceList, updatePriceList } from '#/api/crm/product';

import { loadProductOptions, useFormSchema } from '../data';

const emit = defineEmits<{ success: [] }>();

const formData = ref<null | ProductApi.PriceList>(null);

const getTitle = computed(() =>
  formData.value?.id ? '编辑价格' : '新增价格',
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
    const payload: ProductApi.PriceListPayload = {
      effectiveDate: values.effectiveDate || undefined,
      expireDate: values.expireDate || undefined,
      price: Number(values.price),
      productId: Number(values.productId),
      remark: values.remark || undefined,
      status: values.status ?? 1,
    };
    drawerApi.lock();
    try {
      if (formData.value?.id) {
        await updatePriceList(formData.value.id, payload);
      } else {
        await createPriceList(payload);
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
      const data = drawerApi.getData<ProductApi.PriceList>();
      formData.value = data && data.id ? data : null;
      formApi.resetForm();
      if (formData.value) {
        formApi.setValues({
          effectiveDate: formData.value.effectiveDate,
          expireDate: formData.value.expireDate,
          price: formData.value.price,
          productId: formData.value.productId,
          remark: formData.value.remark,
          status: formData.value.status ?? 1,
        });
      } else {
        formApi.setValues({ status: 1 });
      }
    }
  },
});

onMounted(async () => {
  await loadProductOptions();
});
</script>

<template>
  <Drawer :title="getTitle" class="w-[520px]">
    <Form />
  </Drawer>
</template>
