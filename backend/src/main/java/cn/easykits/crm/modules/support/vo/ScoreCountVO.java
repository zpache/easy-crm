package cn.easykits.crm.modules.support.vo;

import lombok.Data;

/**
 * 评分分布聚合结果（SatisfactionMapper.selectScoreDistribution 的直接映射）。
 */
@Data
public class ScoreCountVO {

    /** 评分 1-5 */
    private Integer score;

    /** 该评分条数 */
    private Long cnt;
}
