package cn.easykits.crm.modules.order.constant;

/**
 * 订单状态机：
 * <pre>
 *   0 草稿 -> 1 待确认 -> 2 已确认 -> 3 已交付 -> 4 已完成
 *   任意非终态（0/1/2/3）可 -> 9 已取消
 *   终态：4 已完成 / 9 已取消，不可再变更
 * </pre>
 */
public final class OrderStatus {

    public static final int DRAFT = 0;
    public static final int PENDING = 1;
    public static final int CONFIRMED = 2;
    public static final int DELIVERED = 3;
    public static final int COMPLETED = 4;
    public static final int CANCELLED = 9;

    private OrderStatus() {
    }

    public static boolean isTerminal(int status) {
        return status == COMPLETED || status == CANCELLED;
    }

    public static boolean isValid(int status) {
        return status == DRAFT || status == PENDING || status == CONFIRMED
                || status == DELIVERED || status == COMPLETED || status == CANCELLED;
    }

    /** 是否允许从 from 流转到 to。 */
    public static boolean canTransition(int from, int to) {
        if (isTerminal(from) || !isValid(to)) {
            return false;
        }
        if (to == CANCELLED) {
            return true;
        }
        return switch (from) {
            case DRAFT -> to == PENDING;
            case PENDING -> to == CONFIRMED;
            case CONFIRMED -> to == DELIVERED;
            case DELIVERED -> to == COMPLETED;
            default -> false;
        };
    }

    public static String name(Integer status) {
        if (status == null) {
            return null;
        }
        return switch (status) {
            case DRAFT -> "草稿";
            case PENDING -> "待确认";
            case CONFIRMED -> "已确认";
            case DELIVERED -> "已交付";
            case COMPLETED -> "已完成";
            case CANCELLED -> "已取消";
            default -> String.valueOf(status);
        };
    }
}
