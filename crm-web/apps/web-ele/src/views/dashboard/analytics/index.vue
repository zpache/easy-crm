<script lang="ts" setup>
import type { AnalysisOverviewItem } from '@vben/common-ui';
import type { EchartsUIType } from '@vben/plugins/echarts';
import type { DashboardApi } from '#/api/dashboard';

import { onMounted, ref } from 'vue';

import { AnalysisOverview } from '@vben/common-ui';
import { EchartsUI, useEcharts } from '@vben/plugins/echarts';
import { SvgBellIcon, SvgCakeIcon, SvgCardIcon, SvgDownloadIcon } from '@vben/icons';

import { ElAlert, ElCard, ElRadioButton, ElRadioGroup } from 'element-plus';

import { getDashboardOverview } from '#/api/dashboard';

const overview = ref<null | DashboardApi.Overview>(null);

/** 近 N 天趋势，7/30/90 可切换 */
const trendDays = ref(30);

const funnelChartRef = ref<EchartsUIType>();
const { renderEcharts: renderFunnel } = useEcharts(funnelChartRef);

const stageChartRef = ref<EchartsUIType>();
const { renderEcharts: renderStage } = useEcharts(stageChartRef);

const followChartRef = ref<EchartsUIType>();
const { renderEcharts: renderFollow } = useEcharts(followChartRef);

const newTrendChartRef = ref<EchartsUIType>();
const { renderEcharts: renderNewTrend } = useEcharts(newTrendChartRef);

const rankChartRef = ref<EchartsUIType>();
const { renderEcharts: renderRank } = useEcharts(rankChartRef);

/** 指标卡 */
const overviewItems = ref<AnalysisOverviewItem[]>([]);

/** 停滞提醒文案 */
const staleText = ref('');

function formatMoney(value: number | undefined) {
  return value == null ? '0' : Number(value).toLocaleString('zh-CN');
}

async function loadOverviewCards() {
  const data = overview.value ?? (await getDashboardOverview(trendDays.value));
  overview.value = data;
  overviewItems.value = [
    {
      icon: SvgCardIcon,
      title: '线索总数',
      totalTitle: '近 7 天新增',
      totalValue: data.cards.leadsNew7d,
      value: data.cards.leadsTotal,
    },
    {
      icon: SvgCakeIcon,
      title: '客户总数',
      totalTitle: '近 7 天新增',
      totalValue: data.cards.customersNew7d,
      value: data.cards.customersTotal,
    },
    {
      icon: SvgDownloadIcon,
      title: '进行中商机',
      totalTitle: '预计金额合计',
      totalValue: data.cards.oppOpenCnt,
      value: data.cards.oppOpenAmount,
    },
    {
      icon: SvgBellIcon,
      title: '我的待办',
      totalTitle: '已过期',
      totalValue: data.cards.myTasksOverdue,
      value: data.cards.myTasksPending,
    },
  ];
}

async function loadCharts() {
  const data = overview.value ?? (await getDashboardOverview(trendDays.value));
  overview.value = data;

  // 停滞提醒
  const stale = data.staleReminders ?? {};
  const yellow = stale['1'] ?? 0;
  const orange = stale['2'] ?? 0;
  const red = stale['3'] ?? 0;
  staleText.value =
    yellow + orange + red === 0
      ? '暂无停滞商机'
      : `停滞商机提醒：红色 ${red} 个、橙色 ${orange} 个、黄色 ${yellow} 个（7/14/30 天无活动）`;

  // 销售转化漏斗
  await renderFunnel({
    series: [
      {
        data: data.funnel.map((item) => ({ name: item.stage, value: item.value })),
        label: { show: true, position: 'inside' },
        type: 'funnel',
      },
    ],
    tooltip: { trigger: 'item', formatter: '{b}: {c}' },
  });

  // 商机阶段分布（数量 + 加权预测）
  await renderStage({
    grid: { bottom: 30, left: 70, right: 70, top: 40 },
    legend: { data: ['商机数', '加权预测金额'], top: 0 },
    series: [
      {
        data: data.stageDistribution.map((item) => item.count),
        name: '商机数',
        type: 'bar',
        yAxisIndex: 0,
      },
      {
        data: data.stageDistribution.map((item) => Number(item.forecastAmount)),
        name: '加权预测金额',
        smooth: true,
        type: 'line',
        yAxisIndex: 1,
      },
    ],
    tooltip: { trigger: 'axis' },
    xAxis: {
      data: data.stageDistribution.map((item) => item.stageName),
      type: 'category',
    },
    yAxis: [
      { name: '商机数', type: 'value' },
      { name: '金额(元)', type: 'value' },
    ],
  });

  // 跟进活动趋势
  await renderFollow({
    areaStyle: { opacity: 0.15 },
    grid: { bottom: 30, left: 60, right: 30, top: 40 },
    series: [
      {
        data: data.followTrend.map((item) => item.count),
        name: '跟进次数',
        smooth: true,
        type: 'line',
      },
    ],
    tooltip: { trigger: 'axis' },
    xAxis: { data: data.followTrend.map((item) => item.date), type: 'category' },
    yAxis: { type: 'value' },
  });

  // 新增线索 / 客户趋势
  await renderNewTrend({
    grid: { bottom: 30, left: 60, right: 30, top: 40 },
    legend: { data: ['新增线索', '新增客户'], top: 0 },
    series: [
      {
        data: data.newTrend.map((item) => item.leads),
        name: '新增线索',
        type: 'bar',
      },
      {
        data: data.newTrend.map((item) => item.customers),
        name: '新增客户',
        type: 'bar',
      },
    ],
    tooltip: { trigger: 'axis' },
    xAxis: { data: data.newTrend.map((item) => item.date), type: 'category' },
    yAxis: { type: 'value', minInterval: 1 },
  });

  // 负责人商机金额 TOP5
  const rank = [...(data.ownerRanking ?? [])].reverse();
  await renderRank({
    grid: { bottom: 10, left: 8, right: 40, top: 10, containLabel: true },
    series: [
      {
        data: rank.map((item) => Number(item.amount)),
        name: '进行中商机金额',
        type: 'bar',
      },
    ],
    tooltip: {
      formatter: (params: any) => {
        const item = rank[params?.[0]?.dataIndex];
        return item
          ? `${item.ownerName}<br/>进行中商机 ${item.count} 个 · 金额 ¥${formatMoney(item.amount)}`
          : '';
      },
      trigger: 'axis',
    },
    xAxis: { type: 'value' },
    yAxis: {
      data: rank.map((item) => item.ownerName),
      type: 'category',
    },
  });
}

