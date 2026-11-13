package cn.easykits.crm.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 商机。必须关联客户；stage_code 冗余当前阶段赢单率（win_rate）供预测直接计算；
 * last_activity_at 为停滞检测依据。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("crm_opportunity")
public class Opportunity extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    /** 关联客户（必须） */
    private Long customerId;

    /** 关联联系人（可选） */
    private Long contactId;

    /** 来源线索 ID（转化时回填） */
    private Long leadId;

    /** 当前阶段编码（crm_opportunity_stage.stage_code） */
    private String stageCode;

    /** 预计金额 */
    private BigDecimal amount;

    /** 币种，默认 CNY */
    private String currency;

    /** 当前赢单率（冗余阶段配置） */
    private Integer winRate;

    /** 预计成交日期 */
    private LocalDate expectedCloseDate;

    /** 实际成交日期（赢单时记录） */
    private LocalDate actualCloseDate;

    /** 实际成交金额（赢单时记录） */
    private BigDecimal actualAmount;

    /** 丢单原因字典值（LostReasons） */
    private String lostReason;

    /** 竞争对手（丢单时可选） */
    private String competitor;

    /** 负责人 */
    private Long ownerId;

    /** 归属部门 */
    private Long deptId;

    /** 最近活动时间（停滞检测依据） */
    private LocalDateTime lastActivityAt;
}
