package cn.easykits.crm.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 满意度评价：工单完成时允许评价，一单一评（uk_ticket_id + 服务层校验）。
 * ownerId / deptId 冗余自工单，供行级数据权限使用。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("crm_satisfaction")
public class Satisfaction extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 工单 ID */
    private Long ticketId;

    /** 评分 1-5 */
    private Integer score;

    /** 评价内容 */
    private String content;

    /** 评价时间 */
    private LocalDateTime evaluateTime;

    /** 冗余工单受理人 */
    private Long ownerId;

    /** 冗余工单归属部门 */
    private Long deptId;
}
