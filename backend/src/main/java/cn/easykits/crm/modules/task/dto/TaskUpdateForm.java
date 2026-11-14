package cn.easykits.crm.modules.task.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 待办任务更新表单（已完成任务不可编辑；biz 关联不可变更）。
 */
@Data
public class TaskUpdateForm {

    @Size(max = 128, message = "任务标题不能超过 128 个字符")
    private String title;

    @Size(max = 1000, message = "任务内容不能超过 1000 个字符")
    private String content;

    private LocalDateTime dueTime;
}
