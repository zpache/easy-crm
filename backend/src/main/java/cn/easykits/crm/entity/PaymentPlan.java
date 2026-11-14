package cn.easykits.crm.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 回款计划。receivedAmount 由回款核销累加；
 * status：0 未核销 / 1 部分核销 / 2 已核销。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("crm_payment_plan")
public class PaymentPlan extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long orderId;

    /** 期次（同订单内唯一） */
    private Integer periodNo;

    private LocalDate planDate;

    private BigDecimal planAmount;

    private BigDecimal receivedAmount;

    private Integer status;

    private String remark;
}
