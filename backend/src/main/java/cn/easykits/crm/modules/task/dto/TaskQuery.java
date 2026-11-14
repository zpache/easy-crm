package cn.easykits.crm.modules.task.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 待办任务分页查询参数。行级数据权限由拦截器注入 owner 条件。
 */
@Data
public class TaskQuery {

    private long pageNum = 1;

    private long pageSize = 10;

    /** 状态：0 待办 / 1 已完成；不传查全部（已过期动态计算，不落库） */
    private Integer status;

    /** 负责人 ID；传 -1 查无负责人 */
    private Long ownerId;

    /** 截止时间起 */
    private LocalDateTime dueTimeStart;

    /** 截止时间止 */
    private LocalDateTime dueTimeEnd;
}
