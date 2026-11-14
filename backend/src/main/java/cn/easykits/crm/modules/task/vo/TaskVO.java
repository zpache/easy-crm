package cn.easykits.crm.modules.task.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 待办任务视图。status 动态映射：已完成(1) / 已过期(2, 未完成且过期) / 待办(0)。
 */
@Data
public class TaskVO {

    private Long id;

    private String title;

    private String content;

    private LocalDateTime dueTime;

    /** 0 待办 / 1 已完成 / 2 已过期（动态计算） */
    private Integer status;

    private String statusName;

    /** 是否已过期（未完成且超期） */
    private Boolean expired;

    private String bizType;

    private String bizTypeName;

    private Long bizId;

    private Long ownerId;

    private String ownerName;

    /** 来源：MANUAL / FOLLOW */
    private String source;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
