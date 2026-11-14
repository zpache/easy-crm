package cn.easykits.crm.modules.payment.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 回款记录创建 / 更新表单。核销状态由 /payments/{id}/writeoff 维护，不接受前端传入。
 */
@Data
public class PaymentForm {

    /** 关联回款计划（可空=无计划直接回款） */
    private Long planId;

    @NotNull(message = "订单不能为空")
    private Long orderId;

    @NotNull(message = "实收金额不能为空")
    @DecimalMin(value = "0.00", message = "实收金额不能为负数")
    private BigDecimal amount;

    private LocalDate paymentDate;

    @Size(max = 500, message = "备注不能超过 500 个字符")
    private String remark;
}
