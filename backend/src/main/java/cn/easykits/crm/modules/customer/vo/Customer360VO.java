package cn.easykits.crm.modules.customer.vo;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 客户 360 视图：固定查询次数（客户 + 联系人各一次），不随联系人数增长。
 * 商机 / 跟进记录列表在对应表建立后填充（轮次 3 / 4），当前返回空数组。
 */
@Data
public class Customer360VO {

    /** 基本信息 */
    private CustomerVO customer;

    /** 联系人列表（主联系人排最前） */
    private List<Map<String, Object>> contacts;

    /** 商机列表（TODO：轮次 3 商机表建立后填充） */
    private List<Map<String, Object>> opportunities;

    /** 跟进记录（TODO：轮次 4 跟进记录表建立后填充） */
    private List<Map<String, Object>> followRecords;
}
