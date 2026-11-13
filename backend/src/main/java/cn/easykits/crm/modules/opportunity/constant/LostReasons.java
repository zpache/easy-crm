package cn.easykits.crm.modules.opportunity.constant;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 丢单原因字典（lost_reason）。
 * <p>TODO（后续轮次）：迁入字典表；前端下拉共用。
 */
public final class LostReasons {

    public static final String PRICE = "PRICE";
    public static final String FEATURE = "FEATURE";
    public static final String COMPETITOR = "COMPETITOR";
    public static final String BUDGET = "BUDGET";
    public static final String NO_CONTACT = "NO_CONTACT";
    public static final String OTHER = "OTHER";

    private static final Map<String, String> LABELS = new LinkedHashMap<>();

    static {
        LABELS.put(PRICE, "价格因素");
        LABELS.put(FEATURE, "功能不满足");
        LABELS.put(COMPETITOR, "选择了竞争对手");
        LABELS.put(BUDGET, "预算取消");
        LABELS.put(NO_CONTACT, "失去联系");
        LABELS.put(OTHER, "其他");
    }

    private LostReasons() {
    }

    public static Map<String, String> all() {
        return Map.copyOf(LABELS);
    }

    public static String labelOf(String code) {
        return code == null ? null : LABELS.get(code);
    }

    public static boolean isValid(String code) {
        return code != null && LABELS.containsKey(code);
    }
}
