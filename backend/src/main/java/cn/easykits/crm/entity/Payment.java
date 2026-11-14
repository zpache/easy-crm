package cn.easykits.crm.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 回款记录。writeoffStatus：0 未核销 / 1 已核销。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("crm_payment")
public class Payment extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 关联回款计划（可空=无计划直接回款） */
    private Long planId;

    private Long orderId;

    /** 实收金额 */
    private BigDecimal amount;

    private LocalDate paymentDate;

    private Integer writeoffStatus;

    private LocalDateTime writeoffTime;

    private String remark;
}
