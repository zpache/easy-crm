package cn.easykits.crm.modules.approval.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 审批实例视图（只读）。
 */
@Data
public class ApprovalInstanceVO {

    private Long id;

    private String bizType;

    private String bizTypeName;

    private Long bizId;

    private String bizNo;

    private String title;

    private Long applyUserId;

    private String applyUserName;

    private Long currentApproverId;

    private String currentApproverName;

    private String nodeName;

    private Integer status;

    private String statusName;

    private LocalDateTime applyTime;

    private LocalDateTime finishTime;

    private String sourceSystem;

    /** 外部审批系统详情地址 */
    private String externalUrl;

    private String remark;

    private LocalDateTime createTime;
}
