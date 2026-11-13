<script lang="ts" setup>
import type { OpportunityApi } from '#/api/crm/opportunity';

import { onMounted, ref } from 'vue';

import { useVbenDrawer } from '@vben/common-ui';

import { ElButton, ElMessage, ElTable, ElTableColumn } from 'element-plus';

import { getStages } from '#/api/crm/opportunity';

const emit = defineEmits<{ success: [] }>();

const stages = ref<Array<OpportunityApi.Stage>>([]);
const editing = ref<null | OpportunityApi.Stage>(null);
const saving = ref(false);

const [Drawer, drawerApi] = useVbenDrawer({
  async onOpenChange(isOpen) {
    if (isOpen) {
      stages.value = await getStages();
      editing.value = null;
    }
  },
});

function onEdit(row: OpportunityApi.Stage) {
  editing.value = { ...row };
}

function onCreate() {
  editing.value = {
    defaultWinRate: 0,
    enabled: 1,
    id: 0,
    isLost: 0,
    isWon: 0,
    sortNo: 0,
    stageCode: '',
    stageName: '',
  };
}

async function onSave() {
  if (!editing.value) {
    return;
  }
  const form = editing.value;
  if (!form.stageCode || !form.stageName) {
    ElMessage.warning('阶段编码与名称必填');
    return;
  }
  saving.value = true;
  try {
    const payload: OpportunityApi.StagePayload = {
      defaultWinRate: form.defaultWinRate,
      enabled: form.enabled ? 1 : 0,
      isLost: form.isLost ? 1 : 0,
      isWon: form.isWon ? 1 : 0,
      sortNo: form.sortNo,
      stageCode: form.stageCode,
      stageName: form.stageName,
    };
    const { updateStage, createStage } = await import('#/api/crm/opportunity');
    if (form.id) {
      await updateStage(form.id, payload);
    } else {
      await createStage(payload);
    }
    ElMessage.success('已保存');
    stages.value = await getStages();
    editing.value = null;
    emit('success');
  } finally {
    saving.value = false;
  }
}
</script>

<template>
  <Drawer class="w-[720px]" title="商机阶段配置">
    <div class="mb-2 flex justify-end">
      <ElButton size="small" type="primary" @click="onCreate">新增阶段</ElButton>
    </div>
    <ElTable :data="stages" size="small" border>
      <ElTableColumn label="编码" prop="stageCode" width="130" />
      <ElTableColumn label="名称" min-width="110">
        <template #default="{ row }">
          <ElButton
            v-if="editing && editing.id === row.id"
            class="!px-1"
            link
            @click.stop
          />
          <span v-if="!(editing && editing.id === row.id)">{{ row.stageName }}</span>
          <input
            v-else
            v-model="editing.stageName"
            class="w-full rounded border px-1"
          />
        </template>
      </ElTableColumn>
      <ElTableColumn label="排序" width="80">
        <template #default="{ row }">
          <span v-if="!(editing && editing.id === row.id)">{{ row.sortNo }}</span>
          <input
            v-else
            v-model.number="editing.sortNo"
            class="w-12 rounded border px-1"
            type="number"
          />
        </template>
      </ElTableColumn>
      <ElTableColumn label="赢单率%" width="90">
        <template #default="{ row }">
          <span v-if="!(editing && editing.id === row.id)">{{ row.defaultWinRate }}</span>
          <input
            v-else
            v-model.number="editing.defaultWinRate"
            class="w-14 rounded border px-1"
            type="number"
          />
        </template>
      </ElTableColumn>
      <ElTableColumn label="终态" width="110">
        <template #default="{ row }">
          <span v-if="row.isWon" class="text-success">赢单</span>
          <span v-else-if="row.isLost" class="text-danger">丢单</span>
          <span v-else>进行中</span>
        </template>
      </ElTableColumn>
      <ElTableColumn label="启用" width="80">
        <template #default="{ row }">{{ row.enabled ? '是' : '否' }}</template>
      </ElTableColumn>
      <ElTableColumn label="操作" width="130" align="center">
        <template #default="{ row }">
          <template v-if="editing && editing.id === row.id">
            <ElButton :loading="saving" size="small" type="primary" @click="onSave">
              保存
            </ElButton>
            <ElButton size="small" @click="editing = null">取消</ElButton>
          </template>
          <ElButton v-else size="small" link type="primary" @click="onEdit(row)">
            编辑
          </ElButton>
        </template>
      </ElTableColumn>
    </ElTable>
    <template v-if="editing && editing.id === 0">
      <div class="mt-3 grid grid-cols-2 gap-2">
        <input v-model="editing.stageCode" class="rounded border px-2 py-1" placeholder="编码（大写字母开头）" />
        <input v-model="editing.stageName" class="rounded border px-2 py-1" placeholder="名称" />
        <input v-model.number="editing.sortNo" class="rounded border px-2 py-1" placeholder="排序" type="number" />
        <input v-model.number="editing.defaultWinRate" class="rounded border px-2 py-1" placeholder="赢单率%" type="number" />
        <label class="flex items-center gap-1 text-xs">
          <input v-model="editing.isWon" type="checkbox" /> 赢单终态
        </label>
        <label class="flex items-center gap-1 text-xs">
          <input v-model="editing.isLost" type="checkbox" /> 丢单终态
        </label>
      </div>
      <div class="mt-2 flex justify-end">
        <ElButton :loading="saving" size="small" type="primary" @click="onSave">
          保存新阶段
        </ElButton>
      </div>
    </template>
  </Drawer>
</template>
