<script lang="ts" setup>
import type {
  OnActionClickParams,
  VxeTableGridOptions,
} from '#/adapter/vxe-table';
import type { KnowledgeApi } from '#/api/crm/support';

import { Page, useVbenDrawer } from '@vben/common-ui';
import { Plus } from '@vben/icons';

import { ElButton, ElMessage, ElMessageBox } from 'element-plus';

import { useVbenVxeGrid } from '#/adapter/vxe-table';
import {
  deleteKnowledge,
  getKnowledgeList,
  SUPPORT_PERMISSIONS,
} from '#/api/crm/support';

import { useColumns, useGridFormSchema } from './data';
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
          return await getKnowledgeList({
            category: formValues?.category,
            keyword: formValues?.keyword,
            pageNum: page.currentPage,
            pageSize: page.pageSize,
            publishStatus: formValues?.publishStatus,
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
  } as VxeTableGridOptions<KnowledgeApi.Knowledge>,
});

function onActionClick(e: OnActionClickParams<KnowledgeApi.Knowledge>) {
  switch (e.code) {
    case 'delete': {
      onDelete(e.row);
      break;
    }
    case 'detail': {
      detailDrawerApi.setData(e.row).open();
      break;
    }
    case 'edit': {
      formDrawerApi.setData(e.row).open();
      break;
    }
  }
}

async function onDelete(row: KnowledgeApi.Knowledge) {
  await ElMessageBox.confirm(
    `确认删除知识「${row.title}」吗？`,
    '删除知识条目',
    { type: 'warning' },
  );
  await deleteKnowledge(row.id);
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
    <DetailDrawerComp @success="gridApi.query()" />
    <Grid table-title="知识库">
      <template #toolbar-tools>
        <ElButton
          v-access:code="SUPPORT_PERMISSIONS.knowledgeCreate"
          type="primary"
          @click="onCreate"
        >
          <Plus class="size-5" />
          新增知识
        </ElButton>
      </template>
    </Grid>
  </Page>
</template>
