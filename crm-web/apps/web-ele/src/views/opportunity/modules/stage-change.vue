<script lang="ts" setup>
import { ref } from 'vue';

import { useVbenDrawer } from '@vben/common-ui';

import { ElMessageBox } from 'element-plus';

import { useVbenForm } from '#/adapter/form';
import { changeStage } from '#/api/crm/opportunity';

import { stageOptions, useStageChangeSchema } from '../data';

const emit = defineEmits<{ success: [] }>();

const opportunity = ref<null | Record<string, any>>(null);

const [Form, formApi] = useVbenForm({
  layout: 'vertical',
  schema: useStageChangeSchema(),
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
      await changeStage(opportunity.value.id, {
        actualAmount: values.actualAmount ?? null,
        actualCloseDate: values.actualCloseDate || undefined,
        competitor: values.competitor || undefined,
        lostReason: values.lostReason || undefined,
        targetStageCode: values.targetStageCode,
      });
      await ElMessageBox.alert('阶段已变更，事件将在事务提交后触发', '操作成功', {
        confirmButtonText: '知道了',
      });
      emit('success');
      drawerApi.close();
    } finally {
      drawerApi.lock(false);
    }
  },
  onOpenChange(isOpen) {
    if (isOpen) {
      opportunity.value = drawerApi.getData<Record<string, any>>();
      formApi.resetForm();
    }
  },
});
</script>

<template>
  <Drawer
    :title="`变更阶段 - ${opportunity?.name ?? ''}`"
    class="w-[480px]"
  >
    <Form />
  </Drawer>
</template>
