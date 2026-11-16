package cn.easykits.crm.modules.approval.dto;

import lombok.Data;

/**
 * 审批入口查询参数。
 * <p>box 三个取值对应页面三个页签：
 * <ul>
 *   <li>todo：审批中且当前审批人是我（待办）</li>
 *   <li>done：已通过 / 已驳回且我审批过（已办）</li>
 *   <li>applied：我发起的（已发起）</li>
 * </ul>
 */
@Data
public class ApprovalQuery {

    /** todo / done / applied，默认 todo */
    private String box = "todo";

    private long pageNum = 1;

    private long pageSize = 10;

    /** 业务类型：QUOTE / CONTRACT / ORDER / INVOICE / REFUND / OTHER */
    private String bizType;

    /** 审批状态：0 审批中 / 1 已通过 / 2 已驳回 / 3 已撤销 */
    private Integer status;

    /** 标题 / 单号模糊匹配 */
    private String keyword;
}