async function onDaysChange() {
  overview.value = null;
  await loadOverviewCards();
  await loadCharts();
}

onMounted(async () => {
  await loadOverviewCards();
  await loadCharts();
});
</script>

<template>
  <div class="p-4">
    <AnalysisOverview class="overview-compact" :items="overviewItems" />

    <ElAlert
      v-if="staleText"
      :closable="false"
      class="mt-4"
      show-icon
      :title="staleText"
      type="warning"
    />

    <div class="mb-3 mt-4 flex items-center gap-2">
      <span class="text-sm text-gray-500">统计范围</span>
      <ElRadioGroup v-model="trendDays" size="small" @change="onDaysChange">
        <ElRadioButton :value="7">近 7 天</ElRadioButton>
        <ElRadioButton :value="30">近 30 天</ElRadioButton>
        <ElRadioButton :value="90">近 90 天</ElRadioButton>
      </ElRadioGroup>
    </div>

    <div class="grid grid-cols-1 gap-4 xl:grid-cols-2">
      <ElCard shadow="never">
        <template #header>
          <div class="flex items-center justify-between">
            <span>销售转化漏斗</span>
            <span class="text-xs text-gray-400">线索 → 商机各阶段数量</span>
          </div>
        </template>
        <EchartsUI ref="funnelChartRef" height="320px" />
      </ElCard>
      <ElCard shadow="never">
        <template #header>
          <div class="flex items-center justify-between">
            <span>商机阶段分布</span>
            <span class="text-xs text-gray-400">进行中商机 · 加权预测 = 赢单率 × 金额</span>
          </div>
        </template>
        <EchartsUI ref="stageChartRef" height="320px" />
      </ElCard>
      <ElCard shadow="never">
        <template #header>
          <div class="flex items-center justify-between">
            <span>跟进活动趋势</span>
            <span class="text-xs text-gray-400">每日跟进记录次数</span>
          </div>
        </template>
        <EchartsUI ref="followChartRef" height="300px" />
      </ElCard>
      <ElCard shadow="never">
        <template #header>
          <div class="flex items-center justify-between">
            <span>新增线索 / 客户趋势</span>
            <span class="text-xs text-gray-400">按创建时间，每日新增</span>
          </div>
        </template>
        <EchartsUI ref="newTrendChartRef" height="300px" />
      </ElCard>
    </div>

    <ElCard class="mt-4" shadow="never">
      <template #header>
        <div class="flex items-center justify-between">
          <span>负责人商机金额 TOP5</span>
          <span class="text-xs text-gray-400">进行中商机，按预计金额降序</span>
        </div>
      </template>
      <EchartsUI ref="rankChartRef" height="260px" />
    </ElCard>
  </div>
</template>

<style scoped lang="scss">
/* 指标卡紧凑化：仅本页压缩，公共组件 AnalysisOverview 保持原样 */
.overview-compact {
  gap: 12px;

  :deep(.p-5) {
    gap: 0;
    padding: 12px 16px 0;
  }

  :deep(.p-6) {
    padding: 6px 16px 0;
  }

  :deep(.p-6:last-child) {
    padding-bottom: 12px;
  }
}
</style>
