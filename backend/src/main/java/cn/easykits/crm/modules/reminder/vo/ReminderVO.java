package cn.easykits.crm.modules.reminder.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 提醒视图（按当前用户 ownerId 过滤后返回）。
 */
@Data
public class ReminderVO {

    private Long id;

    /** 业务类型，如 opportunity_stale */
    private String bizType;

    /** 业务对象 ID */
    private Long bizId;

    /** 提醒接收人 */
    private Long ownerId;

    /** 提醒等级：1 黄 / 2 橙 / 3 红 */
    private Integer remindLevel;

    /** 等级展示名（黄 / 橙 / 红） */
    private String levelName;

    private LocalDateTime remindTime;

    /** 0 未读 / 1 已读 */
    private Integer readFlag;

    private String content;

    private LocalDateTime createTime;
}
