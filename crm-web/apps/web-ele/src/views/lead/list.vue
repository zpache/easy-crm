<script lang="ts" setup>
import type {
  OnActionClickParams,
  VxeTableGridOptions,
} from '#/adapter/vxe-table';
import type { LeadApi } from '#/api/crm/lead';

import { ref } from 'vue';

import { Page, useVbenDrawer } from '@vben/common-ui';
import { Download, Plus } from '@vben/icons';

import { ElButton, ElMessage, ElMessageBox } from 'element-plus';

import { useVbenVxeGrid } from '#/adapter/vxe-table';
import { assignLead, claimLead, getLeadList, toPool } from '#/api/crm/lead';
import AssignUserDialog from '#/components/assign-user-dialog.vue';
import { CRM_PERMISSIONS } from '#/constants/permissions';

import { useColumns, useGridFormSchema } from './data';
import ConvertDrawer from './modules/convert.vue';
import FormDrawer from './modules/form.vue';
import ImportDrawer from './modules/import.vue';

const [FormDrawerComp, formDrawerApi] = useVbenDrawer({
  connectedComponent: FormDrawer,
  destroyOnClose: true,
});

const [ConvertDrawerComp, convertDrawerApi] = useVbenDrawer({
  connectedComponent: ConvertDrawer,
  destroyOnClose: true,
});

const [ImportDrawerComp, importDrawerApi] = useVbenDrawer({
  connectedComponent: ImportDrawer,
  destroyOnClose: true,
});

const [Grid, gridApi] = useVbenVxeGrid({
  formOptions: {
    schema: useGridFormSchema(),
    submitOnChange: true,
  },
  gridOptions: {
    columns: useColumns(onActionClick),
    checkboxConfig: { highlight: true, reserve: true },
    height: 'auto',
    keepSource: true,
    proxyConfig: {
      ajax: {
        query: async ({ page }, formValues) => {
          return await getLeadList({
            name: formValues?.name,
            ownerId: formValues?.ownerId,
            pageNum: page.currentPage,
            pageSize: page.pageSize,
            phone: formValues?.phone,
            source: formValues?.source,
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
  } as VxeTableGridOptions<LeadApi.Lead>,
});

/** 分配负责人弹窗状态（单个分配；批量分配可复用同一弹窗对选中行逐个调用） */
const assignOpen = ref(false);
const assignTargetId = ref<null | number>(null);

function openAssign(row: LeadApi.Lead) {
  assignTargetId.value = row.id;
  assignOpen.value = true;
}

async function handleAssign(ownerId: number) {
  if (assignTargetId.value == null) {
    return;
  }
  await assignLead(assignTargetId.value, ownerId);
  ElMessage.success('分配成功');
  gridApi.query();
}

/** 批量放入公海：复用现有 toPool，对勾选行逐个调用 */
async function onBatchToPool() {
  const rows = gridApi.grid.getCheckboxRecords() as LeadApi.Lead[];
  if (rows.length === 0) {
    ElMessage.warning('请先勾选要放入公海的线索');
    return;
  }
  await ElMessageBox.confirm(
    `确认将选中的 ${rows.length} 条线索放入公海吗？负责人将被清空。`,
    '批量放入公海',
    { type: 'warning' },
  );
  for (const row of rows) {
    await toPool(row.id);
  }
  ElMessage.success('已批量放入公海');
  gridApi.query();
}

function onActionClick(e: OnActionClickParams<LeadApi.Lead>) {
  switch (e.code) {
    case 'assign': {
      openAssign(e.row);
      break;
    }
    case 'claim': {
      onClaim(e.row);
      break;
    }
    case 'convert': {
      convertDrawerApi.setData(e.row).open();
      break;
    }
    case 'edit': {
      formDrawerApi.setData(e.row).open();
      break;
    }
    case 'toPool': {
      onToPool(e.row);
      break;
    }
  }
}

async function onClaim(row: LeadApi.Lead) {
  await claimLead(row.id);
  ElMessage.success(`已领取「${row.name}」`);
  gridApi.query();
}

async function onToPool(row: LeadApi.Lead) {
  await ElMessageBox.confirm(
    `确认将「${row.name}」放入公海吗？负责人将被清空。`,
    '放入公海',
    { type: 'warning' },
  );
  await toPool(row.id);
  ElMessage.success('已放入公海');
  gridApi.query();
}

function onConverted() {
  ElMessage.success('转化成功');
  gridApi.query();
}

function onCreate() {
  formDrawerApi.setData({}).open();
}

function onImport() {
  importDrawerApi.setData({}).open();
}
</script>

<template>
  <Page auto-content-height>
    <FormDrawerComp @success="gridApi.query()" />
    <ConvertDrawerComp @success="onConverted" />
    <ImportDrawerComp @success="gridApi.query()" />
    <AssignUserDialog v-model="assignOpen" @confirm="handleAssign" />
    <Grid table-title="线索列表">
      <template #toolbar-tools>
        <ElButton
          v-access:code="CRM_PERMISSIONS.leadToPool"
          class="mr-2"
          plain
          type="warning"
          @click="onBatchToPool"
        >
          批量放入公海
        </ElButton>
        <ElButton
          v-access:code="CRM_PERMISSIONS.leadImport"
          type="default"
          @click="onImport"
        >
          <Download class="size-5" />
          Excel 导入
        </ElButton>
        <ElButton
          v-access:code="CRM_PERMISSIONS.leadCreate"
          type="primary"
          @click="onCreate"
        >
          <Plus class="size-5" />
          新增线索
        </ElButton>
      </template>
    </Grid>
  </Page>
</template>
