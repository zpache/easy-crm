package cn.easykits.crm.modules.support.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 满意度评价查询 / 统计参数。行级数据权限由拦截器按 crm_satisfaction 注入 owner/dept 条件。
 */
@Data
public class SatisfactionQuery {

    private long pageNum = 1;

    private long pageSize = 10;

    private Long ticketId;

    /** 评分 1-5 */
    private Integer score;

    /** 评价时间起 */
    private LocalDateTime startDate;

    /** 评价时间止 */
    private LocalDateTime endDate;
}
