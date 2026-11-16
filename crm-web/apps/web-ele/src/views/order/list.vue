<script lang="ts" setup>
import type {
  OnActionClickParams,
  VxeTableGridOptions,
} from '#/adapter/vxe-table';
import type { OrderApi } from '#/api/crm/order';

import { onMounted } from 'vue';

import { Page, useVbenDrawer } from '@vben/common-ui';
import { Download, Plus } from '@vben/icons';

import { ElButton, ElMessage, ElMessageBox } from 'element-plus';

import { useVbenVxeGrid } from '#/adapter/vxe-table';
import {
  changeOrderStatus,
  deleteOrder,
  getOrderDetail,
  getOrderList,
} from '#/api/crm/order';
import { useCrmExport } from '#/composables/use-crm-export';
import { CRM_ORDER_PERMISSIONS, EXPORT_PERMISSIONS } from '#/constants/crm-batch3-permissions';

import { loadCustomerOptions, useColumns, useGridFormSchema } from './data';
import DetailDrawer from './modules/detail.vue';
import FormDrawer from './modules/form.vue';

/** 状态机下一状态（0→1→2→3→4），与后端 OrderStatus 一致 */
const NEXT_STATUS: Record<number, { label: string; value: number }> = {
  0: { label: '待确认', value: 1 },
  1: { label: '已确认', value: 2 },
  2: { label: '已交付', value: 3 },
  3: { label: '已完成', value: 4 },
};

const [FormDrawerComp, formDrawerApi] = useVbenDrawer({
  connectedComponent: FormDrawer,
  destroyOnClose: true,
});

const [DetailDrawerComp, detailDrawerApi] = useVbenDrawer({
  connectedComponent: DetailDrawer,
  destroyOnClose: true,
});

const { exportNow, exporting } = useCrmExport('orders');

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
          return await getOrderList({
            customerId: formValues?.customerId,
            orderNo: formValues?.orderNo,
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
  } as VxeTableGridOptions<OrderApi.Order>,
});

onMounted(async () => {
  await loadCustomerOptions();
});

function onActionClick(e: OnActionClickParams<OrderApi.Order>) {
  switch (e.code) {
    case 'cancel': {
      onCancel(e.row);
      break;
    }
    case 'delete': {
      onDelete(e.row);
      break;
    }
    case 'detail': {
      onDetail(e.row);
      break;
    }
    case 'edit': {
      formDrawerApi.setData(e.row).open();
      break;
    }
    case 'status': {
      onNextStatus(e.row);
      break;
    }
  }
}

async function onDetail(row: OrderApi.Order) {
  const detail = await getOrderDetail(row.id);
  detailDrawerApi.setData(detail).open();
}

async function onNextStatus(row: OrderApi.Order) {
  const next = NEXT_STATUS[row.status];
  if (!next) {
    ElMessage.warning('当前状态无法继续推进');
    return;
  }
  await ElMessageBox.confirm(
    `确认将订单「${row.orderNo}」推进到「${next.label}」吗？`,
    '状态流转',
    { type: 'warning' },
  );
  await changeOrderStatus(row.id, { status: next.value });
  ElMessage.success(`已流转为「${next.label}」`);
  gridApi.query();
}

async function onCancel(row: OrderApi.Order) {
  await ElMessageBox.confirm(
    `确认取消订单「${row.orderNo}」吗？该操作不可撤销。`,
    '取消订单',
    { type: 'warning' },
  );
  await changeOrderStatus(row.id, { status: 9 });
  ElMessage.success('订单已取消');
  gridApi.query();
}

async function onDelete(row: OrderApi.Order) {
  await ElMessageBox.confirm(
    `确认删除订单「${row.orderNo}」吗？`,
    '删除订单',
    { type: 'warning' },
  );
  await deleteOrder(row.id);
  ElMessage.success('已删除');
  gridApi.query();
}

/** 按当前筛选条件导出（后端 /exports/orders，流式 xlsx） */
async function onExport() {
  const values = await gridApi.formApi.getValues();
  await exportNow({ keyword: values?.orderNo, status: values?.status });
}

function onCreate() {
  formDrawerApi.setData({}).open();
}
</script>

<template>
  <Page auto-content-height>
    <FormDrawerComp @success="gridApi.query()" />
    <DetailDrawerComp />
    <Grid table-title="订单列表">
      <template #toolbar-tools>
        <ElButton
          v-access:code="EXPORT_PERMISSIONS.order"
          :loading="exporting"
          class="mr-2"
          type="default"
          @click="onExport"
        >
          <Download class="size-5" />
          导出 Excel
        </ElButton>
        <ElButton
          v-access:code="CRM_ORDER_PERMISSIONS.create"
          type="primary"
          @click="onCreate"
        >
          <Plus class="size-5" />
          新增订单
        </ElButton>
      </template>
    </Grid>
  </Page>
</template>
