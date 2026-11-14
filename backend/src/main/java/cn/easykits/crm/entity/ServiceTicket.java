package cn.easykits.crm.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 服务工单。状态机见 {@code TicketStatus}；SLA 应响应/应解决时间在创建时按
 * {@code crm_sla_policy}（ticket_type + priority）计算，超时标记由 ServiceSlaTask 扫描写入。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("crm_service_ticket")
public class ServiceTicket extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 工单编号，业务唯一 */
    private String ticketNo;

    private String title;

    private Long customerId;

    private Long contactId;

    /** 关联订单 ID（可空） */
    private Long orderId;

    /** 工单类型字典值 */
    private String ticketType;

    /** 优先级字典值 */
    private String priority;

    /** 状态：PENDING/ASSIGNED/PROCESSING/WAITING_CONFIRM/COMPLETED/CLOSED */
    private String status;

    /** 受理人 */
    private Long ownerId;

    /** 归属部门 */
    private Long deptId;

    /** SLA 应响应时间 */
    private LocalDateTime slaDueResponseTime;

    /** SLA 应解决时间 */
    private LocalDateTime slaDueResolveTime;

    /** 实际首次响应时间 */
    private LocalDateTime actualResponseTime;

    /** 实际解决时间 */
    private LocalDateTime actualResolveTime;

    /** 超时标记：0 正常 / 1 已超时 */
    private Integer overdueFlag;

    /** 来源字典值 */
    private String source;

    /** 问题描述 */
    private String description;
}
