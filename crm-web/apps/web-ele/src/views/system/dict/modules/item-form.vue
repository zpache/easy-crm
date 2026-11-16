<script lang="ts" setup>
import type { SystemDictApi } from '#/api/system/dict';

import { computed, ref } from 'vue';

import { useVbenDrawer } from '@vben/common-ui';

import { useVbenForm } from '#/adapter/form';
import { createDictItem, updateDictItem } from '#/api/system/dict';
import { $t } from '#/locales';

import { useItemFormSchema } from '../data';

const emit = defineEmits<{ success: [] }>();

const formData = ref<null | SystemDictApi.DictItem>(null);
/** 新增时携带所属字典（用于回显与提交归属） */
const parentDict = ref<null | SystemDictApi.Dict>(null);

const isEdit = computed(() => !!formData.value?.id);

const getTitle = computed(() =>
  formData.value?.id ? '编辑字典项' : $t('ui.actionTitle.create', ['字典项']),
);

const [Form, formApi] = useVbenForm({
  layout: 'vertical',
  schema: useItemFormSchema(isEdit.value),
  showDefaultActions: false,
});

const [Drawer, drawerApi] = useVbenDrawer({
  async onConfirm() {
    const { valid } = await formApi.validate();
    if (!valid) {
      return;
    }
    drawerApi.lock();
    const data = await formApi.getValues();
    try {
      const payload = {
        label: data.label,
        remark: data.remark,
        sortNo: data.sortNo ?? 0,
        status: data.status ?? 1,
      };
      await (formData.value?.id
        ? updateDictItem(formData.value.id, payload)
        : createDictItem(parentDict.value?.id as number, payload));
      drawerApi.close();
      emit('success');
    } finally {
      drawerApi.lock(false);
    }
  },
  onOpenChange(isOpen) {
    if (isOpen) {
      const data = drawerApi.getData<SystemDictApi.DictItem | SystemDictApi.Dict>();
      if (data && 'dictType' in data && data.id && (data as SystemDictApi.DictItem).label) {
        // 编辑：字典项
        formData.value = data as SystemDictApi.DictItem;
        parentDict.value = null;
      } else {
        // 新增：传入所属字典
        formData.value = null;
        parentDict.value = (data as SystemDictApi.Dict) ?? null;
      }
      formApi.setState({ schema: useItemFormSchema(isEdit.value) });
      formApi.resetForm();
      if (formData.value?.id) {
        formApi.setValues({
          label: formData.value.label,
          remark: formData.value.remark,
          sortNo: formData.value.sortNo ?? 0,
          status: formData.value.status ?? 1,
          value: formData.value.value,
        });
      }
    }
  },
});
</script>

<template>
  <Drawer :title="getTitle" class="mx-2 w-[480px]">
    <Form class="mx-4" />
  </Drawer>
</template>
