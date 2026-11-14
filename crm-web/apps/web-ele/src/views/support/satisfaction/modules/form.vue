<script lang="ts" setup>
import type { SatisfactionApi } from '#/api/crm/support';
import type { VbenFormSchema } from '#/adapter/form';

import { onMounted, ref } from 'vue';

import { useVbenDrawer } from '@vben/common-ui';

import { useVbenForm, z } from '#/adapter/form';
import {
  createSatisfaction,
  getTicketList,
  SATISFACTION_SCORES,
} from '#/api/crm/support';

const emit = defineEmits<{ success: [] }>();

/** 仅已完成工单可评价，下拉候选按需加载 */
const completedTickets = ref<Array<{ label: string; value: number }>>([]);

function useRateSchema(): VbenFormSchema[] {
  return [
    {
      component: 'Select',
      componentProps: () => ({
        class: 'w-full',
        filterOption: (input: string, option: any) =>
          option.label.includes(input),
        options: completedTickets.value,
        placeholder: '请选择已完成的工单',
        showSearch: true,
      }),
      fieldName: 'ticketId',
      label: '工单',
      rules: z.number({ message: '请选择工单' }),
    },
    {
      component: 'Select',
      componentProps: { class: 'w-full', options: [...SATISFACTION_SCORES] },
      fieldName: 'score',
      label: '评分',
      rules: z.number({ message: '请选择评分' }).min(1).max(5),
    },
    {
      component: 'Textarea',
      componentProps: { maxLength: 1000, rows: 4, showCount: true },
      fieldName: 'content',
      label: '评价内容',
    },
  ];
}

const [Form, formApi] = useVbenForm({
  layout: 'vertical',
  schema: useRateSchema(),
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
    const payload: SatisfactionApi.SatisfactionPayload = {
      content: values.content || undefined,
      score: values.score,
      ticketId: values.ticketId,
    };
    try {
      await createSatisfaction(payload);
      emit('success');
      drawerApi.close();
    } finally {
      drawerApi.lock(false);
    }
  },
  onOpenChange(isOpen) {
    if (isOpen) {
      formApi.resetForm();
    }
  },
});

onMounted(async () => {
  const result = await getTicketList({ pageSize: 100, status: 'COMPLETED' });
  completedTickets.value = result.items.map((item: any) => ({
    label: `${item.ticketNo} · ${item.title}`,
    value: item.id,
  }));
});
</script>

<template>
  <Drawer class="w-[480px]" title="新增满意度评价">
    <div class="mb-3 text-sm text-gray-500">
      仅「已完成」工单可评价，一单一评；工单详情页同样提供评价入口。
    </div>
    <Form />
  </Drawer>
</template>
