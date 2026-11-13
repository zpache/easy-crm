<script lang="ts" setup>
import type { LeadApi } from '#/api/crm/lead';

import { computed, ref } from 'vue';

import { useVbenDrawer } from '@vben/common-ui';

import { ElAlert, ElButton } from 'element-plus';

import { useVbenForm } from '#/adapter/form';
import { createLead, updateLead } from '#/api/crm/lead';

import { useFormSchema } from '../data';

const emit = defineEmits<{ success: [] }>();

const formData = ref<null | LeadApi.Lead>(null);
/** 编辑前查到的同名线索提示（创建/更新后由后端返回） */
const duplicateNames = ref<LeadApi.Lead['duplicates']>([]);

const getTitle = computed(() =>
  formData.value?.id ? '编辑线索' : '新增线索',
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
    const payload: LeadApi.LeadPayload = {
      contactName: values.contactName || undefined,
      email: values.email || undefined,
      name: values.name,
      phone: values.phone || undefined,
      remark: values.remark || undefined,
      source: values.source ?? undefined,
    };
    try {
      const result: LeadApi.Lead = await (formData.value?.id
        ? updateLead(formData.value.id, payload)
        : createLead(payload));
      if (result?.companyDuplicate) {
        duplicateNames.value = result.duplicates ?? [];
      } else {
        emit('success');
        drawerApi.close();
      }
    } finally {
      drawerApi.lock(false);
    }
  },
  onOpenChange(isOpen) {
    if (isOpen) {
      const data = drawerApi.getData<LeadApi.Lead>();
      formData.value = data && data.id ? data : null;
      duplicateNames.value = [];
      formApi.resetForm();
      if (formData.value?.id) {
        formApi.setValues({
          contactName: formData.value.contactName,
          email: formData.value.email,
          name: formData.value.name,
          phone: formData.value.phone,
          remark: formData.value.remark,
          source: formData.value.source,
        });
      }
    }
  },
});

/**
 * 同名提示确认：仍保存（重复仅提示不阻断）
 */
function confirmDuplicate() {
  emit('success');
  duplicateNames.value = [];
  drawerApi.close();
}
</script>

<template>
  <Drawer :title="getTitle" class="w-[520px]">
    <Form />
    <ElAlert
      v-if="duplicateNames.length > 0"
      :closable="false"
      class="mt-3"
      show-icon
      title="发现同名线索"
      type="warning"
    >
      <div class="text-xs">
        <div v-for="item in duplicateNames" :key="item.id">
          「{{ item.name }}」（ID: {{ item.id }}）
        </div>
        <div class="mt-2">请确认是否重复录入。</div>
      </div>
    </ElAlert>
    <div v-if="duplicateNames.length > 0" class="mt-3 flex justify-end gap-2">
      <ElButton size="small" @click="drawerApi.close()">返回修改</ElButton>
      <ElButton size="small" type="primary" @click="confirmDuplicate">
        确认不重复，仍保存
      </ElButton>
    </div>
  </Drawer>
</template>
