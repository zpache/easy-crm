package cn.easykits.crm.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 开票申请。status：0 待开票 / 1 已开票 / 2 已作废。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("crm_invoice")
public class Invoice extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 发票号（开票后回填） */
    private String invoiceNo;

    private Long orderId;

    private Long customerId;

    private BigDecimal amount;

    private String title;

    private String taxNo;

    private Integer status;

    private LocalDate applyDate;

    private String remark;
}
