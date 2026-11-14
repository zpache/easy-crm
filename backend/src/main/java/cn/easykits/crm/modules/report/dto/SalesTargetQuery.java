package cn.easykits.crm.modules.report.dto;

import lombok.Data;

/**
 * 销售目标分页查询参数。
 */
@Data
public class SalesTargetQuery {

    private long pageNum = 1;

    private long pageSize = 10;

    /** 目标年月 YYYY-MM */
    private String targetMonth;

    private Long deptId;

    private Long ownerId;
}
