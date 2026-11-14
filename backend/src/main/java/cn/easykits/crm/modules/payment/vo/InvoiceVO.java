package cn.easykits.crm.modules.payment.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 开票申请视图。
 */
@Data
public class InvoiceVO {

    private Long id;

    private String invoiceNo;

    private Long orderId;

    private Long customerId;

    private BigDecimal amount;

    private String title;

    private String taxNo;

    private Integer status;

    private String statusName;

    private LocalDate applyDate;

    private String remark;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
