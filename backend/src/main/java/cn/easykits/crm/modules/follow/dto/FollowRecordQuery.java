package cn.easykits.crm.modules.follow.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 跟进记录分页查询参数。行级数据权限由拦截器注入 owner/dept 条件。
 */
@Data
public class FollowRecordQuery {

    private long pageNum = 1;

    private long pageSize = 10;

    /** 业务类型：lead / customer / opportunity */
    private String bizType;

    /** 业务 ID */
    private Long bizId;

    /** 跟进人 ID；传 -1 查无负责人 */
    private Long ownerId;

    /** 跟进时间起（create_time） */
    private LocalDateTime startDate;

    /** 跟进时间止（create_time） */
    private LocalDateTime endDate;
}
