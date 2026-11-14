<script lang="ts" setup>
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { SatisfactionApi } from '#/api/crm/support';

import { onMounted, ref } from 'vue';

import { Page, useVbenDrawer } from '@vben/common-ui';
import { Plus } from '@vben/icons';

import {
  ElButton,
  ElDatePicker,
  ElTable,
  ElTableColumn,
  ElTag,
} from 'element-plus';

import { useVbenVxeGrid } from '#/adapter/vxe-table';
import {
  getSatisfactionList,
  getSatisfactionStats,
  SUPPORT_PERMISSIONS,
} from '#/api/crm/support';

import { useColumns, useGridFormSchema } from './data';
import FormDrawer from './modules/form.vue';

const [FormDrawerComp, formDrawerApi] = useVbenDrawer({
  connectedComponent: FormDrawer,
  destroyOnClose: true,
});

const stats = ref<SatisfactionApi.Stats>({
  averageScore: 0,
  distribution: [],
  total: 0,
});
const statsLoading = ref(false);
/** 统计时间范围（与列表筛选相互独立） */
const statsRange = ref<[string, string] | null>(null);

const [Grid, gridApi] = useVbenVxeGrid({
  formOptions: {
    schema: useGridFormSchema(),
    submitOnChange: true,
  },
  gridOptions: {
    columns: useColumns(),
    height: 'auto',
    keepSource: true,
    proxyConfig: {
      ajax: {
        query: async ({ page }, formValues) => {
          const range = formValues?.evaluateTimeRange as
            | [string, string]
            | undefined;
          return await getSatisfactionList({
            endDate: range?.[1],
            pageNum: page.currentPage,
            pageSize: page.pageSize,
            score: formValues?.score,
            startDate: range?.[0],
            ticketId: formValues?.ticketId,
          });
        },
      },
    },
    rowConfig: { keyField: 'id' },
    toolbarConfig: {
      custom: true,
      export: false,
      refresh: true,
      search: true,
      zoom: true,
    },
  } as VxeTableGridOptions<SatisfactionApi.Satisfaction>,
});

async function loadStats() {
  statsLoading.value = true;
  try {
    stats.value = await getSatisfactionStats({
      endDate: statsRange.value?.[1],
      startDate: statsRange.value?.[0],
    });
  } finally {
    statsLoading.value = false;
  }
}

onMounted(loadStats);

function onCreate() {
  formDrawerApi.setData({}).open();
}

function onRateSuccess() {
  gridApi.query();
  loadStats();
}
</script>

<template>
  <Page auto-content-height>
    <FormDrawerComp @success="onRateSuccess" />
    <div class="mb-3 flex flex-wrap items-center gap-3">
      <div class="flex items-center gap-2">
        <span class="text-sm text-gray-500">评价总数</span>
        <span class="text-xl font-semibold">{{ stats.total }}</span>
      </div>
      <div class="flex items-center gap-2">
        <span class="text-sm text-gray-500">平均分</span>
        <ElTag effect="dark" size="large" type="primary">
          {{ stats.averageScore }}
        </ElTag>
      </div>
      <div class="ml-auto flex items-center gap-2">
        <ElDatePicker
          v-model="statsRange"
          end-placeholder="结束时间"
          start-placeholder="开始时间"
          type="datetimerange"
          value-format="YYYY-MM-DD HH:mm:ss"
        />
        <ElButton :loading="statsLoading" @click="loadStats">刷新统计</ElButton>
      </div>
    </div>

    <div class="mb-3 w-full max-w-[520px]">
      <ElTable :data="stats.distribution" border size="small">
        <ElTableColumn label="评分" prop="score" width="90">
          <template #default="{ row }">{{ row.score }} 分</template>
        </ElTableColumn>
        <ElTableColumn label="评价数" prop="cnt" width="100" />
        <ElTableColumn label="占比">
          <template #default="{ row }">
            <div class="flex items-center gap-2">
              <div class="h-2 flex-1 overflow-hidden rounded bg-gray-100">
                <div
                  class="h-full bg-blue-500"
                  :style="{
                    width: `${stats.total ? (row.cnt / stats.total) * 100 : 0}%`,
                  }"
                />
              </div>
              <span class="w-12 text-right text-xs text-gray-500">
                {{ stats.total ? ((row.cnt / stats.total) * 100).toFixed(1) : '0.0' }}%
              </span>
            </div>
          </template>
        </ElTableColumn>
      </ElTable>
    </div>

    <Grid table-title="满意度评价明细">
      <template #toolbar-tools>
        <ElButton
          v-access:code="SUPPORT_PERMISSIONS.satisfactionCreate"
          type="primary"
          @click="onCreate"
        >
          <Plus class="size-5" />
          新增评价
        </ElButton>
      </template>
    </Grid>
  </Page>
</template>
