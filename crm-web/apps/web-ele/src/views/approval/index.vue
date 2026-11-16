<script lang="ts" setup>
import type { ApprovalApi } from '#/api/crm/approval';

import { computed, onMounted, reactive, ref } from 'vue';

import { Page } from '@vben/common-ui';
import { ExternalLink } from '@vben/icons';

import {
  ElButton,
  ElCard,
  ElDescriptions,
  ElDescriptionsItem,
  ElDrawer,
  ElInput,
  ElMessage,
  ElOption,
  ElPagination,
  ElRadioButton,
  ElRadioGroup,
  ElSelect,
  ElTable,
  ElTableColumn,
  ElTag,
} from 'element-plus';

import {
  APPROVAL_BIZ_TYPES,
  APPROVAL_STATUSES,
  getApprovalList,
  getApprovalSummary,
} from '#/api/crm/approval';

const activeBox = ref<ApprovalApi.Box>('todo');
const loading = ref(false);
const rows = ref<ApprovalApi.Approval[]>([]);
const total = ref(0);
const summary = ref<ApprovalApi.Summary>({ applied: 0, done: 0, todo: 0 });

const query = reactive({
  bizType: undefined as string | undefined,
  keyword: '',
  pageNum: 1,
  pageSize: 10,
  status: undefined as number | undefined,
});

const detailVisible = ref(false);
const detail = ref<ApprovalApi.Approval | null>(null);

const STATUS_TAG: Record<number, string> = {
  0: 'warning',
  1: 'success',
  2: 'danger',
  3: 'info',
};

const boxLabel = computed(() => ({
  applied: `我发起的 (${summary.value.applied})`,
  done: `已办审批 (${summary.value.done})`,
  todo: `待办审批 (${summary.value.todo})`,
}));

async function loadSummary() {
  summary.value = await getApprovalSummary();
}

async function loadList() {
  loading.value = true;
  try {
    const result = await getApprovalList({
      box: activeBox.value,
      bizType: query.bizType,
      keyword: query.keyword || undefined,
      pageNum: query.pageNum,
      pageSize: query.pageSize,
      status: query.status,
    });
    rows.value = result.items;
    total.value = result.total;
  } finally {
    loading.value = false;
  }
}

async function refresh() {
  await Promise.all([loadSummary(), loadList()]);
}

function onBoxChange() {
  query.pageNum = 1;
  loadList();
}

function onSearch() {
  query.pageNum = 1;
  loadList();
}

function onPageChange(page: number) {
  query.pageNum = page;
  loadList();
}

function onSizeChange(size: number) {
  query.pageSize = size;
  query.pageNum = 1;
  loadList();
}

function openExternal(row: ApprovalApi.Approval) {
  if (!row.externalUrl) {
    ElMessage.warning('该审批未提供外部审批链接，请到来源系统查看');
    return;
  }
  window.open(row.externalUrl, '_blank', 'noopener');
}

function openDetail(row: ApprovalApi.Approval) {
  detail.value = row;
  detailVisible.value = true;
}

function openDetailExternal() {
  if (detail.value) {
    openExternal(detail.value);
  }
}

function statusText(status: number) {
  return APPROVAL_STATUSES.find((s) => s.value === status)?.label ?? '-';
}

onMounted(async () => {
  await refresh();
});
</script>

