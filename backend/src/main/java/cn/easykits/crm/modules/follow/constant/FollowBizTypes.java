package cn.easykits.crm.modules.follow.constant;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 跟进 / 待办关联的业务类型字典（多态关联 biz_type 取值）。
 * <p>TODO（轮次 5 订单模块上线后）：新增 ORDER。
 */
public final class FollowBizTypes {

    public static final String LEAD = "lead";
    public static final String CUSTOMER = "customer";
    public static final String OPPORTUNITY = "opportunity";
    public static final String ORDER = "order";

    private static final Map<String, String> LABELS = new LinkedHashMap<>();

    static {
        LABELS.put(LEAD, "线索");
        LABELS.put(CUSTOMER, "客户");
        LABELS.put(OPPORTUNITY, "商机");
        LABELS.put(ORDER, "订单");
    }

    private FollowBizTypes() {
    }

    public static Map<String, String> all() {
        return Map.copyOf(LABELS);
    }

    public static String labelOf(String bizType) {
        return bizType == null ? null : LABELS.get(bizType);
    }

    public static boolean isValid(String bizType) {
        return bizType != null && LABELS.containsKey(bizType);
    }
}
