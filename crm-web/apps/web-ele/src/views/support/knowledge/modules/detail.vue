<script lang="ts" setup>
import type { KnowledgeApi } from '#/api/crm/support';

import { ref } from 'vue';

import { useVbenDrawer } from '@vben/common-ui';

import { ElDescriptions, ElDescriptionsItem, ElTag } from 'element-plus';

import {
  getKnowledgeDetail,
  knowledgeStatusLabel,
  viewKnowledge,
} from '#/api/crm/support';

const emit = defineEmits<{ success: [] }>();

const knowledge = ref<null | KnowledgeApi.Knowledge>(null);
const loading = ref(false);

const [Drawer, drawerApi] = useVbenDrawer({
  footer: false,
  async onOpenChange(isOpen) {
    if (!isOpen) {
      return;
    }
    const data = drawerApi.getData<KnowledgeApi.Knowledge>();
    if (!data?.id) {
      knowledge.value = null;
      return;
    }
    loading.value = true;
    try {
      knowledge.value = await getKnowledgeDetail(data.id);
      // 浏览量 +1（独立接口，避免 GET 带写副作用）；刷新列表以展示最新浏览量
      await viewKnowledge(data.id);
      emit('success');
    } finally {
      loading.value = false;
    }
  },
});
</script>

<template>
  <Drawer
    :title="`知识详情 · ${knowledge?.title ?? ''}`"
    class="w-[680px]"
  >
    <div v-loading="loading" class="space-y-4">
      <template v-if="knowledge">
        <ElDescriptions :column="2" border size="small">
          <ElDescriptionsItem label="分类">
            {{ knowledge.category }}
          </ElDescriptionsItem>
          <ElDescriptionsItem label="发布状态">
            <ElTag effect="plain" type="success">
              {{ knowledgeStatusLabel(knowledge.publishStatus) }}
            </ElTag>
          </ElDescriptionsItem>
          <ElDescriptionsItem label="标签">
            {{ knowledge.tags || '-' }}
          </ElDescriptionsItem>
          <ElDescriptionsItem label="浏览量">
            {{ knowledge.viewCount }}
          </ElDescriptionsItem>
          <ElDescriptionsItem :span="2" label="内容">
            <div class="whitespace-pre-wrap">
              {{ knowledge.content || '暂无内容' }}
            </div>
          </ElDescriptionsItem>
        </ElDescriptions>
      </template>
    </div>
  </Drawer>
</template>
