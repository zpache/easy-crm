package cn.easykits.crm.modules.support.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 服务工单视图（列表 / 详情共用）。流转日志通过 {@code GET /tickets/{id}/logs} 单独加载。
 */
@Data
public class ServiceTicketVO {

    private Long id;

    private String ticketNo;

    private String title;

    private Long customerId;

    private String customerName;

    private Long contactId;

    private String contactName;

    private Long orderId;

    private String ticketType;

    private String ticketTypeName;

    private String priority;

    private String priorityName;

    private String status;

    private String statusName;

    private Long ownerId;

    private String ownerName;

    private Long deptId;

    private LocalDateTime slaDueResponseTime;

    private LocalDateTime slaDueResolveTime;

    private LocalDateTime actualResponseTime;

    private LocalDateTime actualResolveTime;

    /** 0 正常 / 1 已超时（落库标记，由定时任务写入） */
    private Integer overdueFlag;

    /** 是否超时（结合 SLA 时间动态计算，供列表即时展示） */
    private Boolean overdue;

    private String source;

    private String sourceName;

    private String description;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private Integer version;
}
