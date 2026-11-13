<script lang="ts" setup>
import type {
  OnActionClickParams,
  VxeTableGridOptions,
} from '#/adapter/vxe-table';
import type { OpportunityApi } from '#/api/crm/opportunity';

import { onMounted } from 'vue';

import { Page, useVbenDrawer } from '@vben/common-ui';
import { Plus, Settings } from '@vben/icons';

import { ElButton } from 'element-plus';

import { useVbenVxeGrid } from '#/adapter/vxe-table';
import { getOpportunityList, getStages } from '#/api/crm/opportunity';
import { OPPORTUNITY_PERMISSIONS } from '#/constants/permissions';

import { loadCustomerOptions, stageOptions, useColumns, useGridFormSchema } from './data';
import FormDrawer from './modules/form.vue';
import ForecastDrawer from './modules/forecast.vue';
import StageChangeDrawer from './modules/stage-change.vue';
import StageConfigDrawer from './modules/stage-config.vue';

const [FormDrawerComp, formDrawerApi] = useVbenDrawer({
  connectedComponent: FormDrawer,
  destroyOnClose: true,
});

const [StageChangeDrawerComp, stageChangeDrawerApi] = useVbenDrawer({
  connectedComponent: StageChangeDrawer,
  destroyOnClose: true,
});

const [ForecastDrawerComp, forecastDrawerApi] = useVbenDrawer({
  connectedComponent: ForecastDrawer,
  destroyOnClose: true,
});

const [StageConfigDrawerComp, stageConfigDrawerApi] = useVbenDrawer({
  connectedComponent: StageConfigDrawer,
  destroyOnClose: true,
});

const [Grid, gridApi] = useVbenVxeGrid({
  formOptions: {
    schema: useGridFormSchema(),
    submitOnChange: true,
  },
  gridOptions: {
    columns: useColumns(onActionClick),
    height: 'auto',
    keepSource: true,
    proxyConfig: {
      ajax: {
        query: async ({ page }, formValues) => {
          const range = formValues?.expectedCloseRange as
            | [string, string]
            | undefined;
          return await getOpportunityList({
            customerId: formValues?.customerId,
            pageNum: page.currentPage,
            pageSize: page.pageSize,
            stageCode: formValues?.stageCode,
            endDate: range?.[1],
            ownerId: formValues?.ownerId,
            startDate: range?.[0],
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
  } as VxeTableGridOptions<OpportunityApi.Opportunity>,
});

function onActionClick(e: OnActionClickParams<OpportunityApi.Opportunity>) {
  switch (e.code) {
    case 'edit': {
      formDrawerApi.setData(e.row).open();
      break;
    }
    case 'stage': {
      stageChangeDrawerApi.setData(e.row).open();
      break;
    }
  }
}

function onForecast() {
  forecastDrawerApi.setData({}).open();
}

function onStageConfig() {
  stageConfigDrawerApi.setData({}).open();
}

onMounted(async () => {
  stageOptions.value = await getStages();
  await loadCustomerOptions();
});
</script>

<template>
  <Page auto-content-height>
    <FormDrawerComp @success="gridApi.query()" />
    <StageChangeDrawerComp @success="gridApi.query()" />
    <ForecastDrawerComp />
    <StageConfigDrawerComp @success="gridApi.query()" />
    <Grid table-title="商机列表">
      <template #toolbar-tools>
        <ElButton
          v-access:code="OPPORTUNITY_PERMISSIONS.stageManage"
          type="default"
          @click="onStageConfig"
        >
          <Settings class="size-5" />
          阶段配置
        </ElButton>
        <ElButton
          v-access:code="OPPORTUNITY_PERMISSIONS.forecast"
          type="default"
          @click="onForecast"
        >
          销售预测
        </ElButton>
        <ElButton
          v-access:code="OPPORTUNITY_PERMISSIONS.create"
          type="primary"
          @click="formDrawerApi.setData({}).open()"
        >
          <Plus class="size-5" />
          新增商机
        </ElButton>
      </template>
    </Grid>
  </Page>
</template>
