package cn.easykits.crm.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 跟进记录：多态关联业务对象（biz_type + biz_id 字段方案），
 * 写入/更新时通过策略模式（FollowActivityUpdater）同步业务对象活动时间。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("crm_follow_record")
public class FollowRecord extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 业务类型：lead / customer / opportunity / order（轮次 5） */
    private String bizType;

    /** 业务 ID */
    private Long bizId;

    /** 跟进类型字典：PHONE / VISIT / EMAIL / WECHAT / OTHER */
    private String followType;

    private String content;

    /** 下次跟进时间（填写则自动生成待办任务） */
    private LocalDateTime nextFollowTime;

    /** 跟进人 */
    private Long ownerId;

    /** 跟进人部门 */
    private Long deptId;
}
