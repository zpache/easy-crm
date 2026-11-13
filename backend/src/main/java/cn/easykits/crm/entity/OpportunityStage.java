package cn.easykits.crm.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 商机销售阶段配置。赢单（is_won）/ 丢单（is_lost）为终态；
 * sort_no 决定流转前后顺序，default_win_rate 供销售预测计算。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("crm_opportunity_stage")
public class OpportunityStage extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 阶段编码，全局唯一 */
    private String stageCode;

    private String stageName;

    /** 排序，阶段流转按此前后 */
    private Integer sortNo;

    /** 默认赢单率（百分比） */
    private Integer defaultWinRate;

    /** 1 赢单终态 */
    private Integer isWon;

    /** 1 丢单终态 */
    private Integer isLost;

    /** 1 启用 */
    private Integer enabled;
}
