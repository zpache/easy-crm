package cn.easykits.crm.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 报价单。totalAmount 必须等于 crm_quote_item 明细汇总，服务层强制校验。
 * status：0 草稿 / 1 已提交 / 2 已确认 / 3 已作废。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("crm_quote")
public class Quote extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 报价单编号，业务唯一 */
    private String quoteNo;

    private Long opportunityId;

    private Long customerId;

    private BigDecimal totalAmount;

    private Integer status;

    /** 报价有效期（截止日） */
    private LocalDate validUntil;

    private Long ownerId;

    private Long deptId;

    private String remark;
}
