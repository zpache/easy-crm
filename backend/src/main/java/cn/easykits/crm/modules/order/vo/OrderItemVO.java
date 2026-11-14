package cn.easykits.crm.modules.order.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 订单明细视图。
 */
@Data
public class OrderItemVO {

    private Long id;

    private Long orderId;

    private Long productId;

    private String productName;

    private BigDecimal unitPrice;

    private BigDecimal quantity;

    private BigDecimal discount;

    private BigDecimal subtotal;

    private String remark;
}
