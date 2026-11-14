<script lang="ts" setup>
import { computed, ref } from 'vue';

import { useVbenDrawer } from '@vben/common-ui';

import { useVbenForm } from '#/adapter/form';
import { createTask, updateTask } from '#/api/crm/task';

import { useFormSchema } from '../data';

const emit = defineEmits<{ success: [] }>();

const formData = ref<null | TaskApi.Task>(null);

const getTitle = computed(() =>
  formData.value?.id ? '编辑任务' : '新增任务',
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
    const payload: TaskApi.TaskPayload = {
      content: values.content || undefined,
      dueTime: values.dueTime,
      title: values.title,
    };
    try {
      if (formData.value?.id) {
        await updateTask(formData.value.id, payload);
      } else {
        await createTask(payload);
      }
      emit('success');
      drawerApi.close();
    } finally {
      drawerApi.lock(false);
    }
  },
  onOpenChange(isOpen) {
    if (isOpen) {
      const data = drawerApi.getData<TaskApi.Task>();
      formData.value = data && data.id ? data : null;
      formApi.resetForm();
      if (formData.value?.id) {
        formApi.setValues({
          content: formData.value.content,
          dueTime: formData.value.dueTime,
          title: formData.value.title,
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
