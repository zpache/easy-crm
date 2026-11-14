package cn.easykits.crm.modules.product.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商机产品明细视图。
 */
@Data
public class OpportunityItemVO {

    private Long id;

    private Long opportunityId;

    private Long productId;

    private String productName;

    private BigDecimal unitPrice;

    private BigDecimal quantity;

    private BigDecimal discount;

    private BigDecimal subtotal;

    private String remark;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
