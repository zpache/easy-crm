package cn.easykits.crm.modules.support.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * SLA 策略新增 / 编辑表单。(ticketType, priority) 组合唯一。
 */
@Data
public class SlaPolicyForm {

    @NotBlank(message = "工单类型不能为空")
    private String ticketType;

    @NotBlank(message = "优先级不能为空")
    private String priority;

    @NotNull(message = "响应小时数不能为空")
    @Min(value = 1, message = "响应小时数至少为 1")
    private Integer responseHours;

    @NotNull(message = "解决小时数不能为空")
    @Min(value = 1, message = "解决小时数至少为 1")
    private Integer resolveHours;

    /** 1 启用 / 0 停用；不传默认启用 */
    private Integer enabled;
}
