<script lang="ts" setup>
import type {
  OnActionClickParams,
  VxeTableGridOptions,
} from '#/adapter/vxe-table';
import type { SlaApi } from '#/api/crm/support';

import { Page, useVbenDrawer } from '@vben/common-ui';
import { Plus } from '@vben/icons';

import { ElButton, ElMessage, ElMessageBox } from 'element-plus';

import { useVbenVxeGrid } from '#/adapter/vxe-table';
import {
  deleteSlaPolicy,
  getSlaPolicyList,
  SUPPORT_PERMISSIONS,
} from '#/api/crm/support';

import { useColumns, useGridFormSchema } from './data';
import FormDrawer from './modules/form.vue';

const [FormDrawerComp, formDrawerApi] = useVbenDrawer({
  connectedComponent: FormDrawer,
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
          return await getSlaPolicyList({
            enabled: formValues?.enabled,
            pageNum: page.currentPage,
            pageSize: page.pageSize,
            priority: formValues?.priority,
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
  } as VxeTableGridOptions<SlaApi.SlaPolicy>,
});

function onActionClick(e: OnActionClickParams<SlaApi.SlaPolicy>) {
  switch (e.code) {
    case 'delete': {
      onDelete(e.row);
      break;
    }
    case 'edit': {
      formDrawerApi.setData(e.row).open();
      break;
    }
  }
}

async function onDelete(row: SlaApi.SlaPolicy) {
  await ElMessageBox.confirm(
    `确认删除「${row.ticketTypeName || row.ticketType} / ${row.priorityName || row.priority}」的 SLA 策略吗？`,
    '删除 SLA 策略',
    { type: 'warning' },
  );
  await deleteSlaPolicy(row.id);
  ElMessage.success('已删除');
  gridApi.query();
}

function onCreate() {
  formDrawerApi.setData({}).open();
}
</script>

<template>
  <Page auto-content-height>
    <FormDrawerComp @success="gridApi.query()" />
    <Grid table-title="SLA 策略配置">
      <template #toolbar-tools>
        <ElButton
          v-access:code="SUPPORT_PERMISSIONS.slaCreate"
          type="primary"
          @click="onCreate"
        >
          <Plus class="size-5" />
          新增策略
        </ElButton>
      </template>
    </Grid>
  </Page>
</template>
