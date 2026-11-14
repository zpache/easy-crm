package cn.easykits.crm.modules.follow.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 跟进记录视图。
 */
@Data
public class FollowRecordVO {

    private Long id;

    private String bizType;

    private String bizTypeName;

    private Long bizId;

    private String followType;

    private String followTypeName;

    private String content;

    private LocalDateTime nextFollowTime;

    private Long ownerId;

    private String ownerName;

    private Long deptId;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
