<script lang="ts" setup>
import type { ProductApi } from '#/api/crm/product';

import { computed, ref } from 'vue';

import { useVbenDrawer } from '@vben/common-ui';

import { useVbenForm } from '#/adapter/form';
import { createProduct, updateProduct } from '#/api/crm/product';

import { useFormSchema } from '../data';

const emit = defineEmits<{ success: [] }>();

const formData = ref<null | ProductApi.Product>(null);

const getTitle = computed(() =>
  formData.value?.id ? '编辑产品' : '新增产品',
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
    const payload: ProductApi.ProductPayload = {
      category: values.category || undefined,
      code: values.code,
      costPrice: values.costPrice ?? undefined,
      name: values.name,
      remark: values.remark || undefined,
      specification: values.specification || undefined,
      standardPrice: values.standardPrice ?? undefined,
      status: values.status ?? 1,
      unit: values.unit || undefined,
    };
    try {
      if (formData.value?.id) {
        await updateProduct(formData.value.id, payload);
      } else {
        await createProduct(payload);
      }
      emit('success');
      drawerApi.close();
    } finally {
      drawerApi.lock(false);
    }
  },
  onOpenChange(isOpen) {
    if (isOpen) {
      const data = drawerApi.getData<ProductApi.Product>();
      formData.value = data && data.id ? data : null;
      formApi.resetForm();
      if (formData.value?.id) {
        formApi.setValues({
          category: formData.value.category,
          code: formData.value.code,
          costPrice: formData.value.costPrice,
          name: formData.value.name,
          remark: formData.value.remark,
          specification: formData.value.specification,
          standardPrice: formData.value.standardPrice,
          status: formData.value.status,
          unit: formData.value.unit,
        });
      }
    }
  },
});
</script>

<template>
  <Drawer :title="getTitle" class="w-[520px]">
    <Form />
  </Drawer>
</template>
