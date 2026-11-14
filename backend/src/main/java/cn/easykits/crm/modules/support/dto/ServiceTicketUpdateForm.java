package cn.easykits.crm.modules.support.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 工单编辑表单。终态工单不可编辑；
 * customerId / ticketType / priority 创建后不可变更（避免已计算的 SLA 时间漂移）。
 */
@Data
public class ServiceTicketUpdateForm {

    @Size(max = 128, message = "工单标题不能超过 128 个字符")
    private String title;

    private Long contactId;

    private Long orderId;

    private String source;

    @Size(max = 2000, message = "问题描述不能超过 2000 个字符")
    private String description;
}
