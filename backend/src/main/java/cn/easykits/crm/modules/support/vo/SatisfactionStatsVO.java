package cn.easykits.crm.modules.support.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 满意度统计视图：总评价数、平均分、1-5 分分布。
 */
@Data
public class SatisfactionStatsVO {

    /** 评价总数 */
    private long total;

    /** 平均分（保留 2 位小数；无评价时为 0） */
    private BigDecimal averageScore;

    /** 各评分数量（1-5 分，缺失分数补 0） */
    private List<ScoreCountVO> distribution;
}
