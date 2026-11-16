package cn.easykits.crm.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 审批实例镜像（只读）。
 *
 * <p>审批流本身对接到外部审批系统（企业微信审批 / 钉钉 OA 等），CRM 不做流程引擎，
 * 只保存审批实例的快照用于「待办 / 已办 / 已发起」三个入口列表与单据跳转。
 * 数据由外部系统同步任务写入，CRM 侧接口只读，不提供通过 / 驳回动作。
 *
 * <p>status：0 审批中 / 1 已通过 / 2 已驳回 / 3 已撤销。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("crm_approval_instance")
public class ApprovalInstance extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** QUOTE / CONTRACT / ORDER / INVOICE / REFUND / OTHER */
    private String bizType;

    private Long bizId;

    private String bizNo;

    private String title;

    private Long applyUserId;

    private String applyUserName;

    private Long currentApproverId;

    private String currentApproverName;

    /** 当前节点名称 */
    private String nodeName;

    private Integer status;

    private LocalDateTime applyTime;

    private LocalDateTime finishTime;

    /** 来源审批系统 */
    private String sourceSystem;

    /** 外部系统详情地址（点击跳转） */
    private String externalUrl;

    private String remark;
}
