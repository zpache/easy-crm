<script lang="ts" setup>
import type {
  OnActionClickParams,
  VxeTableGridOptions,
} from '#/adapter/vxe-table';
import type { CustomerApi } from '#/api/crm/customer';

import { Page, useVbenDrawer } from '@vben/common-ui';
import { Plus } from '@vben/icons';

import { ElButton, ElMessage, ElMessageBox } from 'element-plus';

import { useVbenVxeGrid } from '#/adapter/vxe-table';
import { claimCustomer, customerToPool, getCustomerList } from '#/api/crm/customer';
import { CUSTOMER_PERMISSIONS } from '#/constants/permissions';

import { useColumns, useGridFormSchema } from './data';
import FormDrawer from './modules/form.vue';
import MergeDrawer from './modules/merge.vue';
import View360Drawer from './modules/view360.vue';

const [FormDrawerComp, formDrawerApi] = useVbenDrawer({
  connectedComponent: FormDrawer,
  destroyOnClose: true,
});

const [MergeDrawerComp, mergeDrawerApi] = useVbenDrawer({
  connectedComponent: MergeDrawer,
  destroyOnClose: true,
});

const [View360DrawerComp, view360DrawerApi] = useVbenDrawer({
  connectedComponent: View360Drawer,
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
          return await getCustomerList({
            industry: formValues?.industry,
            name: formValues?.name,
            ownerId: formValues?.ownerId,
            pageNum: page.currentPage,
            pageSize: page.pageSize,
            status: formValues?.status,
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
  } as VxeTableGridOptions<CustomerApi.Customer>,
});

function onActionClick(e: OnActionClickParams<CustomerApi.Customer>) {
  switch (e.code) {
    case 'claim': {
      onClaim(e.row);
      break;
    }
    case 'edit': {
      formDrawerApi.setData(e.row).open();
      break;
    }
    case 'merge': {
      mergeDrawerApi.setData({ sourceId: e.row.id, sourceName: e.row.name }).open();
      break;
    }
    case 'toPool': {
      onToPool(e.row);
      break;
    }
    case 'view360': {
      view360DrawerApi.setData(e.row).open();
      break;
    }
  }
}

async function onClaim(row: CustomerApi.Customer) {
  await claimCustomer(row.id);
  ElMessage.success(`已领取「${row.name}」`);
  gridApi.query();
}

async function onToPool(row: CustomerApi.Customer) {
  await ElMessageBox.confirm(
    `确认将「${row.name}」放入公海吗？负责人将被清空。`,
    '放入公海',
    { type: 'warning' },
  );
  await customerToPool(row.id);
  ElMessage.success('已放入公海');
  gridApi.query();
}

function onCreate() {
  formDrawerApi.setData({}).open();
}
</script>

<template>
  <Page auto-content-height>
    <FormDrawerComp @success="gridApi.query()" />
    <MergeDrawerComp @success="gridApi.query()" />
    <View360DrawerComp />
    <Grid table-title="客户列表">
      <template #toolbar-tools>
        <ElButton
          v-access:code="CUSTOMER_PERMISSIONS.create"
          type="primary"
          @click="onCreate"
        >
          <Plus class="size-5" />
          新增客户
        </ElButton>
      </template>
    </Grid>
  </Page>
</template>
