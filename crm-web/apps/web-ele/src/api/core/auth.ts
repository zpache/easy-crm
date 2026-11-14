import { useAccessStore } from '@vben/stores';

import { baseRequestClient, requestClient } from '#/api/request';

export namespace AuthApi {
  /** 登录接口参数 */
  export interface LoginParams {
    password?: string;
    username?: string;
  }

  /** 登录接口返回值：accessToken 与 refreshToken 由后端分别签发 */
  export interface LoginResult {
    accessToken: string;
    refreshToken?: string;
  }

  /** baseRequestClient 返回原始响应体（Result<String>），data 即新的 accessToken */
  export interface RefreshTokenResult {
    data: string;
    status: number;
  }
}

/**
 * 登录
 */
export async function loginApi(data: AuthApi.LoginParams) {
  return requestClient.post<AuthApi.LoginResult>('/auth/login', data);
}

/**
 * 刷新 accessToken：携带 refreshToken 调用白名单端点，不需要登录态
 */
export async function refreshTokenApi() {
  const accessStore = useAccessStore() as unknown as { refreshToken?: string };
  return baseRequestClient.post<AuthApi.RefreshTokenResult>('/auth/refresh', {
    refreshToken: accessStore.refreshToken,
  });
}

/**
 * 退出登录：需要带当前令牌，后端递增 tokenVersion 使旧令牌失效
 */
export async function logoutApi() {
  return requestClient.post('/auth/logout');
}

/**
 * 获取用户权限码（后端 /user/codes，包含已授权的菜单与按钮 auth_code）
 */
export async function getAccessCodesApi() {
  return requestClient.get<string[]>('/user/codes');
}
