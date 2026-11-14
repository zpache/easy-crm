package cn.easykits.crm.modules.report.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 达成分析结果：按月份合并「目标 / 商机赢单实绩 / 订单实绩」。
 */
@Data
public class AchievementVO {

    private List<Row> rows;

    private BigDecimal totalTarget;

    private BigDecimal totalOpportunityAmount;

    private BigDecimal totalOrderAmount;

    /** 达成率 = 订单实绩 / 目标（目标为 0 时返回 null） */
    private BigDecimal achievementRate;

    @Data
    public static class Row {

        private String month;

        private BigDecimal targetAmount;

        private BigDecimal opportunityAmount;

        private BigDecimal orderAmount;

        /** 达成率 = 订单实绩 / 目标（目标为 0 时返回 null） */
        private BigDecimal achievementRate;
    }
}
