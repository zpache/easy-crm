package cn.easykits.crm.modules.follow.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 跟进记录创建表单。nextFollowTime 选填，填写后自动生成待办任务。
 */
@Data
public class FollowRecordCreateForm {

    /** 业务类型：lead / customer / opportunity（order 轮次 5） */
    @NotBlank(message = "业务类型不能为空")
    private String bizType;

    @NotNull(message = "业务 ID 不能为空")
    private Long bizId;

    /** 跟进类型字典：PHONE / VISIT / EMAIL / WECHAT / OTHER */
    @NotBlank(message = "跟进类型不能为空")
    private String followType;

    @NotBlank(message = "跟进内容不能为空")
    @Size(max = 1000, message = "跟进内容不能超过 1000 个字符")
    private String content;

    /** 下次跟进时间（选填，生成待办任务） */
    private LocalDateTime nextFollowTime;
}
