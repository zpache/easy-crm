package cn.easykits.crm.modules.payment.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 回款计划创建 / 更新表单。receivedAmount / status 由核销流程维护，不接受前端传入。
 */
@Data
public class PaymentPlanForm {

    @NotNull(message = "订单不能为空")
    private Long orderId;

    @NotNull(message = "期次不能为空")
    @Min(value = 1, message = "期次从 1 开始")
    private Integer periodNo;

    private LocalDate planDate;

    @NotNull(message = "计划金额不能为空")
    @DecimalMin(value = "0.00", message = "计划金额不能为负数")
    private BigDecimal planAmount;

    private String remark;
}
