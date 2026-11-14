package cn.easykits.crm.modules.follow.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 跟进记录更新表单（关联对象不可变更，需删除重建）。
 */
@Data
public class FollowRecordUpdateForm {

    @NotBlank(message = "跟进类型不能为空")
    private String followType;

    @NotBlank(message = "跟进内容不能为空")
    @Size(max = 1000, message = "跟进内容不能超过 1000 个字符")
    private String content;

    /** 下次跟进时间（选填；更新后同步刷新关联待办的截止时间） */
    private LocalDateTime nextFollowTime;
}
