package cn.easykits.crm.modules.payment.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 退货 / 退款视图。
 */
@Data
public class RefundVO {

    private Long id;

    private String refundNo;

    private Long orderId;

    private Long customerId;

    private BigDecimal amount;

    private String reason;

    private Integer status;

    private String statusName;

    private LocalDate refundDate;

    private String remark;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
