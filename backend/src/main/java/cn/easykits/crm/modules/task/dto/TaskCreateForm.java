package cn.easykits.crm.modules.task.dto;

import cn.easykits.crm.modules.follow.constant.FollowBizTypes;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 待办任务创建表单（手动创建；跟进记录自动生成走服务内部）。
 */
@Data
public class TaskCreateForm {

    @NotBlank(message = "任务标题不能为空")
    @Size(max = 128, message = "任务标题不能超过 128 个字符")
    private String title;

    @Size(max = 1000, message = "任务内容不能超过 1000 个字符")
    private String content;

    /** 截止时间 */
    @NotNull(message = "截止时间不能为空")
    private LocalDateTime dueTime;

    /** 关联业务类型（选填） */
    private String bizType;

    /** 关联业务 ID（bizType 填写时必填） */
    private Long bizId;
}
