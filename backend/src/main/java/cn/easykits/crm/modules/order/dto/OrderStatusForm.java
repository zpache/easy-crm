package cn.easykits.crm.modules.order.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

/**
 * 订单状态流转表单。
 */
@Data
public class OrderStatusForm {

    /** 目标状态，见 OrderStatus */
    @NotNull(message = "目标状态不能为空")
    private Integer status;

    /** 流转到「已交付」时可携带实际交付日期 */
    private LocalDate deliveryDate;

    /** 流转到「已交付」时可携带发货日期 */
    private LocalDate shipDate;
}
