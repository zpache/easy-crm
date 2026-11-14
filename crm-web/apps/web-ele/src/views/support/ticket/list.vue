<script lang="ts" setup>
import type {
  OnActionClickParams,
  VxeTableGridOptions,
} from '#/adapter/vxe-table';
import type { TicketApi } from '#/api/crm/support';

import { onMounted } from 'vue';

import { Page, useVbenDrawer } from '@vben/common-ui';
import { Plus } from '@vben/icons';

import { ElButton } from 'element-plus';

import { useVbenVxeGrid } from '#/adapter/vxe-table';
import { getTicketList, SUPPORT_PERMISSIONS } from '#/api/crm/support';

import {
  loadCustomerOptions,
  loadUserOptions,
  useColumns,
  useGridFormSchema,
} from './data';
import AssignDrawer from './modules/assign.vue';
import DetailDrawer from './modules/detail.vue';
import FlowDrawer from './modules/flow.vue';
import FormDrawer from './modules/form.vue';

const [FormDrawerComp, formDrawerApi] = useVbenDrawer({
  connectedComponent: FormDrawer,
  destroyOnClose: true,
});

const [AssignDrawerComp, assignDrawerApi] = useVbenDrawer({
  connectedComponent: AssignDrawer,
  destroyOnClose: true,
});

const [FlowDrawerComp, flowDrawerApi] = useVbenDrawer({
  connectedComponent: FlowDrawer,
  destroyOnClose: true,
});

const [DetailDrawerComp, detailDrawerApi] = useVbenDrawer({
  connectedComponent: DetailDrawer,
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
          const range = formValues?.createTimeRange as
            | [string, string]
            | undefined;
          return await getTicketList({
            createTimeEnd: range?.[1],
            createTimeStart: range?.[0],
            keyword: formValues?.keyword,
            overdueFlag: formValues?.overdueFlag,
            pageNum: page.currentPage,
            pageSize: page.pageSize,
            priority: formValues?.priority,
            status: formValues?.status,
            ticketType: formValues?.ticketType,
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
  } as VxeTableGridOptions<TicketApi.Ticket>,
});

onMounted(async () => {
  await Promise.all([loadCustomerOptions(), loadUserOptions()]);
});

function onActionClick(e: OnActionClickParams<TicketApi.Ticket>) {
  switch (e.code) {
    case 'assign': {
      assignDrawerApi.setData(e.row).open();
      break;
    }
    case 'detail': {
      detailDrawerApi.setData(e.row).open();
      break;
    }
    case 'edit': {
      formDrawerApi.setData(e.row).open();
      break;
    }
    case 'flow': {
      flowDrawerApi.setData(e.row).open();
      break;
    }
  }
}

function onCreate() {
  formDrawerApi.setData({}).open();
}
</script>

<template>
  <Page auto-content-height>
    <FormDrawerComp @success="gridApi.query()" />
    <AssignDrawerComp @success="gridApi.query()" />
    <FlowDrawerComp @success="gridApi.query()" />
    <DetailDrawerComp @success="gridApi.query()" />
    <Grid table-title="服务工单">
      <template #toolbar-tools>
        <ElButton
          v-access:code="SUPPORT_PERMISSIONS.ticketCreate"
          type="primary"
          @click="onCreate"
        >
          <Plus class="size-5" />
          新增工单
        </ElButton>
      </template>
    </Grid>
  </Page>
</template>
