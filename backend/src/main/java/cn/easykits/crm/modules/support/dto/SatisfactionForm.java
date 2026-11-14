package cn.easykits.crm.modules.support.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 满意度评价表单。仅已完成（COMPLETED）工单可评价，一单一评。
 */
@Data
public class SatisfactionForm {

    @NotNull(message = "工单 ID 不能为空")
    private Long ticketId;

    @NotNull(message = "评分不能为空")
    @Min(value = 1, message = "评分最低为 1 分")
    @Max(value = 5, message = "评分最高为 5 分")
    private Integer score;

    @Size(max = 1000, message = "评价内容不能超过 1000 个字符")
    private String content;
}
