<script lang="ts" setup>
import { ref, watch } from 'vue';

import { ElButton, ElDialog, ElMessage, ElOption, ElSelect } from 'element-plus';

import { getUserList } from '#/api/system/user';

/**
 * 分配负责人弹窗：复用 system/user 的 getUserList 加载启用用户，
 * 选中后把 ownerId 交给父页面调用对应 assign 接口。
 */
const visible = defineModel<boolean>({ default: false });
const emit = defineEmits<{ confirm: [number] }>();

interface UserOption {
  id: number;
  nickname: string;
  username: string;
}

const options = ref<UserOption[]>([]);
const selected = ref<number | undefined>(undefined);

watch(visible, async (isOpen) => {
  if (!isOpen) {
    return;
  }
  selected.value = undefined;
  options.value = [];
  try {
    const res = await getUserList({ pageSize: 100, status: 1 });
    options.value = (res.items ?? [])
      .filter((u: any) => u.status !== 0)
      .map((u: any) => ({
        id: u.id,
        nickname: u.nickname ?? u.username,
        username: u.username,
      }));
  } catch {
    options.value = [];
  }
});

function handleConfirm() {
  if (selected.value == null) {
    ElMessage.warning('请选择要分配的负责人');
    return;
  }
  emit('confirm', selected.value);
  visible.value = false;
}
</script>

<template>
  <ElDialog
    v-model="visible"
    :close-on-click-modal="false"
    title="分配负责人"
    width="420px"
  >
    <ElSelect
      v-model="selected"
      class="w-full"
      filterable
      placeholder="请选择负责人"
    >
      <ElOption
        v-for="u in options"
        :key="u.id"
        :label="`${u.nickname}（${u.username}）`"
        :value="u.id"
      />
    </ElSelect>
    <template #footer>
      <ElButton @click="visible = false">取消</ElButton>
      <ElButton type="primary" @click="handleConfirm">确定</ElButton>
    </template>
  </ElDialog>
</template>
