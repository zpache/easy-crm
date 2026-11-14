package cn.easykits.crm.modules.product.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 商机产品明细创建 / 更新表单。subtotal 由服务层计算，不接收前端传入。
 */
@Data
public class OpportunityItemForm {

    @NotNull(message = "商机不能为空")
    private Long opportunityId;

    /** 可空：非标项手工录入 */
    private Long productId;

    @NotBlank(message = "产品名称不能为空")
    private String productName;

    @NotNull(message = "单价不能为空")
    @DecimalMin(value = "0.00", message = "单价不能为负数")
    private BigDecimal unitPrice;

    @NotNull(message = "数量不能为空")
    @DecimalMin(value = "0.00", message = "数量不能为负数")
    private BigDecimal quantity;

    /** 折扣金额（按行减免），为空按 0 处理 */
    @DecimalMin(value = "0.00", message = "折扣不能为负数")
    private BigDecimal discount;

    private String remark;
}
