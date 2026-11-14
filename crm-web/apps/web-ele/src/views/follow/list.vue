<script lang="ts" setup>
import type {
  OnActionClickParams,
  VxeTableGridOptions,
} from '#/adapter/vxe-table';
import type { FollowApi } from '#/api/crm/follow';

import { ref } from 'vue';

import { Page, useVbenDrawer } from '@vben/common-ui';
import { Plus } from '@vben/icons';

import {
  ElButton,
  ElMessage,
  ElMessageBox,
  ElRadioButton,
  ElRadioGroup,
} from 'element-plus';

import { useVbenVxeGrid } from '#/adapter/vxe-table';
import {
  deleteFollowRecord,
  getFollowRecords,
  getTimeline,
} from '#/api/crm/follow';

import {
  FOLLOW_PERMISSIONS,
  useColumns,
  useGridFormSchema,
  useTimelineColumns,
  useTimelineFormSchema,
} from './data';
import FormDrawer from './modules/form.vue';

/** 视图切换：跟进记录列表 / 业务对象时间线 */
const viewMode = ref<'list' | 'timeline'>('list');

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
          const range = formValues?.createTimeRange as
            | [string, string]
            | undefined;
          return await getFollowRecords({
            bizId: formValues?.bizId,
            bizType: formValues?.bizType,
            endDate: range?.[1],
            pageNum: page.currentPage,
            pageSize: page.pageSize,
            startDate: range?.[0],
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
  } as VxeTableGridOptions<FollowApi.FollowRecord>,
});

/** 时间线：按 bizType + bizId 查询 GET /timeline（跟进 + 待办 + 阶段变更聚合） */
const [TimelineGrid] = useVbenVxeGrid({
  formOptions: {
    schema: useTimelineFormSchema(),
    submitOnChange: true,
  },
  gridOptions: {
    columns: useTimelineColumns(),
    height: 'auto',
    keepSource: true,
    proxyConfig: {
      ajax: {
        query: async ({ page }, formValues) => {
          const bizType = formValues?.bizType;
          const bizId = formValues?.bizId;
          if (!bizType || !bizId) {
            return { items: [], total: 0 };
          }
          return await getTimeline({
            bizId,
            bizType,
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
  } as VxeTableGridOptions<FollowApi.TimelineItem>,
});

function onActionClick(e: OnActionClickParams<FollowApi.FollowRecord>) {
  switch (e.code) {
    case 'edit': {
      formDrawerApi.setData(e.row).open();
      break;
    }
    case 'remove': {
      onRemove(e.row);
      break;
    }
  }
}

async function onRemove(row: FollowApi.FollowRecord) {
  await ElMessageBox.confirm(
    '确认删除这条跟进记录吗？删除后其自动生成的未完成待办也会一并取消。',
    '删除跟进记录',
    { type: 'warning' },
  );
  await deleteFollowRecord(row.id);
  ElMessage.success('已删除跟进记录');
  gridApi.query();
}

function onCreate() {
  formDrawerApi.setData({}).open();
}
</script>

<template>
  <Page auto-content-height>
    <FormDrawerComp @success="gridApi.query()" />
    <div class="mb-2">
      <ElRadioGroup
        :model-value="viewMode"
        size="small"
        @update:model-value="viewMode = $event as any"
      >
        <ElRadioButton value="list">跟进记录</ElRadioButton>
        <ElRadioButton value="timeline">时间线</ElRadioButton>
      </ElRadioGroup>
    </div>
    <Grid v-if="viewMode === 'list'" table-title="跟进记录">
      <template #toolbar-tools>
        <ElButton
          v-access:code="FOLLOW_PERMISSIONS.create"
          type="primary"
          @click="onCreate"
        >
          <Plus class="size-5" />
          记跟进
        </ElButton>
      </template>
    </Grid>
    <TimelineGrid v-else table-title="业务对象时间线" />
  </Page>
</template>
