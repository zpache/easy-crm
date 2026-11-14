package cn.easykits.crm.modules.reminder.dto;

import lombok.Data;

/**
 * 提醒分页查询参数。ownerId 不接收前端传值，服务端固定按当前登录用户过滤，
 * 避免越权读取他人提醒。
 */
@Data
public class ReminderQuery {

    private long pageNum = 1;

    private long pageSize = 10;

    /** true 时只返回未读 */
    private Boolean onlyUnread;
}
