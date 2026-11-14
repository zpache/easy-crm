package cn.easykits.crm.modules.report.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 销售目标视图。
 */
@Data
public class SalesTargetVO {

    private Long id;

    private String targetMonth;

    private Long deptId;

    private String deptName;

    private Long ownerId;

    private String ownerName;

    private BigDecimal targetAmount;

    private String remark;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
