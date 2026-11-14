package cn.easykits.crm.modules.contract.dto;

import lombok.Data;

/**
 * 合同分页查询参数。
 */
@Data
public class ContractQuery {

    private long pageNum = 1;

    private long pageSize = 10;

    private Long customerId;

    private Long opportunityId;

    private Integer status;

    /** 合同编号模糊匹配 */
    private String contractNo;
}
