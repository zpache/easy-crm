package cn.easykits.crm.modules.support.dto;

import lombok.Data;

/**
 * SLA 策略查询参数（配置表为全局配置，不接行级数据权限）。
 */
@Data
public class SlaPolicyQuery {

    private long pageNum = 1;

    private long pageSize = 10;

    private String ticketType;

    private String priority;

    private Integer enabled;
}
