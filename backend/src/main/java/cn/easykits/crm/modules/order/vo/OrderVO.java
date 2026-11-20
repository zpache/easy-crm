package cn.easykits.crm.modules.order.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单视图（列表 / 详情共用；详情携带 items）。
 */
@Data
public class OrderVO {

    private Long id;

    private String orderNo;

    private Long customerId;

    private String customerName;

    private Long opportunityId;

    private Long contractId;

    private Long quoteId;

    private BigDecimal totalAmount;

    private Integer status;

    private String statusName;

    private LocalDate shipDate;

    private LocalDate deliveryDate;

    private Long ownerId;

    private Long deptId;

    private String remark;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    /** 仅详情返回 */
    private List<OrderItemVO> items;
}
