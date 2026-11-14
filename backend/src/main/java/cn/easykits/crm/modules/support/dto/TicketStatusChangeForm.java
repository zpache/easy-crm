package cn.easykits.crm.modules.support.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 工单状态流转表单。目标状态必须是合法字典值且满足状态机转移规则，终态不可再变。
 */
@Data
public class TicketStatusChangeForm {

    @NotBlank(message = "目标状态不能为空")
    private String targetStatus;

    @Size(max = 500, message = "备注不能超过 500 个字符")
    private String remark;
}
