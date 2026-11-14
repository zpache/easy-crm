package cn.easykits.crm.modules.payment.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 回款记录视图。
 */
@Data
public class PaymentVO {

    private Long id;

    private Long planId;

    private Long orderId;

    private BigDecimal amount;

    private LocalDate paymentDate;

    private Integer writeoffStatus;

    private String writeoffStatusName;

    private LocalDateTime writeoffTime;

    private String remark;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
