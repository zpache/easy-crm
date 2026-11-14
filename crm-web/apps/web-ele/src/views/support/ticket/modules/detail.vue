<script lang="ts" setup>
import type { TicketApi } from '#/api/crm/support';

import { computed, ref } from 'vue';

import { useVbenDrawer } from '@vben/common-ui';

import {
  ElButton,
  ElDescriptions,
  ElDescriptionsItem,
  ElInput,
  ElMessage,
  ElRate,
  ElTag,
  ElTimeline,
  ElTimelineItem,
} from 'element-plus';

import {
  createSatisfaction,
  getTicketDetail,
  getTicketLogs,
  statusLabel,
  SUPPORT_PERMISSIONS,
} from '#/api/crm/support';
import { usePermission } from '#/composables/use-permission';

const emit = defineEmits<{ success: [] }>();

const { can } = usePermission();
const canRate = computed(() => can(SUPPORT_PERMISSIONS.satisfactionCreate));

const ticket = ref<null | TicketApi.Ticket>(null);
const logs = ref<TicketApi.TicketLog[]>([]);
const loading = ref(false);

const score = ref(5);
const rateContent = ref('');
const rateSubmitting = ref(false);

const statusTypeMap: Record<string, string> = {
  ASSIGNED: 'primary',
  CLOSED: 'info',
  COMPLETED: 'success',
  PENDING: 'warning',
  PROCESSING: 'primary',
  WAITING_CONFIRM: 'warning',
};

const isCompleted = computed(
  () => ticket.value?.status === 'COMPLETED',
);

const [Drawer, drawerApi] = useVbenDrawer({
  footer: false,
  async onOpenChange(isOpen) {
    if (!isOpen) {
      return;
    }
    const data = drawerApi.getData<TicketApi.Ticket>();
    if (!data?.id) {
      ticket.value = null;
      logs.value = [];
      return;
    }
    loading.value = true;
    score.value = 5;
    rateContent.value = '';
    try {
      const [detail, logList] = await Promise.all([
        getTicketDetail(data.id),
        getTicketLogs(data.id),
      ]);
      ticket.value = detail;
      logs.value = logList;
    } finally {
      loading.value = false;
    }
  },
});

async function submitRate() {
  if (!ticket.value) {
    return;
  }
  rateSubmitting.value = true;
  try {
    await createSatisfaction({
      content: rateContent.value || undefined,
      score: score.value,
      ticketId: ticket.value.id,
    });
    ElMessage.success('评价已提交，感谢反馈');
    emit('success');
    drawerApi.close();
  } catch (error: any) {
    ElMessage.error(error?.message || '评价提交失败');
  } finally {
    rateSubmitting.value = false;
  }
}
</script>

<template>
  <Drawer
    :title="`工单详情 · ${ticket?.ticketNo ?? ''}`"
    class="w-[760px]"
  >
    <div v-loading="loading" class="space-y-5">
      <template v-if="ticket">
        <ElDescriptions :column="2" border size="small" title="基本信息">
          <ElDescriptionsItem label="工单编号">
            {{ ticket.ticketNo }}
          </ElDescriptionsItem>
          <ElDescriptionsItem label="状态">
            <ElTag
              :type="(statusTypeMap[ticket.status] ?? 'info') as any"
              effect="plain"
            >
              {{ statusLabel(ticket.status) }}
            </ElTag>
            <ElTag v-if="ticket.overdueFlag === 1" class="ml-1" type="danger">
              SLA 超时
            </ElTag>
          </ElDescriptionsItem>
          <ElDescriptionsItem :span="2" label="标题">
            {{ ticket.title }}
          </ElDescriptionsItem>
          <ElDescriptionsItem label="工单类型">
            {{ ticket.ticketTypeName || '-' }}
          </ElDescriptionsItem>
          <ElDescriptionsItem label="优先级">
            {{ ticket.priorityName || '-' }}
          </ElDescriptionsItem>
          <ElDescriptionsItem label="客户">
            {{ ticket.customerName || '-' }}
          </ElDescriptionsItem>
          <ElDescriptionsItem label="联系人">
            {{ ticket.contactName || '-' }}
          </ElDescriptionsItem>
          <ElDescriptionsItem label="关联订单 ID">
            {{ ticket.orderId ?? '-' }}
          </ElDescriptionsItem>
          <ElDescriptionsItem label="受理人">
            {{ ticket.ownerName || '-' }}
          </ElDescriptionsItem>
          <ElDescriptionsItem label="来源">
            {{ ticket.sourceName || '-' }}
          </ElDescriptionsItem>
          <ElDescriptionsItem label="创建时间">
            {{ ticket.createTime || '-' }}
          </ElDescriptionsItem>
        </ElDescriptions>

        <ElDescriptions :column="2" border size="small" title="SLA 时效">
          <ElDescriptionsItem label="应响应时间">
            {{ ticket.slaDueResponseTime || '-' }}
          </ElDescriptionsItem>
          <ElDescriptionsItem label="实际响应时间">
            {{ ticket.actualResponseTime || '-' }}
          </ElDescriptionsItem>
          <ElDescriptionsItem label="应解决时间">
            {{ ticket.slaDueResolveTime || '-' }}
          </ElDescriptionsItem>
          <ElDescriptionsItem label="实际解决时间">
            {{ ticket.actualResolveTime || '-' }}
          </ElDescriptionsItem>
          <ElDescriptionsItem :span="2" label="问题描述">
            {{ ticket.description || '-' }}
          </ElDescriptionsItem>
        </ElDescriptions>

        <div>
          <div class="mb-2 text-sm font-medium">流转日志</div>
          <ElTimeline v-if="logs.length" class="pl-2">
            <ElTimelineItem
              v-for="(log, index) in logs"
              :key="log.id"
              :hollow="index !== 0"
              :timestamp="log.operateTime"
              :type="index === 0 ? 'primary' : undefined"
              placement="top"
            >
              <div>
                {{ log.fromStatusName || '创建' }} → {{ log.toStatusName }}
                <span class="ml-2 text-xs text-gray-400">
                  {{ log.operatorName || '系统' }}
                </span>
              </div>
              <div v-if="log.remark" class="text-xs text-gray-500">
                {{ log.remark }}
              </div>
            </ElTimelineItem>
          </ElTimeline>
          <div v-else class="text-sm text-gray-400">暂无流转日志</div>
        </div>

        <div v-if="isCompleted" class="rounded border p-3">
          <div class="mb-2 text-sm font-medium">满意度评价</div>
          <template v-if="canRate">
            <div class="mb-2 flex items-center gap-3">
              <span class="text-sm">评分</span>
              <ElRate v-model="score" :max="5" />
            </div>
            <ElInput
              v-model="rateContent"
              :maxlength="1000"
              :rows="2"
              class="mb-2"
              placeholder="请输入评价内容（选填）"
              show-word-limit
              type="textarea"
            />
            <ElButton
              :loading="rateSubmitting"
              type="primary"
              @click="submitRate"
            >
              提交评价
            </ElButton>
          </template>
          <div v-else class="text-sm text-gray-400">暂无评价权限</div>
        </div>
      </template>
    </div>
  </Drawer>
</template>
