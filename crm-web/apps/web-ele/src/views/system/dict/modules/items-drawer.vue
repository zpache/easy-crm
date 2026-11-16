<script lang="ts" setup>
import type {
  OnActionClickParams,
  VxeTableGridOptions,
} from '#/adapter/vxe-table';
import type { SystemDictApi } from '#/api/system/dict';

import { computed, ref } from 'vue';

import { useVbenDrawer } from '@vben/common-ui';
import { Plus } from '@vben/icons';

import { ElButton, ElMessage, ElMessageBox } from 'element-plus';

import { useVbenVxeGrid } from '#/adapter/vxe-table';
import {
  deleteDictItem,
  listDictItems,
} from '#/api/system/dict';
import { SYSTEM_PERMISSIONS } from '#/constants/permissions';
import { $t } from '#/locales';

import { useItemColumns } from '../data';
import ItemForm from './item-form.vue';

const emit = defineEmits<{ success: [] }>();

const currentDict = ref<null | SystemDictApi.Dict>(null);

const getTitle = computed(() =>
  currentDict.value ? `字典项 - ${currentDict.value.name}` : '字典项',
);

async function queryItems() {
  const dict = currentDict.value;
  if (!dict?.id) {
    return { items: [], total: 0 };
  }
  const items = await listDictItems(dict.id);
  return { items, total: items.length };
}

const [Grid, gridApi] = useVbenVxeGrid({
  gridOptions: {
    columns: useItemColumns(onItemActionClick),
    height: 420,
    keepSource: true,
    pagerConfig: { enabled: false },
    proxyConfig: {
      ajax: {
        query: queryItems,
      },
    },
    rowConfig: { keyField: 'id' },
    toolbarConfig: {
      custom: false,
      export: false,
      refresh: true,
      zoom: false,
    },
  } as VxeTableGridOptions<SystemDictApi.DictItem>,
});

const [ItemModal, itemModalApi] = useVbenDrawer({
  connectedComponent: ItemForm,
  destroyOnClose: true,
});

function refresh() {
  gridApi.query();
}

function onCreateItem() {
  itemModalApi.setData(currentDict.value).open();
}

function onEditItem(row: SystemDictApi.DictItem) {
  itemModalApi.setData(row).open();
}

async function onDeleteItem(row: SystemDictApi.DictItem) {
  await ElMessageBox.confirm(
    `确认删除字典项「${row.label}」？`,
    $t('ui.actionTitle.delete'),
    { type: 'warning' },
  );
  await deleteDictItem(row.id as number);
  ElMessage.success($t('ui.actionMessage.deleteSuccess', [row.label]));
  refresh();
  emit('success');
}

function onItemActionClick({
  code,
  row,
}: OnActionClickParams<SystemDictApi.DictItem>) {
  switch (code) {
    case 'delete': {
      onDeleteItem(row).catch(() => {});
      break;
    }
    case 'edit': {
      onEditItem(row);
      break;
    }
  }
}

const [Drawer, drawerApi] = useVbenDrawer({
  onOpenChange(isOpen) {
    if (isOpen) {
      currentDict.value =
        drawerApi.getData<SystemDictApi.Dict | null>() ?? null;
      refresh();
    }
  },
});
</script>

<template>
  <Drawer class="w-[760px]" :title="getTitle">
    <Grid table-title=" ">
      <template #toolbar-tools>
        <ElButton
          v-access:code="SYSTEM_PERMISSIONS.dictCreate"
          type="primary"
          @click="onCreateItem"
        >
          <Plus class="size-5" />
          新增字典项
        </ElButton>
      </template>
    </Grid>
    <ItemModal @success="refresh" />
  </Drawer>
</template>
