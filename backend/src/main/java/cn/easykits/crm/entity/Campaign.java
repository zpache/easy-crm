package cn.easykits.crm.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 营销活动。
 * <p>type：1 线上推广 / 2 线下活动 / 3 电话营销 / 4 渠道合作 / 5 内容营销。
 * <p>status：0 草稿 / 1 进行中 / 2 已结束 / 3 已暂停。
 * <p>活动效果按「crm_lead.campaign_id」归因：线索数、转化数、商机金额、赢单金额、ROI。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("crm_campaign")
public class Campaign extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 活动编码，业务唯一 */
    private String code;

    private String name;

    private Integer type;

    /** 投放渠道（搜索引擎 / 信息流 / 展会 / 公众号…） */
    private String channel;

    private Integer status;

    /** 活动预算 */
    private BigDecimal budget;

    private LocalDate startDate;

    private LocalDate endDate;

    /** 目标线索数 */
    private Integer targetLeads;

    /** 负责人（sys_user.id） */
    private Long ownerId;

    /** 归属部门（sys_dept.id） */
    private Long deptId;

    private String remark;
}
