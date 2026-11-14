package cn.easykits.crm.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 商机阶段变更日志：与阶段变更同事务写入，时间线视图据此还原阶段历史。
 * 仅插入不修改，不继承 BaseEntity（无乐观锁/逻辑删除诉求）。
 */
@Data
@TableName("crm_opportunity_stage_log")
public class OpportunityStageLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long opportunityId;

    /** 原阶段编码（创建时为空） */
    private String fromStage;

    private String toStage;

    private Long operatorId;

    private LocalDateTime changeTime;

    private String remark;
}
