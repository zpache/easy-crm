<script lang="ts" setup>
import type {
  OnActionClickParams,
  VxeTableGridOptions,
} from '#/adapter/vxe-table';
import type { PaymentApi } from '#/api/crm/payment';

import { ref } from 'vue';

import { Page, useVbenDrawer } from '@vben/common-ui';
import { Plus } from '@vben/icons';

import { ElButton, ElMessage, ElMessageBox, ElTabPane, ElTabs } from 'element-plus';

import { useVbenVxeGrid } from '#/adapter/vxe-table';
import {
  deletePayment,
  deletePaymentPlan,
  getPaymentList,
  getPaymentPlanList,
  writeoffPayment,
} from '#/api/crm/payment';
import {
  PAYMENT_PERMISSIONS,
  PAYMENT_PLAN_PERMISSIONS,
} from '#/constants/crm-batch3-permissions';

import {
  usePaymentColumns,
  usePaymentGridFormSchema,
  usePlanColumns,
  usePlanGridFormSchema,
} from './data';
import PaymentFormDrawer from './modules/payment-form.vue';
import PlanFormDrawer from './modules/plan-form.vue';

const activeTab = ref<'payment' | 'plan'>('plan');

const [PlanFormComp, planFormApi] = useVbenDrawer({
  connectedComponent: PlanFormDrawer,
  destroyOnClose: true,
});

const [PaymentFormComp, paymentFormApi] = useVbenDrawer({
  connectedComponent: PaymentFormDrawer,
  destroyOnClose: true,
});

// ---------------- 回款计划 ----------------
const [PlanGrid, planGridApi] = useVbenVxeGrid({
  formOptions: { schema: usePlanGridFormSchema(), submitOnChange: true },
  gridOptions: {
    columns: usePlanColumns(onPlanActionClick),
    height: 'auto',
    keepSource: true,
    proxyConfig: {
      ajax: {
        query: async ({ page }, formValues) => {
          return await getPaymentPlanList({
            orderId: formValues?.orderId,
            pageNum: page.currentPage,
            pageSize: page.pageSize,
            status: formValues?.status,
          });
        },
      },
    },
    rowConfig: { keyField: 'id' },
    toolbarConfig: { custom: true, export: false, refresh: true, search: true, zoom: true },
  } as VxeTableGridOptions<PaymentApi.PaymentPlan>,
});

// ---------------- 回款记录 ----------------
const [PaymentGrid, paymentGridApi] = useVbenVxeGrid({
  formOptions: { schema: usePaymentGridFormSchema(), submitOnChange: true },
  gridOptions: {
    columns: usePaymentColumns(onPaymentActionClick),
    height: 'auto',
    keepSource: true,
    proxyConfig: {
      ajax: {
        query: async ({ page }, formValues) => {
          return await getPaymentList({
            orderId: formValues?.orderId,
            pageNum: page.currentPage,
            pageSize: page.pageSize,
            writeoffStatus: formValues?.writeoffStatus,
          });
        },
      },
    },
    rowConfig: { keyField: 'id' },
    toolbarConfig: { custom: true, export: false, refresh: true, search: true, zoom: true },
  } as VxeTableGridOptions<PaymentApi.Payment>,
});

function onPlanActionClick(e: OnActionClickParams<PaymentApi.PaymentPlan>) {
  switch (e.code) {
    case 'edit': {
      planFormApi.setData(e.row).open();
      break;
    }
    case 'delete': {
      onDeletePlan(e.row);
      break;
    }
  }
}

async function onDeletePlan(row: PaymentApi.PaymentPlan) {
  await ElMessageBox.confirm(`确认删除第 ${row.periodNo} 期回款计划吗？`, '删除计划', {
    type: 'warning',
  });
  await deletePaymentPlan(row.id);
  ElMessage.success('已删除');
  planGridApi.query();
}

function onPaymentActionClick(e: OnActionClickParams<PaymentApi.Payment>) {
  switch (e.code) {
    case 'edit': {
      paymentFormApi.setData(e.row).open();
      break;
    }
    case 'delete': {
      onDeletePayment(e.row);
      break;
    }
    case 'writeoff': {
      onWriteoff(e.row);
      break;
    }
  }
}

async function onDeletePayment(row: PaymentApi.Payment) {
  await ElMessageBox.confirm('确认删除该回款记录吗？', '删除回款', {
    type: 'warning',
  });
  await deletePayment(row.id);
  ElMessage.success('已删除');
  paymentGridApi.query();
}

/** 核销：输入目标回款计划 ID */
async function onWriteoff(row: PaymentApi.Payment) {
  const { value } = await ElMessageBox.prompt(
    '请输入要核销到的回款计划 ID',
    '回款核销',
    {
      inputPattern: /^\d+$/,
      inputErrorMessage: '计划 ID 必须为数字',
      inputValue: row.planId ? String(row.planId) : '',
    },
  );
  await writeoffPayment(row.id, Number(value));
  ElMessage.success('核销成功');
  paymentGridApi.query();
  planGridApi.query();
}

function onCreatePlan() {
  planFormApi.setData({}).open();
}

function onCreatePayment() {
  paymentFormApi.setData({}).open();
}
</script>

<template>
  <Page auto-content-height>
    <PlanFormComp @success="planGridApi.query()" />
    <PaymentFormComp @success="paymentGridApi.query()" />
    <ElTabs v-model="activeTab" class="h-full">
      <ElTabPane label="回款计划" name="plan">
        <PlanGrid table-title="回款计划">
          <template #toolbar-tools>
            <ElButton
              v-access:code="PAYMENT_PLAN_PERMISSIONS.create"
              type="primary"
              @click="onCreatePlan"
            >
              <Plus class="size-5" />
              新增计划
            </ElButton>
          </template>
        </PlanGrid>
      </ElTabPane>
      <ElTabPane label="回款记录" name="payment">
        <PaymentGrid table-title="回款记录">
          <template #toolbar-tools>
            <ElButton
              v-access:code="PAYMENT_PERMISSIONS.create"
              type="primary"
              @click="onCreatePayment"
            >
              <Plus class="size-5" />
              登记回款
            </ElButton>
          </template>
        </PaymentGrid>
      </ElTabPane>
    </ElTabs>
  </Page>
</template>
