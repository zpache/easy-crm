package cn.easykits.crm.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 待办任务。status 落库仅 0 待办 / 1 已完成；「已过期」= 未完成且 due_time < NOW，
 * 查询时动态计算，不做落库（避免每分钟扫表）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("crm_task")
public class Task extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String title;

    private String content;

    /** 截止时间 */
    private LocalDateTime dueTime;

    /** 0 待办 / 1 已完成（已过期动态计算） */
    private Integer status;

    /** 关联业务类型（可空 = 独立待办） */
    private String bizType;

    /** 关联业务 ID */
    private Long bizId;

    /** 负责人 */
    private Long ownerId;

    /** 生成来源跟进记录 ID（source=FOLLOW 时有效） */
    private Long followRecordId;

    /** 来源：MANUAL 手动 / FOLLOW 跟进记录生成 */
    private String source;
}
