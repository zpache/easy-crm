package com.easykits.crm.modules.opportunity.dto;

import lombok.Data;

import java.time.LocalDate;

/**
 * 商机分页查询参数。行级数据权限由拦截器按登录用户注入。
 */
@Data
public class OpportunityQuery {

    private long pageNum = 1;

    private long pageSize = 10;

    /** 关联客户 */
    private Long customerId;

    /** 当前阶段编码 */
    private String stageCode;

    /** 负责人 ID；传 -1 查无负责人 */
    private Long ownerId;

    /** 预计成交日期起 */
    private LocalDate startDate;

    /** 预计成交日期止 */
    private LocalDate endDate;
}
