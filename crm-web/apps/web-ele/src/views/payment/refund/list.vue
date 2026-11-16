<script lang="ts" setup>
import type {
  OnActionClickParams,
  VxeTableGridOptions,
} from '#/adapter/vxe-table';
import type { PaymentApi } from '#/api/crm/payment';

import { onMounted } from 'vue';

import { Page, useVbenDrawer } from '@vben/common-ui';
import { Plus } from '@vben/icons';

import { ElButton, ElMessage, ElMessageBox } from 'element-plus';

import { useVbenVxeGrid } from '#/adapter/vxe-table';
import {
  changeRefundStatus,
  deleteRefund,
  getRefundList,
} from '#/api/crm/payment';
import { REFUND_PERMISSIONS } from '#/constants/crm-batch3-permissions';

import { loadCustomerOptions, loadOrderOptions } from '../shared/options';
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
          return await getRefundList({
            orderId: formValues?.orderId,
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
  } as VxeTableGridOptions<PaymentApi.Refund>,
});

function onActionClick(e: OnActionClickParams<PaymentApi.Refund>) {
  switch (e.code) {
    case 'edit': {
      formDrawerApi.setData(e.row).open();
      break;
    }
    case 'approve': {
      onStatus(e.row, 1, '审核通过');
      break;
    }
    case 'refunded': {
      onStatus(e.row, 2, '标记已退款');
      break;
    }
    case 'reject': {
      onStatus(e.row, 3, '驳回');
      break;
    }
    case 'delete': {
      onDelete(e.row);
      break;
    }
  }
}

async function onStatus(row: PaymentApi.Refund, status: number, text: string) {
  await ElMessageBox.confirm(
    `确认对退款单「${row.refundNo}」执行「${text}」吗？`,
    text,
    { type: 'warning' },
  );
  await changeRefundStatus(row.id, status);
  ElMessage.success(`已${text}`);
  gridApi.query();
}

async function onDelete(row: PaymentApi.Refund) {
  await ElMessageBox.confirm(
    `确认删除退款单「${row.refundNo}」吗？`,
    '删除退款申请',
    { type: 'warning' },
  );
  await deleteRefund(row.id);
  ElMessage.success('已删除');
  gridApi.query();
}

function onCreate() {
  formDrawerApi.setData({}).open();
}

onMounted(async () => {
  await Promise.all([loadCustomerOptions(), loadOrderOptions()]);
});
</script>

<template>
  <Page auto-content-height>
    <FormDrawerComp @success="gridApi.query()" />
    <Grid table-title="退款管理">
      <template #toolbar-tools>
        <ElButton
          v-access:code="REFUND_PERMISSIONS.create"
          type="primary"
          @click="onCreate"
        >
          <Plus class="size-5" />
          新增退款申请
        </ElButton>
      </template>
    </Grid>
  </Page>
</template>
