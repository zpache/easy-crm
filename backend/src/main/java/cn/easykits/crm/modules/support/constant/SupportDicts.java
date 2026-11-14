package cn.easykits.crm.modules.support.constant;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 客户服务域字典常量（工单类型 / 优先级 / 来源 / 知识库发布状态）与 SLA 默认值。
 * <p>TODO（后续轮次）：迁入字典表（sys_dict），常量类为过渡期取值来源；前端下拉共用。
 */
public final class SupportDicts {

    /** 工单类型 */
    public static final Map<String, String> TYPE = new LinkedHashMap<>();

    /** 优先级 */
    public static final Map<String, String> PRIORITY = new LinkedHashMap<>();

    /** 来源 */
    public static final Map<String, String> SOURCE = new LinkedHashMap<>();

    /** 知识库发布状态 */
    public static final Map<String, String> KNOWLEDGE_STATUS = new LinkedHashMap<>();

    static {
        TYPE.put("CONSULT", "咨询");
        TYPE.put("COMPLAINT", "投诉");
        TYPE.put("REPAIR", "报修");
        TYPE.put("RETURN", "退换");
        TYPE.put("OTHER", "其他");

        PRIORITY.put("LOW", "低");
        PRIORITY.put("MEDIUM", "中");
        PRIORITY.put("HIGH", "高");
        PRIORITY.put("URGENT", "紧急");

        SOURCE.put("PORTAL", "门户");
        SOURCE.put("PHONE", "电话");
        SOURCE.put("EMAIL", "邮件");
        SOURCE.put("WECHAT", "微信");
        SOURCE.put("MANUAL", "手动录入");
        SOURCE.put("OTHER", "其他");

        KNOWLEDGE_STATUS.put("DRAFT", "草稿");
        KNOWLEDGE_STATUS.put("PUBLISHED", "已发布");
        KNOWLEDGE_STATUS.put("OFFLINE", "已下线");
    }

    /** 未匹配到启用 SLA 策略时的兜底时限（小时）。 */
    public static final int DEFAULT_RESPONSE_HOURS = 24;
    public static final int DEFAULT_RESOLVE_HOURS = 72;

    /** SLA 超时提醒写入 crm_reminder 的业务类型。 */
    public static final String BIZ_TYPE_SLA_OVERDUE = "service_ticket_sla_overdue";

    /** SLA 超时提醒等级（橙）。 */
    public static final int SLA_OVERDUE_REMIND_LEVEL = 2;

    private SupportDicts() {
    }

    public static String typeLabel(String code) {
        return label(TYPE, code);
    }

    public static String priorityLabel(String code) {
        return label(PRIORITY, code);
    }

    public static String sourceLabel(String code) {
        return label(SOURCE, code);
    }

    public static String knowledgeStatusLabel(String code) {
        return label(KNOWLEDGE_STATUS, code);
    }

    public static boolean isValidType(String code) {
        return code != null && TYPE.containsKey(code);
    }

    public static boolean isValidPriority(String code) {
        return code != null && PRIORITY.containsKey(code);
    }

    public static boolean isValidSource(String code) {
        return code != null && SOURCE.containsKey(code);
    }

    public static boolean isValidKnowledgeStatus(String code) {
        return code != null && KNOWLEDGE_STATUS.containsKey(code);
    }

    private static String label(Map<String, String> dict, String code) {
        return code == null ? null : dict.get(code);
    }
}
