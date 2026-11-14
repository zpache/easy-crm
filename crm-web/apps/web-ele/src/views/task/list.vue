<script lang="ts" setup>
import type {
  OnActionClickParams,
  VxeTableGridOptions,
} from '#/adapter/vxe-table';
import type { TaskApi } from '#/api/crm/task';

import { ref } from 'vue';

import { Page, useVbenDrawer } from '@vben/common-ui';
import { Plus } from '@vben/icons';

import { ElButton, ElMessage, ElMessageBox, ElRadioGroup, ElRadioButton } from 'element-plus';

import { useVbenVxeGrid } from '#/adapter/vxe-table';
import { completeTask, getTaskList } from '#/api/crm/task';
import { TASK_PERMISSIONS } from '#/constants/permissions';

import { useColumns, useGridFormSchema } from './data';
import FormDrawer from './modules/form.vue';

/** 视图切换：我的待办 / 全部任务 */
const viewMode = ref<'all' | 'todo'>('all');

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
          const range = formValues?.dueTimeRange as [string, string] | undefined;
          const params: Recordable<any> = {
            dueTimeEnd: range?.[1],
            dueTimeStart: range?.[0],
            pageNum: page.currentPage,
            pageSize: page.pageSize,
            status: formValues?.status,
          };
          // 我的待办视图：走 /tasks/todo，不传筛选条件
          if (viewMode.value === 'todo') {
            return await (await import('#/api/crm/task')).getMyTodo({
              pageNum: page.currentPage,
              pageSize: page.pageSize,
            });
          }
          return await getTaskList(params);
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
  } as VxeTableGridOptions<TaskApi.Task>,
});

function onViewModeChange(mode: 'all' | 'todo') {
  viewMode.value = mode;
  gridApi.query();
}

function onActionClick(e: OnActionClickParams<TaskApi.Task>) {
  switch (e.code) {
    case 'edit': {
      formDrawerApi.setData(e.row).open();
      break;
    }
    case 'complete': {
      onComplete(e.row);
      break;
    }
  }
}

async function onComplete(row: TaskApi.Task) {
  await completeTask(row.id);
  ElMessage.success(`「${row.title}」已完成`);
  gridApi.query();
}

function onCreate() {
  formDrawerApi.setData({}).open();
}
</script>

<template>
  <Page auto-content-height>
    <FormDrawerComp @success="gridApi.query()" />
    <Grid table-title="待办任务">
      <template #toolbar-tools>
        <ElRadioGroup
          class="mr-2"
          :model-value="viewMode"
          size="small"
          @update:model-value="onViewModeChange($event as any)"
        >
          <ElRadioButton value="all">全部任务</ElRadioButton>
          <ElRadioButton value="todo">我的待办</ElRadioButton>
        </ElRadioGroup>
        <ElButton
          v-access:code="TASK_PERMISSIONS.create"
          type="primary"
          @click="onCreate"
        >
          <Plus class="size-5" />
          新增任务
        </ElButton>
      </template>
    </Grid>
  </Page>
</template>
