<script lang="ts" setup>
import type { SystemDictApi } from '#/api/system/dict';

import { computed, ref } from 'vue';

import { useVbenModal } from '@vben/common-ui';

import { useVbenForm } from '#/adapter/form';
import { createDict, updateDict } from '#/api/system/dict';
import { $t } from '#/locales';

import { useFormSchema } from '../data';

const emit = defineEmits<{ success: [] }>();

const formData = ref<null | SystemDictApi.Dict>(null);

const isEdit = computed(() => !!formData.value?.id);

const getTitle = computed(() =>
  formData.value?.id ? '编辑字典' : $t('ui.actionTitle.create', ['字典']),
);

const [Form, formApi] = useVbenForm({
  layout: 'vertical',
  schema: useFormSchema(isEdit.value),
  showDefaultActions: false,
});

const [Modal, modalApi] = useVbenModal({
  async onConfirm() {
    const { valid } = await formApi.validate();
    if (!valid) {
      return;
    }
    modalApi.lock();
    const data = await formApi.getValues();
    try {
      const payload = {
        name: data.name,
        remark: data.remark,
        status: data.status ?? 1,
      };
      await (formData.value?.id
        ? updateDict(formData.value.id, payload)
        : createDict({ dictType: data.dictType, ...payload }));
      modalApi.close();
      emit('success');
    } finally {
      modalApi.lock(false);
    }
  },
  onOpenChange(isOpen) {
    if (isOpen) {
      const data = modalApi.getData<SystemDictApi.Dict>();
      formData.value = data && data.id ? data : null;
      formApi.setState({ schema: useFormSchema(isEdit.value) });
      formApi.resetForm();
      if (data && data.id) {
        formApi.setValues({
          dictType: data.dictType,
          name: data.name,
          remark: data.remark,
          status: data.status ?? 1,
        });
      }
    }
  },
});
</script>

<template>
  <Modal :title="getTitle">
    <Form class="mx-4" />
  </Modal>
</template>
