package cn.easykits.crm.modules.support.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 派单表单：指定受理人。PENDING → ASSIGNED 视为状态流转；
 * 已派单 / 处理中状态改派只更新受理人（记为同状态日志）。
 */
@Data
public class TicketAssignForm {

    @NotNull(message = "受理人不能为空")
    private Long ownerId;

    @Size(max = 500, message = "备注不能超过 500 个字符")
    private String remark;
}
