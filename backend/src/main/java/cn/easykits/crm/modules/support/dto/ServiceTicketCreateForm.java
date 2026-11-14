package cn.easykits.crm.modules.support.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 工单创建表单。创建时按 (ticketType, priority) 匹配启用的 SLA 策略计算
 * 应响应 / 应解决时间；customerId / ticketType / priority 创建后不可变更。
 */
@Data
public class ServiceTicketCreateForm {

    @NotBlank(message = "工单标题不能为空")
    @Size(max = 128, message = "工单标题不能超过 128 个字符")
    private String title;

    @NotNull(message = "客户 ID 不能为空")
    private Long customerId;

    /** 联系人 ID（可空） */
    private Long contactId;

    /** 订单 ID（可空） */
    private Long orderId;

    /** 工单类型字典值 */
    @NotBlank(message = "工单类型不能为空")
    private String ticketType;

    /** 优先级字典值 */
    @NotBlank(message = "优先级不能为空")
    private String priority;

    /** 来源字典值（可空） */
    private String source;

    @Size(max = 2000, message = "问题描述不能超过 2000 个字符")
    private String description;
}
