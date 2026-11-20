package cn.easykits.crm.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 订单。状态机见 modules/order/constant/OrderStatus：
 * 0 草稿 -> 1 待确认 -> 2 已确认 -> 3 已交付 -> 4 已完成；9 已取消。
 * totalAmount 必须等于 crm_order_item 明细汇总。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("crm_order")
public class Order extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 订单编号，业务唯一 */
    private String orderNo;

    private Long customerId;

    private Long opportunityId;

    private Long contractId;

    /** 关联报价单 ID（从报价成交转单时回填） */
    private Long quoteId;

    private BigDecimal totalAmount;

    private Integer status;

    private LocalDate shipDate;

    private LocalDate deliveryDate;

    private Long ownerId;

    private Long deptId;

    private String remark;
}
