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
  changeInvoiceStatus,
  deleteInvoice,
  getInvoiceList,
  updateInvoice,
} from '#/api/crm/payment';
import { INVOICE_PERMISSIONS } from '#/constants/crm-batch3-permissions';

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
          return await getInvoiceList({
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
  } as VxeTableGridOptions<PaymentApi.Invoice>,
});

function onActionClick(e: OnActionClickParams<PaymentApi.Invoice>) {
  switch (e.code) {
    case 'edit': {
      formDrawerApi.setData(e.row).open();
      break;
    }
    case 'issue': {
      onIssue(e.row);
      break;
    }
    case 'void': {
      onVoid(e.row);
      break;
    }
    case 'delete': {
      onDelete(e.row);
      break;
    }
  }
}

async function onIssue(row: PaymentApi.Invoice) {
  const { value } = await ElMessageBox.prompt(
    '确认开票后请填写发票号（可留空，后续在编辑页补录）',
    '确认开票',
    { inputPattern: /^[\w-]{0,64}$/, inputPlaceholder: '发票号' },
  );
  if (value) {
    // 先回填发票号，再推进状态，避免「已开票但无票号」的中间态
    await updateInvoice(row.id, {
      amount: row.amount,
      applyDate: row.applyDate,
      customerId: row.customerId,
      invoiceNo: value,
      orderId: row.orderId,
      remark: row.remark,
      taxNo: row.taxNo,
      title: row.title,
    });
  }
  await changeInvoiceStatus(row.id, 1);
  ElMessage.success('已标记为已开票');
  gridApi.query();
}

async function onVoid(row: PaymentApi.Invoice) {
  await ElMessageBox.confirm('作废后该开票申请不可再修改，确认作废吗？', '作废开票', {
    type: 'warning',
  });
  await changeInvoiceStatus(row.id, 2);
  ElMessage.success('已作废');
  gridApi.query();
}

async function onDelete(row: PaymentApi.Invoice) {
  await ElMessageBox.confirm('确认删除该开票申请吗？', '删除开票申请', {
    type: 'warning',
  });
  await deleteInvoice(row.id);
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
    <Grid table-title="开票管理">
      <template #toolbar-tools>
        <ElButton
          v-access:code="INVOICE_PERMISSIONS.create"
          type="primary"
          @click="onCreate"
        >
          <Plus class="size-5" />
          新增开票申请
        </ElButton>
      </template>
    </Grid>
  </Page>
</template>
