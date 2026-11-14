package cn.easykits.crm.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 报价明细（独立表，不复用 opportunity_item）。
 * subtotal = unitPrice * quantity - discount。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("crm_quote_item")
public class QuoteItem extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long quoteId;

    private Long productId;

    /** 产品名称快照 */
    private String productName;

    private BigDecimal unitPrice;

    private BigDecimal quantity;

    private BigDecimal discount;

    private BigDecimal subtotal;

    private String remark;
}
