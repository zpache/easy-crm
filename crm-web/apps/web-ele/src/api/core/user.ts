import type { UserInfo } from '@vben/types';

import { requestClient } from '#/api/request';

/**
 * 获取用户信息
 */
export async function getUserInfoApi() {
  return requestClient.get<UserInfo>('/user/info');
}

export interface ChangePasswordParams {
  confirmPassword: string;
  newPassword: string;
  oldPassword: string;
}

/**
 * 修改当前登录用户密码
 * 成功后后端 tokenVersion 递增，当前会话的旧令牌立即失效
 */
export async function changePasswordApi(data: ChangePasswordParams) {
  return requestClient.put<void>('/user/password', data);
}
