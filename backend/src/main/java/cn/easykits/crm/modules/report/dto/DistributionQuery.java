package cn.easykits.crm.modules.report.dto;

import lombok.Data;

/**
 * 客户分布查询参数。
 */
@Data
public class DistributionQuery {

    /** 维度白名单：industry / region / level / status，非法值回退 industry */
    private String dimension = "industry";

    private Long deptId;

    private Long ownerId;
}
