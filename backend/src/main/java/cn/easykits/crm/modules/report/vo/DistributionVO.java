package cn.easykits.crm.modules.report.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 客户分布：按行业 / 区域 / 级别 / 状态维度统计客户数与占比。
 */
@Data
public class DistributionVO {

    /** 维度：industry / region / level / status */
    private String dimension;

    private String dimensionName;

    private Long total;

    private List<Item> items;

    @Data
    public static class Item {

        /** 原始字典值（未匹配到时为 "未知"） */
        private String key;

        /** 展示名称（字典优先，常量兜底） */
        private String label;

        private Long count;

        /** 占比（%），保留 2 位小数 */
        private BigDecimal percent;
    }
}
