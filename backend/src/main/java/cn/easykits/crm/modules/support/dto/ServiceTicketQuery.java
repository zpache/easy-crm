package cn.easykits.crm.modules.support.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 工单分页查询参数。行级数据权限由拦截器按 crm_service_ticket 注入 owner/dept 条件。
 */
@Data
public class ServiceTicketQuery {

    private long pageNum = 1;

    private long pageSize = 10;

    /** 关键字：工单编号 / 标题模糊匹配 */
    private String keyword;

    /** 状态（TicketStatus.code） */
    private String status;

    private String ticketType;

    private String priority;

    private Long customerId;

    /** 受理人 ID；传 -1 查未派单 */
    private Long ownerId;

    /** 超时标记：0 正常 / 1 已超时 */
    private Integer overdueFlag;

    private String source;

    /** 创建时间起 */
    private LocalDateTime createTimeStart;

    /** 创建时间止 */
    private LocalDateTime createTimeEnd;
}
