<script lang="ts" setup>
import { computed, ref } from 'vue';

import { useVbenDrawer } from '@vben/common-ui';

import { ElButton, ElMessage } from 'element-plus';

import { mergeCustomer } from '#/api/crm/customer';

const emit = defineEmits<{ success: [] }>();

const source = ref<{ id: number; name?: string }>({ id: 0 });
const targetId = ref<null | number>(null);
/** 候选目标客户（排除被合并客户自身与已合并客户，由后端列表默认排除已合并） */
const targetOptions = ref<Array<{ id: number; name: string }>>([]);
const loading = ref(false);

const getTitle = computed(() =>
  source.value.name ? `合并客户「${source.value.name}」` : '合并客户',
);

const [Drawer, drawerApi] = useVbenDrawer({
  async onConfirm() {
    if (!targetId.value) {
      ElMessage.warning('请选择合并目标客户');
      return;
    }
    loading.value = true;
    try {
      await mergeCustomer(source.value.id, targetId.value);
      ElMessage.success('合并成功：联系人、线索关联已迁移');
      emit('success');
      drawerApi.close();
    } finally {
      loading.value = false;
    }
  },
  async onOpenChange(isOpen) {
    if (!isOpen) {
      return;
    }
    const data = drawerApi.getData<{ sourceId: number; sourceName?: string }>();
    source.value = { id: data.sourceId, name: data.sourceName };
    targetId.value = null;
    // 加载候选目标客户（前 100 个正常/公海客户，排除自身）
    const { getCustomerList } = await import('#/api/crm/customer');
    const result = await getCustomerList({ pageSize: 100 });
    targetOptions.value = result.items
      .filter((item: any) => item.id !== source.value.id)
      .map((item: any) => ({ id: item.id, name: item.name }));
  },
});
</script>

<template>
  <Drawer :title="getTitle" class="w-[480px]">
    <div class="space-y-3">
      <div class="text-muted-foreground text-xs">
        被合并客户（{{ source.name }}）的联系人、线索转化关联将迁移至目标客户，
        商机 / 跟进记录迁移将在后续轮次支持；合并后原客户标记为「已合并」且不再出现在默认列表。
      </div>
      <div class="text-sm font-medium">合并目标客户</div>
      <ElButton
        v-for="option in targetOptions"
        :key="option.id"
        :type="targetId === option.id ? 'primary' : 'default'"
        class="!ml-0 mr-2"
        size="small"
        @click="targetId = option.id"
      >
        {{ option.name }}（ID: {{ option.id }}）
      </ElButton>
      <div v-if="targetOptions.length === 0" class="text-muted-foreground text-xs">
        没有可用的候选客户。
      </div>
    </div>
  </Drawer>
</template>
