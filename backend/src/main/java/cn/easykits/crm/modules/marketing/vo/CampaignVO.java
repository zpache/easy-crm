package cn.easykits.crm.modules.marketing.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 营销活动视图：基础信息 + 按线索归因的效果指标。
 */
@Data
public class CampaignVO {

    private Long id;

    private String code;

    private String name;

    private Integer type;

    private String typeName;

    private String channel;

    private Integer status;

    private String statusName;

    private BigDecimal budget;

    private LocalDate startDate;

    private LocalDate endDate;

    private Integer targetLeads;

    private Long ownerId;

    private String ownerName;

    private Long deptId;

    private String remark;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    // ---------------- 效果指标（按 crm_lead.campaign_id 归因） ----------------

    /** 活动带来的线索数 */
    private Long leadCount;

    /** 已转化线索数 */
    private Long convertedCount;

    /** 转化率 = 已转化 / 线索数（线索数为 0 时为 null） */
    private BigDecimal conversionRate;

    /** 关联商机总金额 */
    private BigDecimal pipelineAmount;

    /** 赢单金额 */
    private BigDecimal wonAmount;

    /** ROI = 赢单金额 / 预算（预算为 0 时为 null） */
    private BigDecimal roi;

    /** 目标达成率 = 线索数 / 目标线索数（目标为 0 时为 null） */
    private BigDecimal targetRate;
}
