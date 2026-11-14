package cn.easykits.crm.modules.support.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * SLA 策略视图。
 */
@Data
public class SlaPolicyVO {

    private Long id;

    private String ticketType;

    private String ticketTypeName;

    private String priority;

    private String priorityName;

    private Integer responseHours;

    private Integer resolveHours;

    private Integer enabled;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
