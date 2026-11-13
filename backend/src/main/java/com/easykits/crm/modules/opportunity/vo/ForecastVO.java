package com.easykits.crm.modules.opportunity.vo;

import lombok.Data;

import java.util.List;

/**
 * 销售预测：一条聚合 SQL 按阶段返回，totalForecast 为各行合计（固定行数求和，非循环累加）。
 */
@Data
public class ForecastVO {

    private List<StageRow> rows;

    /** 全阶段预测合计 */
    private java.math.BigDecimal totalForecast;

    /** 商机总数 */
    private long totalCount;

    /**
     * 单阶段聚合行。
     */
    @Data
    public static class StageRow {
        private String stageCode;
        private String stageName;
        private long cnt;
        private java.math.BigDecimal totalAmount;
        private java.math.BigDecimal forecastAmount;
    }
}
