<script lang="ts" setup>
import type {
  OnActionClickParams,
  VxeTableGridOptions,
} from '#/adapter/vxe-table';
import type { ProductApi } from '#/api/crm/product';

import { Page, useVbenDrawer } from '@vben/common-ui';
import { Plus } from '@vben/icons';

import { ElButton, ElMessage, ElMessageBox } from 'element-plus';

import { useVbenVxeGrid } from '#/adapter/vxe-table';
import { deleteProduct, getProductList } from '#/api/crm/product';
import { PRODUCT_PERMISSIONS } from '#/constants/crm-batch3-permissions';

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
          return await getProductList({
            category: formValues?.category,
            keyword: formValues?.keyword,
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
  } as VxeTableGridOptions<ProductApi.Product>,
});

function onActionClick(e: OnActionClickParams<ProductApi.Product>) {
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

async function onDelete(row: ProductApi.Product) {
  await ElMessageBox.confirm(`确认删除产品「${row.name}」吗？`, '删除产品', {
    type: 'warning',
  });
  await deleteProduct(row.id);
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
    <Grid table-title="产品列表">
      <template #toolbar-tools>
        <ElButton
          v-access:code="PRODUCT_PERMISSIONS.create"
          type="primary"
          @click="onCreate"
        >
          <Plus class="size-5" />
          新增产品
        </ElButton>
      </template>
    </Grid>
  </Page>
</template>
