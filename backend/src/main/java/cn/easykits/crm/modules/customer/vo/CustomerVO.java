package cn.easykits.crm.modules.customer.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 客户视图。
 */
@Data
public class CustomerVO {

    private Long id;

    private String name;

    private String creditCode;

    private Integer industry;

    private String industryName;

    private Integer scale;

    private String scaleName;

    private Integer region;

    private String regionName;

    private Integer level;

    private String levelName;

    /** 1 正常 / 2 公海 / 3 已合并 */
    private Integer status;

    private String statusName;

    private Long ownerId;

    private String ownerName;

    private Long deptId;

    private String deptName;

    /** 合并目标客户 ID（status=3 时有效） */
    private Long mergedToId;

    private String remark;

    private LocalDateTime lastFollowTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private Integer version;
}
