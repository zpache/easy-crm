package cn.easykits.crm.modules.product.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 报价明细分项。subtotal 由服务端按「单价*数量-折扣」计算，不接收前端传入。
 */
@Data
public class QuoteItemForm {

    /** 可空：非标项手工录入 */
    private Long productId;

    @NotBlank(message = "明细产品名称不能为空")
    @Size(max = 128, message = "明细产品名称不能超过 128 个字符")
    private String productName;

    @NotNull(message = "明细单价不能为空")
    @DecimalMin(value = "0.00", message = "明细单价不能为负数")
    private BigDecimal unitPrice;

    @NotNull(message = "明细数量不能为空")
    @DecimalMin(value = "0.00", message = "明细数量不能为负数")
    private BigDecimal quantity;

    /** 折扣金额，为空按 0 */
    @DecimalMin(value = "0.00", message = "明细折扣不能为负数")
    private BigDecimal discount;

    @Size(max = 500, message = "明细备注不能超过 500 个字符")
    private String remark;
}
