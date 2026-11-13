package com.easykits.crm.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 线索：尚未验证的潜在客户，独立于客户体系。
 * owner_id 为空表示在公海；转化后记录客户 / 联系人 / 商机 ID 并置为已验证。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("crm_lead")
public class Lead extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 线索名称 / 公司名（同名做重复提示但不阻断） */
    private String name;

    /** 联系人姓名 */
    private String contactName;

    /** 手机号（业务层全局唯一） */
    private String phone;

    /** 邮箱（业务层全局唯一） */
    private String email;

    /** 来源字典值，见 LeadSource 常量类（后续接字典表） */
    private Integer source;

    /** 状态：0 新建 1 跟进中 2 已验证 3 不合格，见 LeadStatus */
    private Integer status;

    /** 负责人（sys_user.id），NULL 表示公海 */
    private Long ownerId;

    /** 归属部门（sys_dept.id） */
    private Long deptId;

    private String remark;

    /** 转化生成的客户 ID */
    private Long convertedCustomerId;

    /** 转化生成的联系人 ID */
    private Long convertedContactId;

    /** 转化生成的商机 ID */
    private Long convertedOpportunityId;

    /** 最近跟进时间（公海回收依据） */
    private LocalDateTime lastFollowTime;
}
