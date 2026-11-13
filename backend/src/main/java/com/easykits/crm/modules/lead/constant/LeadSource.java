package com.easykits.crm.modules.lead.constant;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 线索来源字典值。
 * <p>TODO（后续轮次）：迁入字典表（sys_dict + sys_dict_item），本常量类作为过渡期的取值来源；
 * Excel 导入按中文标签匹配来源码。
 */
public final class LeadSource {

    public static final int WEBSITE = 1;
    public static final int PHONE = 2;
    public static final int AD = 3;
    public static final int REFERRAL = 4;
    public static final int EVENT = 5;
    public static final int OTHER = 6;

    /** code -> 中文名（保持插入顺序，前端下拉 / Excel 导入共用） */
    private static final Map<Integer, String> LABELS = new LinkedHashMap<>();

    static {
        LABELS.put(WEBSITE, "官网");
        LABELS.put(PHONE, "电话");
        LABELS.put(AD, "广告投放");
        LABELS.put(REFERRAL, "客户转介绍");
        LABELS.put(EVENT, "展会活动");
        LABELS.put(OTHER, "其他");
    }

    private LeadSource() {
    }

    public static Map<Integer, String> all() {
        return Map.copyOf(LABELS);
    }

    public static String labelOf(Integer code) {
        return code == null ? null : LABELS.get(code);
    }

    /**
     * 按中文标签找 code（Excel 导入用）；找不到返回 null（导入时报错，不静默归为「其他」）。
     */
    public static Integer codeOfLabel(String label) {
        if (label == null || label.isBlank()) {
            return null;
        }
        return LABELS.entrySet().stream()
                .filter(e -> e.getValue().equals(label.trim()))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(null);
    }

    /**
     * code 是否合法（创建/导入时校验）。
     */
    public static boolean isValid(Integer code) {
        return code == null || LABELS.containsKey(code);
    }
}