<template>
  <Page auto-content-height>
    <div class="p-4">
      <ElCard shadow="never">
        <template #header>
          <div class="flex flex-wrap items-center justify-between gap-2">
            <span>审批中心（对接外部审批系统，仅提供入口与跳转）</span>
            <div class="flex items-center gap-2">
              <ElInput
                v-model="query.keyword"
                clearable
                class="w-52"
                placeholder="标题 / 单号"
                @keyup.enter="onSearch"
              />
              <ElSelect
                v-model="query.bizType"
                clearable
                class="w-32"
                placeholder="业务类型"
              >
                <ElOption
                  v-for="t in APPROVAL_BIZ_TYPES"
                  :key="t.value"
                  :label="t.label"
                  :value="t.value"
                />
              </ElSelect>
              <ElSelect
                v-model="query.status"
                clearable
                class="w-32"
                placeholder="状态"
              >
                <ElOption
                  v-for="s in APPROVAL_STATUSES"
                  :key="s.value"
                  :label="s.label"
                  :value="s.value"
                />
              </ElSelect>
              <ElButton type="primary" @click="onSearch">查询</ElButton>
            </div>
          </div>
        </template>

        <ElRadioGroup v-model="activeBox" class="mb-3" @change="onBoxChange">
          <ElRadioButton :label="boxLabel.todo" value="todo" />
          <ElRadioButton :label="boxLabel.done" value="done" />
          <ElRadioButton :label="boxLabel.applied" value="applied" />
        </ElRadioGroup>

        <ElTable v-loading="loading" :data="rows" border size="small">
          <ElTableColumn label="标题" min-width="220">
            <template #default="{ row }">
              <ElButton link type="primary" @click="openDetail(row)">
                {{ row.title }}
              </ElButton>
            </template>
          </ElTableColumn>
          <ElTableColumn prop="bizTypeName" label="业务类型" width="110" />
          <ElTableColumn prop="bizNo" label="业务单号" min-width="150" />
          <ElTableColumn prop="nodeName" label="当前节点" width="130" />
          <ElTableColumn label="状态" width="100">
            <template #default="{ row }">
              <ElTag :type="STATUS_TAG[row.status] ?? 'info'" size="small">
                {{ row.statusName ?? statusText(row.status) }}
              </ElTag>
            </template>
          </ElTableColumn>
          <ElTableColumn prop="applyUserName" label="发起人" width="110" />
          <ElTableColumn prop="applyTime" label="发起时间" width="165" />
          <ElTableColumn prop="finishTime" label="结束时间" width="165" />
          <ElTableColumn prop="sourceSystem" label="来源系统" width="140" />
          <ElTableColumn align="center" label="操作" width="160">
            <template #default="{ row }">
              <ElButton link type="primary" @click="openDetail(row)">
                详情
              </ElButton>
              <ElButton
                v-if="row.externalUrl"
                link
                type="primary"
                @click="openExternal(row)"
              >
                <ExternalLink class="size-4" />
                去审批
              </ElButton>
            </template>
          </ElTableColumn>
        </ElTable>

        <div class="mt-3 flex justify-end">
          <ElPagination
            :current-page="query.pageNum"
            :page-size="query.pageSize"
            :page-sizes="[10, 20, 50]"
            :total="total"
            layout="total, sizes, prev, pager, next"
            @current-change="onPageChange"
            @size-change="onSizeChange"
          />
        </div>
      </ElCard>
    </div>

    <ElDrawer v-model="detailVisible" size="520px" title="审批详情（只读镜像）">
      <ElDescriptions v-if="detail" :column="1" border size="small">
        <ElDescriptionsItem label="标题">{{ detail.title }}</ElDescriptionsItem>
        <ElDescriptionsItem label="业务类型">
          {{ detail.bizTypeName ?? detail.bizType }}
        </ElDescriptionsItem>
        <ElDescriptionsItem label="业务单号">
          {{ detail.bizNo ?? '-' }}
        </ElDescriptionsItem>
        <ElDescriptionsItem label="当前节点">
          {{ detail.nodeName ?? '-' }}
        </ElDescriptionsItem>
        <ElDescriptionsItem label="状态">
          {{ detail.statusName ?? statusText(detail.status) }}
        </ElDescriptionsItem>
        <ElDescriptionsItem label="发起人">
          {{ detail.applyUserName ?? '-' }}
        </ElDescriptionsItem>
        <ElDescriptionsItem label="当前审批人">
          {{ detail.currentApproverName ?? '-' }}
        </ElDescriptionsItem>
        <ElDescriptionsItem label="发起时间">
          {{ detail.applyTime ?? '-' }}
        </ElDescriptionsItem>
        <ElDescriptionsItem label="结束时间">
          {{ detail.finishTime ?? '-' }}
        </ElDescriptionsItem>
        <ElDescriptionsItem label="来源系统">
          {{ detail.sourceSystem ?? '-' }}
        </ElDescriptionsItem>
        <ElDescriptionsItem label="备注">
          {{ detail.remark ?? '-' }}
        </ElDescriptionsItem>
      </ElDescriptions>
      <div class="mt-4 text-sm text-gray-500">
        审批的通过与驳回在外部审批系统内完成，此处仅同步展示状态。
      </div>
      <div class="mt-3">
        <ElButton
          v-if="detail?.externalUrl"
          type="primary"
          @click="openDetailExternal"
        >
          打开外部审批
        </ElButton>
      </div>
    </ElDrawer>
  </Page>
</template>
