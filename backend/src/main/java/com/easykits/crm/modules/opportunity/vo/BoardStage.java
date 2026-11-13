package com.easykits.crm.modules.opportunity.vo;

import lombok.Data;

import java.util.List;

/**
 * 看板单阶段分组（含该阶段的商机卡片列表）。
 */
@Data
public class BoardStage {

    private String stageCode;

    private String stageName;

    private Integer sortNo;

    private Integer defaultWinRate;

    /** 赢单终态 */
    private Boolean won;

    /** 丢单终态 */
    private Boolean lost;

    private Boolean enabled;

    /** 该阶段的商机卡片 */
    private List<OpportunityVO> opportunities;
}
