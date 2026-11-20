<script setup lang="ts">
import type { VbenFormSchema } from '#/adapter/form';

import { computed } from 'vue';
import { useRouter } from 'vue-router';

import { LOGIN_PATH } from '@vben/constants';
import { resetAllStores, useAccessStore } from '@vben/stores';

import { ProfilePasswordSetting, z } from '@vben/common-ui';

import { ElMessage } from 'element-plus';

import { changePasswordApi } from '#/api/core/user';

const formSchema = computed((): VbenFormSchema[] => {
  return [
    {
      fieldName: 'oldPassword',
      label: '旧密码',
      component: 'VbenInputPassword',
      componentProps: {
        placeholder: '请输入旧密码',
      },
    },
    {
      fieldName: 'newPassword',
      label: '新密码',
      component: 'VbenInputPassword',
      componentProps: {
        passwordStrength: true,
        placeholder: '请输入新密码',
      },
    },
    {
      fieldName: 'confirmPassword',
      label: '确认密码',
      component: 'VbenInputPassword',
      componentProps: {
        passwordStrength: true,
        placeholder: '请再次输入新密码',
      },
      dependencies: {
        rules(values) {
          const { newPassword } = values;
          return z
            .string({ required_error: '请再次输入新密码' })
            .min(1, { message: '请再次输入新密码' })
            .refine((value) => value === newPassword, {
              message: '两次输入的密码不一致',
            });
        },
        triggerFields: ['newPassword'],
      },
    },
  ];
});

const router = useRouter();

async function handleSubmit(values: Record<string, unknown>) {
  await changePasswordApi({
    confirmPassword: String(values.confirmPassword ?? ''),
    newPassword: String(values.newPassword ?? ''),
    oldPassword: String(values.oldPassword ?? ''),
  });
  ElMessage.success('密码修改成功，请使用新密码重新登录');
  // 后端改密后 tokenVersion 已递增，旧令牌全部失效；
  // 这里不再调后端 /auth/logout（旧令牌已无效，避免 401 级联），
  // 直接清理本地登录态并回到登录页
  const accessStore = useAccessStore();
  accessStore.setAccessToken(null);
  resetAllStores();
  accessStore.setLoginExpired(false);
  await router.replace({ path: LOGIN_PATH });
}
</script>
<template>
  <ProfilePasswordSetting
    class="w-1/3"
    :form-schema="formSchema"
    @submit="handleSubmit"
  />
</template>
