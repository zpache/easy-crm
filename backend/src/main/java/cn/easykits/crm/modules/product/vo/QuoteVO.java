package cn.easykits.crm.modules.product.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 报价单视图（列表 / 详情共用；详情携带 items，列表不携带以减小载荷）。
 */
@Data
public class QuoteVO {

    private Long id;

    private String quoteNo;

    private Long opportunityId;

    private Long customerId;

    private String customerName;

    private BigDecimal totalAmount;

    private Integer status;

    private String statusName;

    private LocalDate validUntil;

    private Long ownerId;

    private Long deptId;

    private String remark;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    /** 仅详情返回 */
    private List<QuoteItemVO> items;
}
