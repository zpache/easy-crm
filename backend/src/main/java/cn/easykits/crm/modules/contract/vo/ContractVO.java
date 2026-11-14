package cn.easykits.crm.modules.contract.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 合同视图。
 */
@Data
public class ContractVO {

    private Long id;

    private String contractNo;

    private Long customerId;

    private String customerName;

    private Long opportunityId;

    private BigDecimal amount;

    private LocalDate signDate;

    private Integer status;

    private String statusName;

    private Long ownerId;

    private Long deptId;

    private String remark;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
