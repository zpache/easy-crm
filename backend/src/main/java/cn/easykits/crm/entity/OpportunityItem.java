package cn.easykits.crm.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 商机产品明细快照：名称/单价为录入时刻快照，不随产品主数据变化。
 * subtotal = unitPrice * quantity - discount（由服务层计算并校验）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("crm_opportunity_item")
public class OpportunityItem extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long opportunityId;

    /** 可空：非标项手工录入时无产品 ID */
    private Long productId;

    /** 产品名称快照 */
    private String productName;

    private BigDecimal unitPrice;

    private BigDecimal quantity;

    /** 折扣金额（按行减免） */
    private BigDecimal discount;

    private BigDecimal subtotal;

    private String remark;
}
