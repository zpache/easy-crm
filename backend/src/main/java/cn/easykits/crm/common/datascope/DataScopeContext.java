package cn.easykits.crm.common.datascope;

import java.util.function.Supplier;

/**
 * 系统内部调用上下文。
 *
 * <p>行级数据权限采用 fail closed：任何没有登录上下文、却访问受控业务表的 SQL 都将被拒绝。
 * 定时任务、启动初始化、消息消费者等没有用户身份的线程，必须显式地用
 * {@link #runAsSystem(Runnable)} 或 {@link #callAsSystem(Supplier)} 包裹执行体，
 * 才能跨过数据权限过滤。豁免范围严格限定在当前线程与包裹的代码块内。
 */
public final class DataScopeContext {

    private static final ThreadLocal<Boolean> SYSTEM_CALL = new ThreadLocal<>();

    private DataScopeContext() {
    }

    public static boolean isSystemCall() {
        return Boolean.TRUE.equals(SYSTEM_CALL.get());
    }

    public static void runAsSystem(Runnable action) {
        boolean previous = isSystemCall();
        SYSTEM_CALL.set(Boolean.TRUE);
        try {
            action.run();
        } finally {
            if (!previous) {
                SYSTEM_CALL.remove();
            }
        }
    }

    public static <T> T callAsSystem(Supplier<T> action) {
        boolean previous = isSystemCall();
        SYSTEM_CALL.set(Boolean.TRUE);
        try {
            return action.get();
        } finally {
            if (!previous) {
                SYSTEM_CALL.remove();
            }
        }
    }
}
