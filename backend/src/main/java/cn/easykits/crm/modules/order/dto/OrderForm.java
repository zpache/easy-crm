package cn.easykits.crm.modules.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 订单创建 / 更新表单。
 * 强制约定：明细汇总（Σ 单价*数量-折扣）必须等于 totalAmount，不一致拒绝保存。
 * orderNo 留空由服务端生成；状态流转走 /orders/{id}/status，不在本表单里改。
 */
@Data
public class OrderForm {

    /** 订单编号；留空自动生成 SO-yyyyMMddHHmmss-xxxx */
    @Size(max = 64, message = "订单编号不能超过 64 个字符")
    private String orderNo;

    @NotNull(message = "客户不能为空")
    private Long customerId;

    private Long opportunityId;

    private Long contractId;

    @NotNull(message = "总金额不能为空")
    @DecimalMin(value = "0.00", message = "总金额不能为负数")
    private BigDecimal totalAmount;

    private LocalDate shipDate;

    private LocalDate deliveryDate;

    @Size(max = 500, message = "备注不能超过 500 个字符")
    private String remark;

    @Valid
    @NotEmpty(message = "订单明细不能为空")
    private List<OrderItemForm> items;
}
