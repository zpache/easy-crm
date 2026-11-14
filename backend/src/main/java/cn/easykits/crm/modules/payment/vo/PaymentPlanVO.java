package cn.easykits.crm.modules.payment.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 回款计划视图。
 */
@Data
public class PaymentPlanVO {

    private Long id;

    private Long orderId;

    private Integer periodNo;

    private LocalDate planDate;

    private BigDecimal planAmount;

    private BigDecimal receivedAmount;

    private Integer status;

    private String statusName;

    private String remark;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
