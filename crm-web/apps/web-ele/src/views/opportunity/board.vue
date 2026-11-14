<script lang="ts" setup>
import type { OpportunityApi } from '#/api/crm/opportunity';

import { computed, onMounted, ref } from 'vue';

import { Page } from '@vben/common-ui';

import {
  ElButton,
  ElDatePicker,
  ElDialog,
  ElInput,
  ElInputNumber,
  ElMessage,
  ElOption,
  ElSelect,
  ElTag,
} from 'element-plus';

import {
  changeStage,
  getBoard,
  LOST_REASONS,
} from '#/api/crm/opportunity';
import { usePermission } from '#/composables/use-permission';

/** 阶段变更权限码（与后端 @PreAuthorize 保持一致，由统一整合方登记到 permissions.ts） */
const STAGE_PERMISSION = 'crm:opportunity:stage';

const { can } = usePermission();
const canStage = can(STAGE_PERMISSION);

const stages = ref<OpportunityApi.BoardStage[]>([]);
const loading = ref(false);

async function loadBoard() {
  loading.value = true;
  try {
    stages.value = await getBoard();
  } finally {
    loading.value = false;
  }
}

onMounted(loadBoard);

/** 可用目标阶段（仅启用阶段） */
const enabledStages = computed(() =>
  stages.value
    .filter((s) => s.enabled)
    .map((s) => ({
      lost: s.lost,
      stageCode: s.stageCode,
      stageName: s.stageName,
      won: s.won,
    })),
);

function stageAmount(stage: OpportunityApi.BoardStage) {
  const total = stage.opportunities.reduce(
    (sum, o) => sum + (o.amount == null ? 0 : Number(o.amount)),
    0,
  );
  return total.toLocaleString();
}

// ------------------------------------------------------------ 阶段变更弹窗
const dialogOpen = ref(false);
const current = ref<null | OpportunityApi.Opportunity>(null);
const targetStageCode = ref('');
const actualCloseDate = ref('');
const actualAmount = ref<number | undefined>(undefined);
const lostReason = ref('');
const competitor = ref('');

const targetIsWon = computed(
  () =>
    enabledStages.value.find((s) => s.stageCode === targetStageCode.value)
      ?.won === true,
);
const targetIsLost = computed(
  () =>
    enabledStages.value.find((s) => s.stageCode === targetStageCode.value)
      ?.lost === true,
);

/**
 * 打开阶段变更弹窗。无论按钮点击还是拖拽落位，都统一走这里，
 * 最终调用 PUT /opportunities/{id}/stage（changeStage），不绕过后端状态机。
 */
function openStageChange(op: OpportunityApi.Opportunity, preset?: string) {
  if (!canStage) {
    return;
  }
  current.value = op;
  targetStageCode.value = preset ?? '';
  actualCloseDate.value = '';
  actualAmount.value = undefined;
  lostReason.value = '';
  competitor.value = '';
  dialogOpen.value = true;
}

async function confirmStageChange() {
  if (!current.value) {
    return;
  }
  if (!targetStageCode.value) {
    ElMessage.warning('请选择目标阶段');
    return;
  }
  if (targetIsLost.value && !lostReason.value) {
    ElMessage.warning('丢单必须选择丢单原因');
    return;
  }
  const payload: OpportunityApi.StageChangePayload = {
    actualAmount: targetIsWon.value ? (actualAmount.value ?? null) : undefined,
    actualCloseDate: targetIsWon.value
      ? actualCloseDate.value || undefined
      : undefined,
    competitor: targetIsLost.value ? competitor.value || undefined : undefined,
    lostReason: targetIsLost.value ? lostReason.value : undefined,
    targetStageCode: targetStageCode.value,
  };
  await changeStage(current.value.id, payload);
  ElMessage.success('阶段已变更');
  dialogOpen.value = false;
  await loadBoard();
}

// ------------------------------------------------------------ 拖拽
const draggingId = ref<null | number>(null);

function onDragStart(op: OpportunityApi.Opportunity) {
  if (!canStage) {
    return;
  }
  draggingId.value = op.id;
}

function onDrop(stageCode: string) {
  const id = draggingId.value;
  draggingId.value = null;
  if (id == null) {
    return;
  }
  const op = stages.value
    .flatMap((s) => s.opportunities)
    .find((o) => o.id === id);
  if (!op || op.stageCode === stageCode) {
    return;
  }
  openStageChange(op, stageCode);
}

function staleType(level?: number) {
  if (level === 3) return 'danger';
  if (level === 2 || level === 1) return 'warning';
  return 'success';
}

function staleText(level?: number) {
  if (level === 3) return '红（30天）';
  if (level === 2) return '橙（14天）';
  if (level === 1) return '黄（7天）';
  return '正常';
}
</script>

