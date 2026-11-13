package com.easykits.crm.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 提醒记录（商机停滞检测等场景写入，owner 维度已读/未读）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("crm_reminder")
public class Reminder extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 业务类型，如 opportunity_stale */
    private String bizType;

    /** 业务 ID */
    private Long bizId;

    /** 提醒接收人 */
    private Long ownerId;

    /** 提醒等级：1 黄 2 橙 3 红 */
    private Integer remindLevel;

    private LocalDateTime remindTime;

    /** 0 未读 / 1 已读 */
    private Integer readFlag;

    private String content;
}
