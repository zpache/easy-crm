<script lang="ts" setup>
import { ref } from 'vue';

import { useVbenDrawer } from '@vben/common-ui';

import { ElAlert, ElButton, ElMessage, ElUpload } from 'element-plus';

import { importLeads } from '#/api/crm/lead';
import type { LeadApi } from '#/api/crm/lead';

const emit = defineEmits<{ success: [] }>();

const uploading = ref(false);
const result = ref<null | LeadApi.ImportResult>(null);
const selectedFile = ref<null | File>(null);

const [Drawer, drawerApi] = useVbenDrawer({
  onOpenChange(isOpen) {
    if (isOpen) {
      result.value = null;
      selectedFile.value = null;
    }
  },
});

async function onConfirm() {
  if (!selectedFile.value) {
    ElMessage.warning('请先选择 .xlsx 文件');
    return;
  }
  uploading.value = true;
  try {
    result.value = await importLeads(selectedFile.value);
    emit('success');
  } finally {
    uploading.value = false;
  }
}

function onFileChange(file: { raw?: File }) {
  selectedFile.value = file.raw ?? null;
  result.value = null;
}
</script>

<template>
  <Drawer class="w-[520px]" title="Excel 批量导入线索">
    <div class="space-y-3">
      <div class="text-muted-foreground text-xs">
        模板列：线索名称（必填）| 联系人 | 手机号 | 邮箱 | 来源 | 备注；
        来源支持：官网 / 电话 / 广告投放 / 客户转介绍 / 展会活动 / 其他。
        逐行校验，单行失败不影响其余行。
      </div>
      <ElUpload
        :auto-upload="false"
        :limit="1"
        accept=".xlsx"
        drag
        :on-change="onFileChange as any"
      >
        <div class="p-4">点击选择 .xlsx 文件</div>
      </ElUpload>
      <template v-if="result">
        <ElAlert
          :closable="false"
          show-icon
          :title="`共 ${result.total} 行：成功 ${result.successCount}，失败 ${result.failCount}`"
          :type="result.failCount === 0 ? 'success' : 'warning'"
        />
        <ElAlert
          v-for="error in result.errors"
          :key="error.row"
          :closable="false"
          class="mt-1"
          show-icon
          type="error"
          :title="`第 ${error.row} 行：${error.message}`"
        />
      </template>
    </div>
    <template #footer>
      <ElButton :disabled="uploading" @click="drawerApi.close()">关闭</ElButton>
      <ElButton
        :disabled="!selectedFile"
        :loading="uploading"
        type="primary"
        @click="onConfirm"
      >
        开始导入
      </ElButton>
    </template>
  </Drawer>
</template>
