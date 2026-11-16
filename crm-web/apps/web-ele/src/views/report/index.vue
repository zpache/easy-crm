<script lang="ts" setup>
import type {
  OnActionClickParams,
  VxeTableGridOptions,
} from '#/adapter/vxe-table';
import type { EchartsUIType } from '@vben/plugins/echarts';
import type { ReportApi } from '#/api/crm/report';

import { computed, onMounted, ref } from 'vue';

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
  ElOption,
  ElSelect,
  ElTabPane,
  ElTable,
  ElTableColumn,
  ElTabs,
} from 'element-plus';

import { useVbenVxeGrid } from '#/adapter/vxe-table';
import {
  deleteSalesTarget,
  getAchievement,
  getCustomerDistribution,
  getFunnel,
  getRanking,
  getRevenue,
  getSalesTargetList,
} from '#/api/crm/report';
import { usePermission } from '#/composables/use-permission';
import { SALES_TARGET_PERMISSIONS } from '#/constants/crm-batch3-permissions';

import { useColumns, useGridFormSchema } from './data';
import FormDrawer from './modules/form.vue';

function defaultMonth(offset: number) {
  const d = new Date();
  d.setMonth(d.getMonth() + offset);
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`;
}

const { can } = usePermission();

const filters = ref<ReportApi.AchievementQuery>({
  deptId: undefined,
  endMonth: defaultMonth(0),
  ownerId: undefined,
  startMonth: defaultMonth(-11),
});

const activeTab = ref('achievement');
const distributionDimension = ref('industry');

const achievement = ref<null | ReportApi.Achievement>(null);
const funnel = ref<null | ReportApi.Funnel>(null);
const ranking = ref<null | ReportApi.Ranking>(null);
const revenue = ref<null | ReportApi.Revenue>(null);
const distribution = ref<null | ReportApi.Distribution>(null);

const chartRef = ref<EchartsUIType>();
const { renderEcharts } = useEcharts(chartRef);
const funnelChartRef = ref<EchartsUIType>();
const { renderEcharts: renderFunnel } = useEcharts(funnelChartRef);
const revenueChartRef = ref<EchartsUIType>();
const { renderEcharts: renderRevenue } = useEcharts(revenueChartRef);
const distributionChartRef = ref<EchartsUIType>();
const { renderEcharts: renderDistribution } = useEcharts(distributionChartRef);

const tabVisible = computed(() => ({
  achievement: can('crm:report:achievement'),
  customer: can('crm:report:customer'),
  funnel: can('crm:report:funnel'),
  ranking: can('crm:report:ranking'),
  revenue: can('crm:report:revenue'),
}));

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
    toolbarConfig: {
      custom: true,
      export: false,
      refresh: true,
      search: true,
      zoom: true,
    },
  } as VxeTableGridOptions<ReportApi.SalesTarget>,
});

function formatMoney(value: number | undefined | null) {
  return value == null ? '-' : Number(value).toLocaleString('zh-CN');
}

/** 后端 achievementRate 为比值（0.xx），展示时乘 100 */
function formatRate(value: number | undefined | null) {
  if (value == null) {
    return '-';
  }
  return `${(Number(value) * 100).toFixed(2)}%`;
}

/** 漏斗 / 回款 / 客户分布接口返回的已是百分比数值（32.15 表示 32.15%） */
function formatPercent(value: number | undefined | null) {
  return value == null ? '-' : `${Number(value).toFixed(2)}%`;
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

async function loadFunnel() {
  const data = await getFunnel(filters.value);
  funnel.value = data;
  await renderFunnel({
    grid: { bottom: 30, left: 70, right: 40, top: 40 },
    series: [
      {
        data: data.stages.map((s) => s.count),
        itemStyle: { color: '#5b8ff9' },
        label: { position: 'top', show: true },
        name: '数量',
        type: 'bar',
      },
      {
        data: data.stages.map((s) => (s.amount == null ? 0 : Number(s.amount))),
        name: '金额',
        type: 'line',
        yAxisIndex: 1,
      },
    ],
    tooltip: { trigger: 'axis' },
    xAxis: { data: data.stages.map((s) => s.label), type: 'category' },
    yAxis: [
      { name: '数量', type: 'value' },
      { name: '金额(元)', type: 'value' },
    ],
  });
}

async function loadRanking() {
  ranking.value = await getRanking(filters.value);
}

async function loadRevenue() {
  const data = await getRevenue(filters.value);
  revenue.value = data;
  await renderRevenue({
    grid: { bottom: 30, left: 70, right: 40, top: 50 },
    legend: { data: ['计划回款', '实收', '已开票', '已退款'], top: 0 },
    series: [
      {
        data: data.rows.map((r) => Number(r.planAmount)),
        name: '计划回款',
        type: 'bar',
      },
      {
        data: data.rows.map((r) => Number(r.paymentAmount)),
        name: '实收',
        type: 'bar',
      },
      {
        data: data.rows.map((r) => Number(r.invoiceAmount)),
        name: '已开票',
        type: 'bar',
      },
      {
        data: data.rows.map((r) => Number(r.refundAmount)),
        name: '已退款',
        type: 'bar',
      },
    ],
    tooltip: { trigger: 'axis' },
    xAxis: { data: data.rows.map((r) => r.month), type: 'category' },
    yAxis: { name: '金额(元)', type: 'value' },
  });
}

async function loadDistribution() {
  const data = await getCustomerDistribution({
    ...filters.value,
    dimension: distributionDimension.value,
  });
  distribution.value = data;
  await renderDistribution({
    legend: { bottom: 0, type: 'scroll' },
    series: [
      {
        data: data.items.map((i) => ({ name: i.label, value: i.count })),
        name: '客户数',
        radius: ['40%', '68%'],
        type: 'pie',
      },
    ],
    tooltip: { trigger: 'item' },
  });
}

/** 查询按钮：只刷新当前页签，避免无谓请求 */
async function onSearch() {
  switch (activeTab.value) {
    case 'achievement': {
      gridApi.query();
      await loadAchievement();
      break;
    }
    case 'customer': {
      await loadDistribution();
      break;
    }
    case 'funnel': {
      await loadFunnel();
      break;
    }
    case 'ranking': {
      await loadRanking();
      break;
    }
    case 'revenue': {
      await loadRevenue();
      break;
    }
  }
}

async function onTabChange(name: number | string) {
  activeTab.value = String(name);
  await onSearch();
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
      <ElCard shadow="never">
        <template #header>
          <span>报表中心（按时间 / 部门 / 负责人筛选）</span>
        </template>
        <div class="flex flex-wrap items-center gap-3">
          <div class="flex items-center gap-1">
            <span class="text-sm text-gray-500">起始年月</span>
            <ElInput
              v-model="filters.startMonth"
              class="w-32"
              placeholder="YYYY-MM"
            />
          </div>
          <div class="flex items-center gap-1">
            <span class="text-sm text-gray-500">结束年月</span>
            <ElInput
              v-model="filters.endMonth"
              class="w-32"
              placeholder="YYYY-MM"
            />
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
          <ElButton type="primary" @click="onSearch">查询</ElButton>
        </div>
      </ElCard>

      <ElTabs v-model="activeTab" class="mt-4" @tab-change="onTabChange">
        <ElTabPane label="销售达成" name="achievement">
          <ElCard shadow="never">
            <div class="flex flex-wrap gap-6 text-sm">
              <span>
                目标合计：<b>{{ formatMoney(achievement?.totalTarget) }}</b>
              </span>
              <span>
                订单实绩：<b>{{ formatMoney(achievement?.totalOrderAmount) }}</b>
              </span>
              <span>
                商机赢单实绩：<b>{{
                  formatMoney(achievement?.totalOpportunityAmount)
                }}</b>
              </span>
              <span>
                达成率：<b>{{ formatRate(achievement?.achievementRate) }}</b>
              </span>
            </div>
          </ElCard>
          <ElCard class="mt-4" shadow="never">
            <template #header>
              <span>月度目标 vs 实绩</span>
            </template>
            <EchartsUI ref="chartRef" height="320px" />
          </ElCard>
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
        </ElTabPane>

        <ElTabPane v-if="tabVisible.funnel" label="销售漏斗" name="funnel">
          <ElCard shadow="never">
            <template #header>
              <div class="flex flex-wrap items-center gap-3">
                <span>线索 → 有效线索 → 商机 → 赢单 → 订单</span>
                <span class="text-xs text-gray-400">
                  环节转化率为相邻环节数量比；存在「未转化线索直接建商机」时可能大于 100%
                </span>
              </div>
            </template>
            <EchartsUI ref="funnelChartRef" height="320px" />
            <ElTable :data="funnel?.stages ?? []" border class="mt-4" size="small">
              <ElTableColumn prop="label" label="环节" min-width="160" />
              <ElTableColumn prop="count" label="数量" width="110" />
              <ElTableColumn label="金额" width="150">
                <template #default="{ row }">
                  {{ row.amount == null ? '-' : formatMoney(row.amount) }}
                </template>
              </ElTableColumn>
              <ElTableColumn label="环节转化率" width="130">
                <template #default="{ row }">
                  {{ formatPercent(row.conversionRate) }}
                </template>
              </ElTableColumn>
            </ElTable>
          </ElCard>
        </ElTabPane>

        <ElTabPane v-if="tabVisible.ranking" label="销售排行" name="ranking">
          <ElCard shadow="never">
            <template #header>
              <span>负责人实绩排行（按赢单金额降序，最多 20 名）</span>
            </template>
            <ElTable :data="ranking?.rows ?? []" border size="small">
              <ElTableColumn prop="rank" label="排名" width="80" />
              <ElTableColumn label="负责人" min-width="140">
                <template #default="{ row }">
                  {{ row.ownerName ?? `#${row.ownerId}` }}
                </template>
              </ElTableColumn>
              <ElTableColumn prop="wonCount" label="赢单数" width="100" />
              <ElTableColumn label="赢单金额" width="150">
                <template #default="{ row }">
                  {{ formatMoney(row.wonAmount) }}
                </template>
              </ElTableColumn>
              <ElTableColumn prop="orderCount" label="订单数" width="100" />
              <ElTableColumn label="订单金额" width="150">
                <template #default="{ row }">
                  {{ formatMoney(row.orderAmount) }}
                </template>
              </ElTableColumn>
              <ElTableColumn label="回款金额" width="150">
                <template #default="{ row }">
                  {{ formatMoney(row.paymentAmount) }}
                </template>
              </ElTableColumn>
            </ElTable>
          </ElCard>
        </ElTabPane>

        <ElTabPane v-if="tabVisible.revenue" label="回款分析" name="revenue">
          <ElCard shadow="never">
            <div class="flex flex-wrap gap-6 text-sm">
              <span>
                计划合计：<b>{{ formatMoney(revenue?.totalPlanAmount) }}</b>
              </span>
              <span>
                实收合计：<b>{{ formatMoney(revenue?.totalPaymentAmount) }}</b>
              </span>
              <span>
                开票合计：<b>{{ formatMoney(revenue?.totalInvoiceAmount) }}</b>
              </span>
              <span>
                退款合计：<b>{{ formatMoney(revenue?.totalRefundAmount) }}</b>
              </span>
              <span>
                净流入：<b>{{ formatMoney(revenue?.totalNetAmount) }}</b>
              </span>
            </div>
            <EchartsUI ref="revenueChartRef" class="mt-4" height="320px" />
            <ElTable :data="revenue?.rows ?? []" border class="mt-4" size="small">
              <ElTableColumn prop="month" label="月份" width="110" />
              <ElTableColumn label="计划回款" width="140">
                <template #default="{ row }">
                  {{ formatMoney(row.planAmount) }}
                </template>
              </ElTableColumn>
              <ElTableColumn label="实收" width="140">
                <template #default="{ row }">
                  {{ formatMoney(row.paymentAmount) }}
                </template>
              </ElTableColumn>
              <ElTableColumn label="已开票" width="140">
                <template #default="{ row }">
                  {{ formatMoney(row.invoiceAmount) }}
                </template>
              </ElTableColumn>
              <ElTableColumn label="已退款" width="140">
                <template #default="{ row }">
                  {{ formatMoney(row.refundAmount) }}
                </template>
              </ElTableColumn>
              <ElTableColumn label="净流入" width="140">
                <template #default="{ row }">
                  {{ formatMoney(row.netAmount) }}
                </template>
              </ElTableColumn>
              <ElTableColumn label="计划完成率" width="130">
                <template #default="{ row }">
                  {{ formatPercent(row.completionRate) }}
                </template>
              </ElTableColumn>
            </ElTable>
          </ElCard>
        </ElTabPane>

        <ElTabPane v-if="tabVisible.customer" label="客户分布" name="customer">
          <ElCard shadow="never">
            <template #header>
              <div class="flex items-center gap-2">
                <span>客户分布</span>
                <ElSelect
                  v-model="distributionDimension"
                  class="w-32"
                  @change="loadDistribution"
                >
                  <ElOption label="按行业" value="industry" />
                  <ElOption label="按区域" value="region" />
                  <ElOption label="按级别" value="level" />
                  <ElOption label="按状态" value="status" />
                </ElSelect>
                <span class="text-sm text-gray-500">
                  客户总数：{{ distribution?.total ?? 0 }}
                </span>
              </div>
            </template>
            <EchartsUI ref="distributionChartRef" height="320px" />
            <ElTable
              :data="distribution?.items ?? []"
              border
              class="mt-4"
              size="small"
            >
              <ElTableColumn prop="label" label="分类" min-width="160" />
              <ElTableColumn prop="count" label="客户数" width="120" />
              <ElTableColumn label="占比" width="120">
                <template #default="{ row }">
                  {{ formatPercent(row.percent) }}
                </template>
              </ElTableColumn>
            </ElTable>
          </ElCard>
        </ElTabPane>
      </ElTabs>
    </div>
  </Page>
</template>
