<script lang="ts" setup>
import type {
  OnActionClickParams,
  VxeTableGridOptions,
} from '#/adapter/vxe-table';
import type { SystemDictApi } from '#/api/system/dict';

import { Page, useVbenModal } from '@vben/common-ui';
import { Plus } from '@vben/icons';

import { ElButton, ElMessage, ElMessageBox } from 'element-plus';

import { useVbenVxeGrid } from '#/adapter/vxe-table';
import { deleteDict, pageDicts } from '#/api/system/dict';
import { SYSTEM_PERMISSIONS } from '#/constants/permissions';
import { $t } from '#/locales';

import { useColumns, useGridFormSchema } from './data';
import Form from './modules/form.vue';
import ItemsDrawer from './modules/items-drawer.vue';

const [FormModal, formModalApi] = useVbenModal({
  connectedComponent: Form,
  destroyOnClose: true,
});

const [ItemsDrawerComp, itemsDrawerApi] = useVbenModal({
  connectedComponent: ItemsDrawer,
  destroyOnClose: true,
});

function onCreate() {
  formModalApi.setData(null).open();
}

function onEdit(row: SystemDictApi.Dict) {
  formModalApi.setData(row).open();
}

function onItems(row: SystemDictApi.Dict) {
  itemsDrawerApi.setData(row).open();
}

async function onDelete(row: SystemDictApi.Dict) {
  await ElMessageBox.confirm(
    `确认删除字典「${row.name}」及其全部字典项？`,
    $t('ui.actionTitle.delete'),
    { type: 'warning' },
  );
  await deleteDict(row.id);
  ElMessage.success($t('ui.actionMessage.deleteSuccess', [row.name]));
  refreshGrid();
}

function onActionClick({ code, row }: OnActionClickParams<SystemDictApi.Dict>) {
  switch (code) {
    case 'delete': {
      onDelete(row).catch(() => {});
      break;
    }
    case 'edit': {
      onEdit(row);
      break;
    }
    case 'items': {
      onItems(row);
      break;
    }
  }
}

const [Grid, gridApi] = useVbenVxeGrid({
  gridOptions: {
    columns: useColumns(onActionClick),
    formOptions: {
      collapsed: false,
      schema: useGridFormSchema(),
      submitOnChange: true,
      showCollapseButton: false,
    },
    height: 'auto',
    keepSource: true,
    pagerConfig: {
      pageSize: 20,
    },
    proxyConfig: {
      ajax: {
        query: async ({ page }, formValues = {}) => {
          return await pageDicts({
            keyword: formValues?.keyword,
            pageNum: page.currentPage,
            pageSize: page.pageSize,
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
  } as VxeTableGridOptions<SystemDictApi.Dict>,
});

function refreshGrid() {
  gridApi.query();
}
</script>

<template>
  <Page auto-content-height>
    <FormModal @success="refreshGrid" />
    <ItemsDrawerComp />
    <Grid table-title="数据字典">
      <template #toolbar-tools>
        <ElButton
          v-access:code="SYSTEM_PERMISSIONS.dictCreate"
          type="primary"
          @click="onCreate"
        >
          <Plus class="size-5" />
          {{ $t('ui.actionTitle.create', ['字典']) }}
        </ElButton>
      </template>
    </Grid>
  </Page>
</template>
