package cn.easykits.crm.modules.system.constant;

import cn.easykits.crm.modules.system.vo.DictItemVO;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 内置字典默认值（与迁移 V20261116_01 的种子数据一致）。
 * <p>当字典表被清空 / 停用 / 未初始化时，读接口回退到这里，保证行为与
 * 常量类时代（LeadSource / CustomerDicts / FollowTypes / LostReasons / FollowBizTypes）一致；
 * 常量类保留为编译期来源，此处为运行期 fallback。
 */
public final class DictDefaults {

    private static final Map<String, List<DictItemVO>> BUILT_IN = new LinkedHashMap<>();

    static {
        BUILT_IN.put("lead_source", List.of(
                item("lead_source", "官网", "1", 10),
                item("lead_source", "电话", "2", 20),
                item("lead_source", "广告投放", "3", 30),
                item("lead_source", "客户转介绍", "4", 40),
                item("lead_source", "展会活动", "5", 50),
                item("lead_source", "其他", "6", 60)));

        BUILT_IN.put("lost_reason", List.of(
                item("lost_reason", "价格因素", "PRICE", 10),
                item("lost_reason", "功能不满足", "FEATURE", 20),
                item("lost_reason", "选择了竞争对手", "COMPETITOR", 30),
                item("lost_reason", "预算取消", "BUDGET", 40),
                item("lost_reason", "失去联系", "NO_CONTACT", 50),
                item("lost_reason", "其他", "OTHER", 60)));

        BUILT_IN.put("customer_industry", List.of(
                item("customer_industry", "制造业", "1", 10),
                item("customer_industry", "互联网", "2", 20),
                item("customer_industry", "金融", "3", 30),
                item("customer_industry", "零售消费", "4", 40),
                item("customer_industry", "医疗健康", "5", 50),
                item("customer_industry", "教育", "6", 60),
                item("customer_industry", "其他", "9", 70)));

        BUILT_IN.put("customer_scale", List.of(
                item("customer_scale", "1-50 人", "1", 10),
                item("customer_scale", "51-200 人", "2", 20),
                item("customer_scale", "201-1000 人", "3", 30),
                item("customer_scale", "1000 人以上", "4", 40)));

        BUILT_IN.put("customer_region", List.of(
                item("customer_region", "华北", "1", 10),
                item("customer_region", "华东", "2", 20),
                item("customer_region", "华南", "3", 30),
                item("customer_region", "西南", "4", 40),
                item("customer_region", "海外", "5", 50)));

        BUILT_IN.put("customer_level", List.of(
                item("customer_level", "A（战略）", "1", 10),
                item("customer_level", "B（重点）", "2", 20),
                item("customer_level", "C（普通）", "3", 30)));

        BUILT_IN.put("customer_status", List.of(
                item("customer_status", "正常", "1", 10),
                item("customer_status", "公海", "2", 20),
                item("customer_status", "已合并", "3", 30),
                item("customer_status", "成交客户", "4", 40)));

        BUILT_IN.put("follow_type", List.of(
                item("follow_type", "电话", "PHONE", 10),
                item("follow_type", "拜访", "VISIT", 20),
                item("follow_type", "邮件", "EMAIL", 30),
                item("follow_type", "微信", "WECHAT", 40),
                item("follow_type", "其他", "OTHER", 50)));

        BUILT_IN.put("follow_biz_type", List.of(
                item("follow_biz_type", "线索", "lead", 10),
                item("follow_biz_type", "客户", "customer", 20),
                item("follow_biz_type", "商机", "opportunity", 30),
                item("follow_biz_type", "订单", "order", 40)));
    }

    private DictDefaults() {
    }

    private static DictItemVO item(String dictType, String label, String value, int sortNo) {
        return new DictItemVO(null, dictType, label, value, sortNo, 1);
    }

    /** 是否为内置字典类型 */
    public static boolean isBuiltInType(String dictType) {
        return dictType != null && BUILT_IN.containsKey(dictType);
    }

    /** 内置字典默认字典项（只读，调用方不得修改） */
    public static List<DictItemVO> itemsOf(String dictType) {
        return dictType == null ? List.of() : BUILT_IN.getOrDefault(dictType, List.of());
    }

    /** 全部内置字典类型编码 */
    public static java.util.Set<String> types() {
        return java.util.Collections.unmodifiableSet(BUILT_IN.keySet());
    }
}
