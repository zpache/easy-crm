package cn.easykits.crm.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 订单明细快照。subtotal = unitPrice * quantity - discount。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("crm_order_item")
public class OrderItem extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long orderId;

    private Long productId;

    /** 产品名称快照 */
    private String productName;

    private BigDecimal unitPrice;

    private BigDecimal quantity;

    private BigDecimal discount;

    private BigDecimal subtotal;

    private String remark;
}
