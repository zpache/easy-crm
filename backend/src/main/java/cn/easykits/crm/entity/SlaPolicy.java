package cn.easykits.crm.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * SLA 策略：工单类型 + 优先级 → 响应小时数 / 解决小时数，可配置启停。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("crm_sla_policy")
public class SlaPolicy extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 工单类型字典值 */
    private String ticketType;

    /** 优先级字典值 */
    private String priority;

    /** 响应时限（小时） */
    private Integer responseHours;

    /** 解决时限（小时） */
    private Integer resolveHours;

    /** 1 启用 / 0 停用 */
    private Integer enabled;
}
