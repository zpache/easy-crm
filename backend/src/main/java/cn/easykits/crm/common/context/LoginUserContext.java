package cn.easykits.crm.common.context;

import cn.easykits.crm.common.api.ResultCode;
import cn.easykits.crm.common.exception.BizException;

/**
 * 当前请求的登录上下文（ThreadLocal 实现）。
 * <p>
 * 使用方在请求线程内通过 {@link #get()} 获取登录主体；
 * 认证过滤器在 finally 中调用 {@link #clear()}，避免线程复用导致的数据串号。
 * <p>
 * 行级数据权限拦截器从这里取 userId / deptId / dataScope 构造过滤条件。
 */
public final class LoginUserContext {

    private static final ThreadLocal<LoginUser> HOLDER = new ThreadLocal<>();

    private LoginUserContext() {
    }

    public static void set(LoginUser loginUser) {
        HOLDER.set(loginUser);
    }

    public static LoginUser get() {
        return HOLDER.get();
    }

    /**
     * 获取登录主体，未登录时抛出 UNAUTHORIZED 业务异常。
     */
    public static LoginUser require() {
        LoginUser loginUser = HOLDER.get();
        if (loginUser == null) {
            throw new BizException(ResultCode.UNAUTHORIZED);
        }
        return loginUser;
    }

    public static Long userId() {
        return require().userId();
    }

    public static void clear() {
        HOLDER.remove();
    }
}
