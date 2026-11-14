package cn.easykits.crm.modules.auth.dto;

/** 刷新令牌请求：由前端在 access token 过期时携带 refreshToken 调用（无需登录态）。 */
public record RefreshRequest(String refreshToken) {
}
