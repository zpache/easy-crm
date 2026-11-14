package cn.easykits.crm.modules.customer.constant;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 客户模块字典常量（行业 / 规模 / 区域 / 级别）。
 * <p>TODO（后续轮次）：迁入字典表（sys_dict），常量类作为过渡期取值来源；前端下拉共用。
 */
public final class CustomerDicts {

    /** 客户状态语义常量：与 STATUS 字典、DDL 注释、前端状态选项保持一致 */
    public static final int STATUS_NORMAL = 1;
    public static final int STATUS_POOL = 2;
    public static final int STATUS_MERGED = 3;
    /** 赢单后由 OpportunityServiceImpl.changeStage 回写的「成交客户」状态 */
    public static final int STATUS_DEAL = 4;

    /** 行业 */
    public static final Map<Integer, String> INDUSTRY = Map.of(
            1, "制造业",
            2, "互联网",
            3, "金融",
            4, "零售消费",
            5, "医疗健康",
            6, "教育",
            9, "其他");

    /** 规模 */
    public static final Map<Integer, String> SCALE = Map.of(
            1, "1-50 人",
            2, "51-200 人",
            3, "201-1000 人",
            4, "1000 人以上");

    /** 区域 */
    public static final Map<Integer, String> REGION = Map.of(
            1, "华北",
            2, "华东",
            3, "华南",
            4, "西南",
            5, "海外");

    /** 客户级别 */
    public static final Map<Integer, String> LEVEL = Map.of(
            1, "A（战略）",
            2, "B（重点）",
            3, "C（普通）");

    /** 客户状态：1 正常 / 2 公海 / 3 已合并 / 4 成交客户 */
    public static final Map<Integer, String> STATUS = new LinkedHashMap<>();

    static {
        STATUS.put(STATUS_NORMAL, "正常");
        STATUS.put(STATUS_POOL, "公海");
        STATUS.put(STATUS_MERGED, "已合并");
        STATUS.put(STATUS_DEAL, "成交客户");
    }

    private CustomerDicts() {
    }

    public static String label(Map<Integer, String> dict, Integer code) {
        return code == null ? null : dict.get(code);
    }
}
