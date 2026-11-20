package cn.easykits.crm.modules.order.dto;

import lombok.Data;

/**
 * 订单分页查询参数。
 */
@Data
public class OrderQuery {

    private long pageNum = 1;

    private long pageSize = 10;

    private Long customerId;

    private Long opportunityId;

    private Long contractId;

    /** 关联报价单 ID */
    private Long quoteId;

    private Integer status;

    /** 订单编号模糊匹配 */
    private String orderNo;
}
