package cn.easykits.crm.modules.payment.dto;

import lombok.Data;

/**
 * 回款记录分页查询参数。
 */
@Data
public class PaymentQuery {

    private long pageNum = 1;

    private long pageSize = 10;

    private Long orderId;

    private Long planId;

    /** 核销状态：0 未核销 / 1 已核销 */
    private Integer writeoffStatus;
}
