package cn.easykits.crm.modules.follow.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 时间线条目（跟进记录 / 待办任务 / 商机阶段变更三类聚合）。
 */
@Data
public class TimelineItemVO {

    /** 来源：follow 跟进记录 / task 待办任务 / stage_log 阶段变更 */
    private String source;

    /** 对应表主键 */
    private Long id;

    /** 发生时间（跟进=创建时间，任务=截止时间，阶段变更=变更时间） */
    private LocalDateTime time;

    /** 内容（跟进内容 / 任务标题 / 阶段变更备注） */
    private String content;

    /** 类型码：跟进类型 / 任务状态 / 目标阶段 */
    private String typeCode;

    /** 展示用类型名（电话… / 待办… / 阶段名…） */
    private String typeName;

    /** 阶段变更专用：原阶段 / 新阶段编码 */
    private String fromStage;

    private String toStage;

    private String fromStageName;

    private String toStageName;

    /** 操作人 / 跟进人 */
    private Long operatorId;

    private String operatorName;
}
