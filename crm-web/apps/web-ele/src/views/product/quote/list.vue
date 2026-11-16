<script lang="ts" setup>
import type {
  OnActionClickParams,
  VxeTableGridOptions,
} from '#/adapter/vxe-table';
import type { ProductApi } from '#/api/crm/product';

import { onMounted } from 'vue';

import { Page, useVbenDrawer } from '@vben/common-ui';
import { Plus } from '@vben/icons';

import { ElButton, ElMessage, ElMessageBox } from 'element-plus';

import { useVbenVxeGrid } from '#/adapter/vxe-table';
import { deleteQuote, getQuoteList } from '#/api/crm/product';
import { QUOTE_PERMISSIONS } from '#/constants/crm-batch3-permissions';

import { loadCustomerOptions, useColumns, useGridFormSchema } from './data';
import DetailDrawer from './modules/detail.vue';
import FormDrawer from './modules/form.vue';

const [FormDrawerComp, formDrawerApi] = useVbenDrawer({
  connectedComponent: FormDrawer,
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
          return await getQuoteList({
            customerId: formValues?.customerId,
            pageNum: page.currentPage,
            pageSize: page.pageSize,
            quoteNo: formValues?.quoteNo,
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
  } as VxeTableGridOptions<ProductApi.Quote>,
});

function onActionClick(e: OnActionClickParams<ProductApi.Quote>) {
  switch (e.code) {
    case 'detail': {
      detailDrawerApi.setData(e.row).open();
      break;
    }
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

async function onDelete(row: ProductApi.Quote) {
  await ElMessageBox.confirm(
    `确认删除报价单「${row.quoteNo}」及其明细吗？`,
    '删除报价单',
    { type: 'warning' },
  );
  await deleteQuote(row.id);
  ElMessage.success('已删除');
  gridApi.query();
}

function onCreate() {
  formDrawerApi.setData({}).open();
}

onMounted(async () => {
  await loadCustomerOptions();
});
</script>

<template>
  <Page auto-content-height>
    <FormDrawerComp @success="gridApi.query()" />
    <DetailDrawerComp />
    <Grid table-title="报价单">
      <template #toolbar-tools>
        <ElButton
          v-access:code="QUOTE_PERMISSIONS.create"
          type="primary"
          @click="onCreate"
        >
          <Plus class="size-5" />
          新增报价单
        </ElButton>
      </template>
    </Grid>
  </Page>
</template>
