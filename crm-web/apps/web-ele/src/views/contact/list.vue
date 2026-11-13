<script lang="ts" setup>
import type {
  OnActionClickParams,
  VxeTableGridOptions,
} from '#/adapter/vxe-table';
import type { ContactApi } from '#/api/crm/contact';

import { onMounted } from 'vue';

import { Page, useVbenDrawer } from '@vben/common-ui';
import { Plus } from '@vben/icons';

import { ElButton, ElMessage, ElMessageBox } from 'element-plus';

import { useVbenVxeGrid } from '#/adapter/vxe-table';
import {
  deleteContact,
  getContactList,
  setContactPrimary,
} from '#/api/crm/contact';
import { CONTACT_PERMISSIONS } from '#/constants/permissions';

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
          return await getContactList({
            customerId: formValues?.customerId,
            name: formValues?.name,
            pageNum: page.currentPage,
            pageSize: page.pageSize,
            phone: formValues?.phone,
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
  } as VxeTableGridOptions<ContactApi.Contact>,
});

onMounted(async () => {
  // 客户下拉选项（查询表单 / 新增抽屉共用），加载后组件 props 通过函数自动刷新
  await loadCustomerOptions();
});

function onActionClick(e: OnActionClickParams<ContactApi.Contact>) {
  switch (e.code) {
    case 'delete': {
      onDelete(e.row);
      break;
    }
    case 'edit': {
      formDrawerApi.setData(e.row).open();
      break;
    }
    case 'setPrimary': {
      onSetPrimary(e.row);
      break;
    }
  }
}

async function onDelete(row: ContactApi.Contact) {
  await ElMessageBox.confirm(
    `确认删除联系人「${row.name}」吗？`,
    '删除联系人',
    { type: 'warning' },
  );
  await deleteContact(row.id);
  ElMessage.success('已删除');
  gridApi.query();
}

async function onSetPrimary(row: ContactApi.Contact) {
  await setContactPrimary(row.id);
  ElMessage.success(`「${row.name}」已设为主联系人，同客户其他主联系人已取消`);
  gridApi.query();
}

function onCreate() {
  formDrawerApi.setData({}).open();
}
</script>

<template>
  <Page auto-content-height>
    <FormDrawerComp @success="gridApi.query()" />
    <Grid table-title="联系人列表">
      <template #toolbar-tools>
        <ElButton
          v-access:code="CONTACT_PERMISSIONS.create"
          type="primary"
          @click="onCreate"
        >
          <Plus class="size-5" />
          新增联系人
        </ElButton>
      </template>
    </Grid>
  </Page>
</template>
