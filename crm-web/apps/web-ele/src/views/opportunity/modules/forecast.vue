<script lang="ts" setup>
import type { OpportunityApi } from '#/api/crm/opportunity';

import { ref } from 'vue';

import { useVbenDrawer } from '@vben/common-ui';

import {
  ElAlert,
  ElDescriptions,
  ElDescriptionsItem,
  ElTable,
  ElTableColumn,
} from 'element-plus';

import { getForecast } from '#/api/crm/opportunity';

const result = ref<null | OpportunityApi.Forecast>(null);

const [Drawer, drawerApi] = useVbenDrawer({
  async onOpenChange(isOpen) {
    if (isOpen) {
      result.value = await getForecast({});
    }
  },
});

function formatMoney(value: unknown) {
  return value == null ? '-' : Number(value).toLocaleString();
}
</script>

<template>
  <Drawer class="w-[640px]" title="销售预测（按阶段聚合）">
    <template v-if="result">
      <ElAlert
        :closable="false"
        class="mb-3"
        show-icon
        :title="`进行中商机 ${result.totalCount} 个，加权预测合计 ${formatMoney(result.totalForecast)}（赢单率 × 预计金额，一条聚合 SQL，不含赢单/丢单终态）`"
        type="info"
      />
      <ElTable :data="result.rows" border size="small">
        <ElTableColumn label="阶段" prop="stageName" width="120" />
        <ElTableColumn label="商机数" prop="cnt" width="90" />
        <ElTableColumn label="预计金额合计" min-width="130">
          <template #default="{ row }">{{ formatMoney(row.totalAmount) }}</template>
        </ElTableColumn>
        <ElTableColumn label="加权预测" min-width="130">
          <template #default="{ row }">{{ formatMoney(row.forecastAmount) }}</template>
        </ElTableColumn>
      </ElTable>
      <ElDescriptions :column="1" border class="mt-3" size="small">
        <ElDescriptionsItem label="说明">
          预测金额 = SUM(预计金额 × 阶段默认赢单率 / 100)；
          可按部门 + 预计成交时间范围聚合（接口 /opportunities/forecast）。
        </ElDescriptionsItem>
      </ElDescriptions>
    </template>
  </Drawer>
</template>
