package cn.easykits.crm.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 工单流转日志。每次状态变更（含派单 / 改派 / 关闭）与工单变更在同一事务写入。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("crm_service_ticket_log")
public class ServiceTicketLog extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 工单 ID */
    private Long ticketId;

    /** 原状态（创建/无状态变更时可空） */
    private String fromStatus;

    /** 新状态 */
    private String toStatus;

    /** 操作人 */
    private Long operatorId;

    /** 操作时间 */
    private LocalDateTime operateTime;

    /** 备注 */
    private String remark;
}
