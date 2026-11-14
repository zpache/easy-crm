package cn.easykits.crm.modules.dashboard.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * CRM 数据看板聚合视图（一次请求返回全部板块）。
 * 各板块均为独立聚合 SQL，查询次数固定，不随业务数据量增长；
 * 行级数据权限拦截器自动限定统计口径（SELF 用户看到自己的数据）。
 */
@Data
public class DashboardOverviewVO {

    /** 指标卡 */
    private Cards cards;

    /** 销售转化漏斗：线索 → 跟进中 → 已验证 → 成交客户 → 进行中商机 → 赢单 */
    private List<FunnelItem> funnel;

    /** 商机阶段分布（进行中，含加权预测） */
    private List<StageDist> stageDistribution;

    /** 近 N 天跟进活动趋势 */
    private List<TrendPoint> followTrend;

    /** 近 N 天新增线索 / 客户趋势 */
    private List<NewTrendPoint> newTrend;

    /** 负责人进行中商机 TOP 5（金额降序） */
    private List<OwnerRank> ownerRanking;

    /** 停滞提醒等级分布（未读）：1 黄 / 2 橙 / 3 红 */
    private Map<Integer, Long> staleReminders;

    /** 指标卡数据 */
    @Data
    public static class Cards {
        private long leadsTotal;
        private long leadsNew7d;
        private long customersTotal;
        private long customersNew7d;
        private long oppTotal;
        private long oppOpenCnt;
        /** 进行中商机预计金额合计 */
        private BigDecimal oppOpenAmount;
        /** 加权预测金额（赢单率 × 金额） */
        private BigDecimal oppForecastAmount;
        /** 我的未完成待办数 */
        private long myTasksPending;
        /** 我的已过期待办数 */
        private long myTasksOverdue;
    }

    /**
     * 漏斗项。
     */
    @Data
    public static class FunnelItem {
        private String stage;
        private long value;

        public FunnelItem(String stage, long value) {
            this.stage = stage;
            this.value = value;
        }
    }

    /**
     * 阶段分布项。
     */
    @Data
    public static class StageDist {
        private String stageName;
        private Integer sortNo;
        private Integer winRate;
        private long count;
        private BigDecimal amount;
        private BigDecimal forecastAmount;
    }

    /**
     * 跟进趋势点。
     */
    @Data
    public static class TrendPoint {
        private String date;
        private long count;
    }

    /**
     * 新增线索 / 客户趋势点。
     */
    @Data
    public static class NewTrendPoint {
        private String date;
        private long leads;
        private long customers;
    }

    /**
     * 负责人排行项。
     */
    @Data
    public static class OwnerRank {
        private String ownerName;
        private long count;
        private BigDecimal amount;
    }
}
