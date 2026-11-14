package cn.easykits.crm.modules.payment.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 开票申请创建 / 更新表单。status 流转走 /invoices/{id}/status。
 */
@Data
public class InvoiceForm {

    /** 发票号（开票后回填） */
    @Size(max = 64, message = "发票号不能超过 64 个字符")
    private String invoiceNo;

    @NotNull(message = "订单不能为空")
    private Long orderId;

    @NotNull(message = "客户不能为空")
    private Long customerId;

    @NotNull(message = "开票金额不能为空")
    @DecimalMin(value = "0.00", message = "开票金额不能为负数")
    private BigDecimal amount;

    @Size(max = 128, message = "发票抬头不能超过 128 个字符")
    private String title;

    @Size(max = 64, message = "税号不能超过 64 个字符")
    private String taxNo;

    private LocalDate applyDate;

    @Size(max = 500, message = "备注不能超过 500 个字符")
    private String remark;
}
