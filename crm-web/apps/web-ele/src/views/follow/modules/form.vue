<script lang="ts" setup>
import type { FollowApi } from '#/api/crm/follow';

import { computed, ref } from 'vue';

import { useVbenDrawer } from '@vben/common-ui';

import { useVbenForm } from '#/adapter/form';
import { createFollowRecord, updateFollowRecord } from '#/api/crm/follow';

import { useFormSchema } from '../data';

const emit = defineEmits<{ success: [] }>();

const formData = ref<null | FollowApi.FollowRecord>(null);

const getTitle = computed(() =>
  formData.value?.id ? '编辑跟进记录' : '新增跟进记录',
);

const [Form, formApi] = useVbenForm({
  layout: 'vertical',
  schema: useFormSchema(false),
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
    try {
      if (formData.value?.id) {
        // 编辑：关联对象不可变更，仅提交内容 / 类型 / 下次跟进时间
        const payload: FollowApi.FollowUpdatePayload = {
          content: values.content,
          followType: values.followType,
          nextFollowTime: values.nextFollowTime || undefined,
        };
        await updateFollowRecord(formData.value.id, payload);
      } else {
        const payload: FollowApi.FollowPayload = {
          bizId: values.bizId,
          bizType: values.bizType,
          content: values.content,
          followType: values.followType,
          nextFollowTime: values.nextFollowTime || undefined,
        };
        await createFollowRecord(payload);
      }
      emit('success');
      drawerApi.close();
    } finally {
      drawerApi.lock(false);
    }
  },
  onOpenChange(isOpen) {
    if (!isOpen) {
      return;
    }
    const data = drawerApi.getData<FollowApi.FollowRecord>();
    formData.value = data && data.id ? data : null;
    formApi.resetForm();
    formApi.setState({ schema: useFormSchema(formData.value?.id != null) });
    if (formData.value?.id) {
      formApi.setValues({
        bizId: formData.value.bizId,
        bizType: formData.value.bizType,
        content: formData.value.content,
        followType: formData.value.followType,
        nextFollowTime: formData.value.nextFollowTime,
      });
    }
  },
});
</script>

<template>
  <Drawer :title="getTitle" class="w-[520px]">
    <Form />
  </Drawer>
</template>
