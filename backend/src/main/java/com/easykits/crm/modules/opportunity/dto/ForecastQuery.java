package com.easykits.crm.modules.opportunity.dto;

import lombok.Data;

import java.time.LocalDate;

/**
 * 销售预测查询参数：按部门 + 预计成交时间范围聚合。
 */
@Data
public class ForecastQuery {

    /** 部门 ID（可选，不传为全部有权限数据） */
    private Long deptId;

    /** 预计成交日期起 */
    private LocalDate startDate;

    /** 预计成交日期止 */
    private LocalDate endDate;
}