<template>
  <Page auto-content-height>
    <div v-loading="loading" class="flex h-full gap-3 overflow-x-auto pb-2">
      <div
        v-for="stage in stages"
        :key="stage.stageCode"
        class="flex h-full min-w-[280px] flex-1 flex-col rounded-lg border border-gray-200 bg-gray-50 dark:border-gray-700 dark:bg-gray-900"
        @dragover.prevent
        @drop="onDrop(stage.stageCode)"
      >
        <div
          class="flex items-center justify-between rounded-t-lg border-b border-gray-200 bg-white px-3 py-2 dark:border-gray-700 dark:bg-gray-800"
        >
          <div class="flex items-center gap-2">
            <span class="text-sm font-medium">{{ stage.stageName }}</span>
            <ElTag size="small" type="info">{{ stage.opportunities.length }}</ElTag>
            <ElTag v-if="stage.won" size="small" type="success">赢单</ElTag>
            <ElTag v-if="stage.lost" size="small" type="danger">丢单</ElTag>
            <ElTag v-if="!stage.enabled" size="small" type="warning">停用</ElTag>
          </div>
          <span class="text-xs text-gray-400">¥ {{ stageAmount(stage) }}</span>
        </div>

        <div class="flex-1 space-y-2 overflow-y-auto p-2">
          <div
            v-for="op in stage.opportunities"
            :key="op.id"
            :draggable="canStage"
            class="cursor-move rounded-md border border-gray-200 bg-white p-3 shadow-sm transition hover:shadow-md dark:border-gray-700 dark:bg-gray-800"
            @dragstart="onDragStart(op)"
          >
            <div class="mb-1 flex items-start justify-between gap-2">
              <span class="text-sm font-medium">{{ op.name }}</span>
              <ElTag :type="staleType(op.staleLevel)" size="small">
                {{ staleText(op.staleLevel) }}
              </ElTag>
            </div>
            <div class="mb-1 text-xs text-gray-500">
              客户：{{ op.customerName || '-' }}
            </div>
            <div class="mb-2 flex items-center justify-between text-xs text-gray-500">
              <span>¥ {{ op.amount == null ? '-' : Number(op.amount).toLocaleString() }}</span>
              <span>赢单率 {{ op.winRate ?? 0 }}%</span>
            </div>
            <div class="flex items-center justify-between">
              <span class="text-xs text-gray-400">{{ op.ownerName || '公海' }}</span>
              <ElButton
                v-if="canStage"
                link
                size="small"
                type="primary"
                @click="openStageChange(op)"
              >
                变更阶段
              </ElButton>
            </div>
          </div>
          <div
            v-if="stage.opportunities.length === 0"
            class="py-6 text-center text-xs text-gray-400"
          >
            暂无商机
          </div>
        </div>
      </div>
    </div>

    <!-- 阶段变更弹窗：统一调用 PUT /opportunities/{id}/stage，携带赢单/丢单表单字段 -->
    <ElDialog
      v-model="dialogOpen"
      :title="`变更阶段 - ${current?.name ?? ''}`"
      width="460px"
    >
      <div class="space-y-3">
        <div>
          <div class="mb-1 text-xs text-gray-500">目标阶段</div>
          <ElSelect v-model="targetStageCode" class="w-full" placeholder="请选择目标阶段">
            <ElOption
              v-for="s in enabledStages"
              :key="s.stageCode"
              :label="s.stageName"
              :value="s.stageCode"
            />
          </ElSelect>
        </div>
        <template v-if="targetIsWon">
          <div>
            <div class="mb-1 text-xs text-gray-500">实际成交日期（缺省今天）</div>
            <ElDatePicker
              v-model="actualCloseDate"
              class="w-full"
              type="date"
              value-format="YYYY-MM-DD"
            />
          </div>
          <div>
            <div class="mb-1 text-xs text-gray-500">实际成交金额（缺省预计金额）</div>
            <ElInputNumber
              v-model="actualAmount"
              class="w-full"
              :min="0"
              :precision="2"
            />
          </div>
        </template>
        <template v-if="targetIsLost">
          <div>
            <div class="mb-1 text-xs text-gray-500">丢单原因（必填）</div>
            <ElSelect v-model="lostReason" class="w-full" placeholder="请选择丢单原因">
              <ElOption
                v-for="r in LOST_REASONS"
                :key="r.value"
                :label="r.label"
                :value="r.value"
              />
            </ElSelect>
          </div>
          <div>
            <div class="mb-1 text-xs text-gray-500">竞争对手（选填）</div>
            <ElInput v-model="competitor" placeholder="请输入竞争对手" />
          </div>
        </template>
      </div>
      <template #footer>
        <ElButton @click="dialogOpen = false">取消</ElButton>
        <ElButton type="primary" @click="confirmStageChange">确定</ElButton>
      </template>
    </ElDialog>
  </Page>
</template>
