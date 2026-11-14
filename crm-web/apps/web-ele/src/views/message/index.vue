<script lang="ts" setup>
import type { VxeTableGridOptions } from '#/adapter/vxe-table';

import { Page } from '@vben/common-ui';

import { ElButton, ElMessage } from 'element-plus';

import { useVbenVxeGrid } from '#/adapter/vxe-table';
import { getReminders, readAllReminders } from '#/api/crm/reminder';

import { REMINDER_PERMISSIONS, useColumns, useGridFormSchema } from './data';

defineOptions({ name: 'MessageCenter' });

const [Grid, gridApi] = useVbenVxeGrid({
  formOptions: {
    schema: useGridFormSchema(),
    submitOnChange: true,
  },
  gridOptions: {
    columns: useColumns(),
    height: 'auto',
    keepSource: true,
    proxyConfig: {
      ajax: {
        query: async ({ page }, formValues) => {
          return await getReminders({
            onlyUnread: formValues?.onlyUnread,
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
  } as VxeTableGridOptions,
});

/** 全部已读：作用于当前登录用户的未读提醒 */
async function handleReadAll() {
  await readAllReminders();
  ElMessage.success('已全部标记为已读');
  gridApi.query();
}
</script>

<template>
  <Page auto-content-height>
    <Grid table-title="消息提醒">
      <template #toolbar-tools>
        <ElButton
          v-access:code="REMINDER_PERMISSIONS.read"
          class="mr-2"
          plain
          type="primary"
          @click="handleReadAll"
        >
          全部已读
        </ElButton>
      </template>
    </Grid>
  </Page>
</template>
