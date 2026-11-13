package cn.easykits.crm.modules.opportunity.vo;

import lombok.Data;

/**
 * 阶段配置视图。
 */
@Data
public class StageVO {

    private Long id;

    private String stageCode;

    private String stageName;

    private Integer sortNo;

    private Integer defaultWinRate;

    private Boolean isWon;

    private Boolean isLost;

    private Boolean enabled;
}
