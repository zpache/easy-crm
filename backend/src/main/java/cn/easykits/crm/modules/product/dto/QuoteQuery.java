package cn.easykits.crm.modules.product.dto;

import lombok.Data;

/**
 * 报价单分页查询参数。
 */
@Data
public class QuoteQuery {

    private long pageNum = 1;

    private long pageSize = 10;

    private Long customerId;

    private Long opportunityId;

    private Integer status;

    /** 报价单编号模糊匹配 */
    private String quoteNo;
}
