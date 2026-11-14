<script lang="ts" setup>
import type {
  OnActionClickParams,
  VxeTableGridOptions,
} from '#/adapter/vxe-table';
import type { ContractApi } from '#/api/crm/contract';

import { onMounted } from 'vue';

import { Page, useVbenDrawer } from '@vben/common-ui';
import { Plus } from '@vben/icons';

import { ElButton, ElMessage, ElMessageBox } from 'element-plus';

import { useVbenVxeGrid } from '#/adapter/vxe-table';
import { deleteContract, getContractList } from '#/api/crm/contract';
import { CONTRACT_PERMISSIONS } from '#/constants/crm-batch3-permissions';

import { loadCustomerOptions, useColumns, useGridFormSchema } from './data';
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
          return await getContractList({
            contractNo: formValues?.contractNo,
            customerId: formValues?.customerId,
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
  } as VxeTableGridOptions<ContractApi.Contract>,
});

onMounted(async () => {
  await loadCustomerOptions();
});

function onActionClick(e: OnActionClickParams<ContractApi.Contract>) {
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

async function onDelete(row: ContractApi.Contract) {
  await ElMessageBox.confirm(
    `确认删除合同「${row.contractNo}」吗？`,
    '删除合同',
    { type: 'warning' },
  );
  await deleteContract(row.id);
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
    <Grid table-title="合同列表">
      <template #toolbar-tools>
        <ElButton
          v-access:code="CONTRACT_PERMISSIONS.create"
          type="primary"
          @click="onCreate"
        >
          <Plus class="size-5" />
          新增合同
        </ElButton>
      </template>
    </Grid>
  </Page>
</template>
