package cn.easykits.crm.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 合同。status：0 草稿 / 1 生效 / 2 已归档 / 3 已作废。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("crm_contract")
public class Contract extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 合同编号，业务唯一 */
    private String contractNo;

    private Long customerId;

    private Long opportunityId;

    private BigDecimal amount;

    private LocalDate signDate;

    private Integer status;

    private Long ownerId;

    private Long deptId;

    private String remark;
}
