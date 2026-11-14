<script lang="ts" setup>
import type {
  OnActionClickParams,
  VxeTableGridOptions,
} from '#/adapter/vxe-table';
import type { EchartsUIType } from '@vben/plugins/echarts';
import type { ReportApi } from '#/api/crm/report';

import { onMounted, ref } from 'vue';

import { Page, useVbenDrawer } from '@vben/common-ui';
import { Plus } from '@vben/icons';
import { EchartsUI, useEcharts } from '@vben/plugins/echarts';

import {
  ElButton,
  ElCard,
  ElInput,
  ElInputNumber,
  ElMessage,
  ElMessageBox,
} from 'element-plus';

import { useVbenVxeGrid } from '#/adapter/vxe-table';
import {
  deleteSalesTarget,
  getAchievement,
  getSalesTargetList,
} from '#/api/crm/report';
import { SALES_TARGET_PERMISSIONS } from '#/constants/crm-batch3-permissions';

import { useColumns, useGridFormSchema } from './data';
import FormDrawer from './modules/form.vue';

function defaultMonth(offset: number) {
  const d = new Date();
  d.setMonth(d.getMonth() + offset);
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`;
}

const filters = ref<ReportApi.AchievementQuery>({
  deptId: undefined,
  endMonth: defaultMonth(0),
  ownerId: undefined,
  startMonth: defaultMonth(-11),
});

const achievement = ref<null | ReportApi.Achievement>(null);

const chartRef = ref<EchartsUIType>();
const { renderEcharts } = useEcharts(chartRef);

const [FormDrawerComp, formDrawerApi] = useVbenDrawer({
  connectedComponent: FormDrawer,
  destroyOnClose: true,
});

const [Grid, gridApi] = useVbenVxeGrid({
  formOptions: { schema: useGridFormSchema(), submitOnChange: true },
  gridOptions: {
    columns: useColumns(onActionClick),
    height: 'auto',
    keepSource: true,
    proxyConfig: {
      ajax: {
        query: async ({ page }, formValues) => {
          return await getSalesTargetList({
            deptId: formValues?.deptId,
            ownerId: formValues?.ownerId,
            pageNum: page.currentPage,
            pageSize: page.pageSize,
            targetMonth: formValues?.targetMonth,
          });
        },
      },
    },
    rowConfig: { keyField: 'id' },
    toolbarConfig: { custom: true, export: false, refresh: true, search: true, zoom: true },
  } as VxeTableGridOptions<ReportApi.SalesTarget>,
});

function formatMoney(value: number | undefined | null) {
  return value == null ? '-' : Number(value).toLocaleString('zh-CN');
}

function formatRate(value: number | undefined | null) {
  if (value == null) {
    return '-';
  }
  return `${(Number(value) * 100).toFixed(2)}%`;
}

async function loadAchievement() {
  const data = await getAchievement(filters.value);
  achievement.value = data;
  await renderEcharts({
    grid: { bottom: 30, left: 70, right: 30, top: 50 },
    legend: { data: ['目标金额', '订单实绩', '商机赢单实绩'], top: 0 },
    series: [
      {
        data: data.rows.map((r) => Number(r.targetAmount)),
        name: '目标金额',
        type: 'bar',
      },
      {
        data: data.rows.map((r) => Number(r.orderAmount)),
        name: '订单实绩',
        type: 'bar',
      },
      {
        data: data.rows.map((r) => Number(r.opportunityAmount)),
        name: '商机赢单实绩',
        smooth: true,
        type: 'line',
      },
    ],
    tooltip: { trigger: 'axis' },
    xAxis: { data: data.rows.map((r) => r.month), type: 'category' },
    yAxis: { name: '金额(元)', type: 'value' },
  });
}

function onActionClick(e: OnActionClickParams<ReportApi.SalesTarget>) {
  switch (e.code) {
    case 'edit': {
      formDrawerApi.setData(e.row).open();
      break;
    }
    case 'delete': {
      onDelete(e.row);
      break;
    }
  }
}

async function onDelete(row: ReportApi.SalesTarget) {
  await ElMessageBox.confirm(
    `确认删除 ${row.targetMonth} 的销售目标吗？`,
    '删除目标',
    { type: 'warning' },
  );
  await deleteSalesTarget(row.id);
  ElMessage.success('已删除');
  gridApi.query();
  await loadAchievement();
}

function onCreate() {
  formDrawerApi.setData({}).open();
}

onMounted(async () => {
  await loadAchievement();
});
</script>

<template>
  <Page auto-content-height>
    <FormDrawerComp @success="gridApi.query()" />
    <div class="p-4">
      <!-- 达成分析筛选 -->
      <ElCard shadow="never">
        <template #header>
          <span>销售达成分析（按时间 / 部门 / 负责人）</span>
        </template>
        <div class="flex flex-wrap items-center gap-3">
          <div class="flex items-center gap-1">
            <span class="text-sm text-gray-500">起始年月</span>
            <ElInput v-model="filters.startMonth" class="w-32" placeholder="YYYY-MM" />
          </div>
          <div class="flex items-center gap-1">
            <span class="text-sm text-gray-500">结束年月</span>
            <ElInput v-model="filters.endMonth" class="w-32" placeholder="YYYY-MM" />
          </div>
          <div class="flex items-center gap-1">
            <span class="text-sm text-gray-500">部门 ID</span>
            <ElInputNumber
              v-model="filters.deptId"
              :min="0"
              class="w-32"
              controls-position="right"
              placeholder="不限"
            />
          </div>
          <div class="flex items-center gap-1">
            <span class="text-sm text-gray-500">负责人 ID</span>
            <ElInputNumber
              v-model="filters.ownerId"
              :min="0"
              class="w-32"
              controls-position="right"
              placeholder="不限"
            />
          </div>
          <ElButton type="primary" @click="loadAchievement">查询</ElButton>
        </div>
        <div class="mt-4 flex flex-wrap gap-6 text-sm">
          <span>目标合计：<b>{{ formatMoney(achievement?.totalTarget) }}</b></span>
          <span>订单实绩：<b>{{ formatMoney(achievement?.totalOrderAmount) }}</b></span>
          <span>商机赢单实绩：<b>{{ formatMoney(achievement?.totalOpportunityAmount) }}</b></span>
          <span>达成率：<b>{{ formatRate(achievement?.achievementRate) }}</b></span>
        </div>
      </ElCard>

      <ElCard class="mt-4" shadow="never">
        <template #header>
          <span>月度目标 vs 实绩</span>
        </template>
        <EchartsUI ref="chartRef" height="320px" />
      </ElCard>

      <!-- 销售目标表格 -->
      <div class="mt-4">
        <Grid table-title="销售目标">
          <template #toolbar-tools>
            <ElButton
              v-access:code="SALES_TARGET_PERMISSIONS.create"
              type="primary"
              @click="onCreate"
            >
              <Plus class="size-5" />
              新增目标
            </ElButton>
          </template>
        </Grid>
      </div>
    </div>
  </Page>
</template>
