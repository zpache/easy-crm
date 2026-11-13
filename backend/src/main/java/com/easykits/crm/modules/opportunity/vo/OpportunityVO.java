package com.easykits.crm.modules.opportunity.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 商机视图（列表 / 详情共用）。
 */
@Data
public class OpportunityVO {

    private Long id;

    private String name;

    private Long customerId;

    private String customerName;

    private Long contactId;

    private String contactName;

    /** 来源线索 ID（转化创建时回填） */
    private Long leadId;

    private String stageCode;

    private String stageName;

    /** 预计金额 */
    private BigDecimal amount;

    private String currency;

    private Integer winRate;

    private LocalDate expectedCloseDate;

    private LocalDate actualCloseDate;

    private BigDecimal actualAmount;

    /** 丢单原因（丢单时） */
    private String lostReason;

    private String lostReasonName;

    private String competitor;

    private Long ownerId;

    private String ownerName;

    private Long deptId;

    private String deptName;

    /** 停滞等级：0 正常 / 1 黄（7 天）/ 2 橙（14 天）/ 3 红（30 天） */
    private Integer staleLevel;

    private LocalDateTime lastActivityAt;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private Integer version;
}
