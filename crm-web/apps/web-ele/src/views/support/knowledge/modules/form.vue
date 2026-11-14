<script lang="ts" setup>
import type { KnowledgeApi } from '#/api/crm/support';

import { computed, ref } from 'vue';

import { useVbenDrawer } from '@vben/common-ui';

import { useVbenForm } from '#/adapter/form';
import { createKnowledge, updateKnowledge } from '#/api/crm/support';

import { useFormSchema } from '../data';

const emit = defineEmits<{ success: [] }>();

const formData = ref<null | KnowledgeApi.Knowledge>(null);

const getTitle = computed(() =>
  formData.value?.id ? '编辑知识' : '新增知识',
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
    const payload: KnowledgeApi.KnowledgePayload = {
      category: values.category,
      content: values.content || undefined,
      publishStatus: values.publishStatus,
      tags: values.tags || undefined,
      title: values.title,
    };
    try {
      if (formData.value?.id) {
        await updateKnowledge(formData.value.id, payload);
      } else {
        await createKnowledge(payload);
      }
      emit('success');
      drawerApi.close();
    } finally {
      drawerApi.lock(false);
    }
  },
  onOpenChange(isOpen) {
    if (isOpen) {
      const data = drawerApi.getData<KnowledgeApi.Knowledge>();
      formData.value = data && data.id ? data : null;
      formApi.resetForm();
      if (formData.value?.id) {
        formApi.setValues({
          category: formData.value.category,
          content: formData.value.content,
          publishStatus: formData.value.publishStatus,
          tags: formData.value.tags,
          title: formData.value.title,
        });
      }
    }
  },
});
</script>

<template>
  <Drawer :title="getTitle" class="w-[620px]">
    <Form />
  </Drawer>
</template>
