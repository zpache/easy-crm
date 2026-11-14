package cn.easykits.crm.modules.payment.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 退货 / 退款创建 / 更新表单。refundNo 留空由服务端生成；status 流转走 /refunds/{id}/status。
 */
@Data
public class RefundForm {

    /** 退款单号；留空自动生成 RF-yyyyMMddHHmmss-xxxx */
    @Size(max = 64, message = "退款单号不能超过 64 个字符")
    private String refundNo;

    @NotNull(message = "订单不能为空")
    private Long orderId;

    @NotNull(message = "客户不能为空")
    private Long customerId;

    @NotNull(message = "退款金额不能为空")
    @DecimalMin(value = "0.00", message = "退款金额不能为负数")
    private BigDecimal amount;

    @Size(max = 500, message = "退款原因不能超过 500 个字符")
    private String reason;

    private LocalDate refundDate;

    @Size(max = 500, message = "备注不能超过 500 个字符")
    private String remark;
}
