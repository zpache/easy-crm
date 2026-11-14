package cn.easykits.crm.modules.follow.constant;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 跟进类型字典（follow_type）。
 * <p>TODO（后续轮次）：迁入字典表（sys_dict），常量类为过渡期取值来源；前端下拉共用。
 */
public final class FollowTypes {

    public static final String PHONE = "PHONE";
    public static final String VISIT = "VISIT";
    public static final String EMAIL = "EMAIL";
    public static final String WECHAT = "WECHAT";
    public static final String OTHER = "OTHER";

    private static final Map<String, String> LABELS = new LinkedHashMap<>();

    static {
        LABELS.put(PHONE, "电话");
        LABELS.put(VISIT, "拜访");
        LABELS.put(EMAIL, "邮件");
        LABELS.put(WECHAT, "微信");
        LABELS.put(OTHER, "其他");
    }

    private FollowTypes() {
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
