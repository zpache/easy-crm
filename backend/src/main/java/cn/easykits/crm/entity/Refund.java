package cn.easykits.crm.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 退货 / 退款。status：0 申请中 / 1 已审核 / 2 已退款 / 3 已驳回。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("crm_refund")
public class Refund extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 退款单号，业务唯一 */
    private String refundNo;

    private Long orderId;

    private Long customerId;

    private BigDecimal amount;

    private String reason;

    private Integer status;

    private LocalDate refundDate;

    private String remark;
}
