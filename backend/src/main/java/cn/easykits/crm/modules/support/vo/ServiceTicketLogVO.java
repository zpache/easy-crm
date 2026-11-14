package cn.easykits.crm.modules.support.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 工单流转日志视图（详情抽屉时间线）。
 */
@Data
public class ServiceTicketLogVO {

    private Long id;

    private Long ticketId;

    private String fromStatus;

    private String fromStatusName;

    private String toStatus;

    private String toStatusName;

    private Long operatorId;

    private String operatorName;

    private LocalDateTime operateTime;

    private String remark;
}
