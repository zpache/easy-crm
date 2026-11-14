package cn.easykits.crm.modules.product.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 报价明细视图。
 */
@Data
public class QuoteItemVO {

    private Long id;

    private Long quoteId;

    private Long productId;

    private String productName;

    private BigDecimal unitPrice;

    private BigDecimal quantity;

    private BigDecimal discount;

    private BigDecimal subtotal;

    private String remark;
}
